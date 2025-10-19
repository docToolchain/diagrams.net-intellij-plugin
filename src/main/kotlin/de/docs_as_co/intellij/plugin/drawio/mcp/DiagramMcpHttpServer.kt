package de.docs_as_co.intellij.plugin.drawio.mcp

import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.URLDecoder
import java.util.Base64
import java.util.zip.Inflater

/**
 * HTTP server using NanoHTTPD that exposes the MCP REST API.
 *
 * NanoHTTPD was chosen over Ktor/Netty due to classloader/threading compatibility issues
 * in the IntelliJ plugin environment. See MCP_HTTP_SERVER_ISSUES.md for details.
 */
class DiagramMcpHttpServer(private val port: Int, private val service: DiagramMcpService) : NanoHTTPD(port) {
    private val LOG = Logger.getInstance(DiagramMcpHttpServer::class.java)
    private val gson = Gson()

    init {
        LOG.info("DiagramMcpHttpServer initialized on port $port")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        LOG.debug("MCP HTTP Request: $method $uri")

        return try {
            // Add CORS headers
            val response = handleRequest(session, uri, method)
            addCORSHeaders(response)
            response
        } catch (e: Exception) {
            LOG.error("Error handling MCP HTTP request $uri", e)

            val errorResponse = mapOf(
                "error" to "Internal server error",
                "message" to (e.message ?: "Unknown error"),
                "type" to e.javaClass.simpleName
            )
            val response = newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(errorResponse)
            )
            addCORSHeaders(response)
            response
        }
    }

    private fun handleRequest(session: IHTTPSession, uri: String, method: Method): Response {
        // Handle OPTIONS for CORS preflight
        if (method == Method.OPTIONS) {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
        }

        // Route requests
        return when {
            uri == "/" && method == Method.GET -> handleRoot()
            uri == "/api/test" && method == Method.GET -> handleTest()
            uri == "/api/status" && method == Method.GET -> handleStatus()
            uri == "/api/diagrams" && method == Method.GET -> handleListDiagrams()
            uri == "/api/diagrams" && method == Method.POST -> handleCreateDiagram(session)
            uri.startsWith("/api/diagrams/") && method == Method.GET -> {
                val id = uri.substring("/api/diagrams/".length)
                if (id.contains("?")) {
                    // Handle query parameters (e.g., /api/diagrams/by-path?path=...)
                    when {
                        uri.startsWith("/api/diagrams/by-path") -> handleFindByPath(session)
                        else -> notFound(uri)
                    }
                } else {
                    handleGetDiagram(id)
                }
            }
            uri.startsWith("/api/diagrams/") && method == Method.PUT -> {
                val id = uri.substring("/api/diagrams/".length)
                handleUpdateDiagram(session, id)
            }
            uri == "/api/mcp/info" && method == Method.GET -> handleMcpInfo()
            else -> notFound(uri)
        }
    }

    private fun handleRoot(): Response {
        LOG.debug("Handling root / request")
        return newFixedLengthResponse(
            Response.Status.OK,
            "text/plain",
            "diagrams.net MCP Server - Running"
        )
    }

    private fun handleTest(): Response {
        LOG.debug("Handling /api/test request")
        val response = mapOf(
            "message" to "Test endpoint working",
            "timestamp" to System.currentTimeMillis()
        )
        return jsonResponse(Response.Status.OK, response)
    }

    private fun handleStatus(): Response {
        LOG.debug("Handling /api/status request")

        val diagramCount = ApplicationManager.getApplication().runReadAction<Int> {
            service.listDiagrams().size
        }

        val response = mapOf(
            "status" to "running",
            "port" to port,
            "version" to "0.2.7",
            "openDiagrams" to diagramCount
        )

        LOG.debug("Responding to /api/status with $diagramCount diagrams")
        return jsonResponse(Response.Status.OK, response)
    }

    private fun handleListDiagrams(): Response {
        LOG.debug("Handling /api/diagrams list request")

        val diagrams = ApplicationManager.getApplication().runReadAction<List<DiagramInfo>> {
            service.listDiagrams()
        }

        LOG.debug("Responding with ${diagrams.size} diagrams")
        return jsonResponse(Response.Status.OK, mapOf("diagrams" to diagrams))
    }

    private fun handleGetDiagram(id: String): Response {
        LOG.debug("Handling /api/diagrams/$id request")

        val editorRef = ApplicationManager.getApplication().runReadAction<DiagramEditorReference?> {
            service.getEditor(id)
        }

        if (editorRef != null) {
            // For GET requests, always read fresh from disk to avoid serving stale cached content
            // This ensures Claude Desktop and other MCP clients get the latest saved content
            val rawXml = try {
                ApplicationManager.getApplication().runReadAction<String> {
                    editorRef.file.inputStream.reader().readText()
                }
            } catch (e: Exception) {
                LOG.warn("Failed to read diagram from disk, falling back to cached content", e)
                editorRef.editor.getXmlContent()
            }

            // For SVG files, extract the embedded mxfile XML from the content attribute
            val xml = if (editorRef.file.name.endsWith(".svg")) {
                extractMxfileFromSvg(rawXml ?: "")
            } else {
                rawXml
            }

            // Decode the diagram content for MCP clients
            // This provides a readable mxGraphModel XML instead of base64+zlib compressed data
            val decodedXml = xml?.let { decodeDiagramContent(it) }

            val response = mutableMapOf(
                "id" to id,
                "filePath" to editorRef.file.path,
                "fileName" to editorRef.file.name,
                "fileType" to getFileType(editorRef.file.name),
                "project" to editorRef.project.name,
                "xml" to xml
            )

            // Add decoded XML if available
            if (decodedXml != null) {
                response["decodedXml"] = decodedXml
            }

            return jsonResponse(Response.Status.OK, response)
        } else {
            return jsonResponse(
                Response.Status.NOT_FOUND,
                mapOf(
                    "error" to "Diagram not found",
                    "code" to "NOT_FOUND",
                    "message" to "No diagram found with ID: $id"
                )
            )
        }
    }

    private fun handleUpdateDiagram(session: IHTTPSession, id: String): Response {
        LOG.debug("Handling PUT /api/diagrams/$id request")

        val body = readRequestBody(session)
        val request = gson.fromJson(body, Map::class.java) as Map<*, *>
        val xml = request["xml"] as? String

        if (xml == null) {
            return jsonResponse(
                Response.Status.BAD_REQUEST,
                mapOf(
                    "error" to "Missing required field",
                    "code" to "MISSING_FIELD",
                    "message" to "'xml' field is required"
                )
            )
        }

        val editorRef = ApplicationManager.getApplication().runReadAction<DiagramEditorReference?> {
            service.getEditor(id)
        }

        if (editorRef != null) {
            editorRef.editor.updateAndSaveXmlContent(xml)
            return jsonResponse(
                Response.Status.OK,
                mapOf(
                    "success" to true,
                    "message" to "Diagram updated successfully"
                )
            )
        } else {
            return jsonResponse(
                Response.Status.NOT_FOUND,
                mapOf(
                    "error" to "Diagram not found",
                    "code" to "NOT_FOUND",
                    "message" to "No diagram found with ID: $id"
                )
            )
        }
    }

    private fun handleCreateDiagram(session: IHTTPSession): Response {
        LOG.debug("Handling POST /api/diagrams request")

        val body = readRequestBody(session)
        val request = gson.fromJson(body, Map::class.java) as Map<*, *>

        val projectName = request["project"] as? String
        val relativePath = request["path"] as? String
        val fileType = (request["fileType"] as? String) ?: "svg"
        val initialContent = request["content"] as? String

        if (projectName == null || relativePath == null) {
            return jsonResponse(
                Response.Status.BAD_REQUEST,
                mapOf(
                    "error" to "Missing required parameters",
                    "code" to "MISSING_PARAMETERS",
                    "message" to "Both 'project' and 'path' are required"
                )
            )
        }

        // Implementation would go here - for now return not implemented
        return jsonResponse(
            Response.Status.NOT_IMPLEMENTED,
            mapOf(
                "error" to "Not implemented",
                "message" to "Create diagram endpoint is not yet implemented"
            )
        )
    }

    private fun handleFindByPath(session: IHTTPSession): Response {
        val params = session.parms
        val path = params["path"]

        if (path == null) {
            return jsonResponse(
                Response.Status.BAD_REQUEST,
                mapOf(
                    "error" to "Missing parameter",
                    "code" to "MISSING_PARAMETER",
                    "message" to "'path' parameter is required"
                )
            )
        }

        // Implementation would search for diagram by path
        return jsonResponse(
            Response.Status.NOT_IMPLEMENTED,
            mapOf(
                "error" to "Not implemented",
                "message" to "Find by path endpoint is not yet implemented"
            )
        )
    }

    private fun handleMcpInfo(): Response {
        val info = mapOf(
            "name" to "diagrams.net MCP Server",
            "version" to "0.2.7",
            "protocol" to "mcp-1.0",
            "capabilities" to mapOf(
                "diagrams" to mapOf(
                    "list" to true,
                    "get" to true,
                    "update" to true,
                    "create" to false
                )
            )
        )
        return jsonResponse(Response.Status.OK, info)
    }

    private fun notFound(uri: String): Response {
        return jsonResponse(
            Response.Status.NOT_FOUND,
            mapOf(
                "error" to "Not found",
                "code" to "NOT_FOUND",
                "message" to "Endpoint not found: $uri"
            )
        )
    }

    private fun jsonResponse(status: Response.Status, data: Any): Response {
        val json = gson.toJson(data)
        return newFixedLengthResponse(status, "application/json", json)
    }

    private fun readRequestBody(session: IHTTPSession): String {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (contentLength == 0) {
            return "{}"
        }

        // Read the body directly from the input stream
        return try {
            val buffer = ByteArray(contentLength)
            var totalRead = 0
            val inputStream = session.inputStream

            while (totalRead < contentLength) {
                val read = inputStream.read(buffer, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }

            val body = String(buffer, 0, totalRead, Charsets.UTF_8)
            LOG.debug("readRequestBody: successfully read $totalRead bytes")
            body
        } catch (e: Exception) {
            LOG.error("Error reading request body", e)
            "{}"
        }
    }

    private fun addCORSHeaders(response: Response): Response {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
        return response
    }

    private fun getFileType(fileName: String): String {
        return when {
            fileName.endsWith(".svg") -> "svg"
            fileName.endsWith(".png") -> "png"
            fileName.endsWith(".xml") -> "xml"
            else -> "unknown"
        }
    }

    private fun extractMxfileFromSvg(svgContent: String): String? {
        // Extract the mxfile XML from the SVG's content attribute
        // The content attribute contains HTML-encoded XML like: content="&lt;mxfile...&gt;&lt;/mxfile&gt;"
        try {
            val contentMatch = Regex("""content="([^"]+)"""").find(svgContent)
            if (contentMatch != null) {
                val encodedXml = contentMatch.groupValues[1]
                // Decode HTML entities
                return encodedXml
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
            }
        } catch (e: Exception) {
            LOG.error("Error extracting mxfile from SVG", e)
        }
        return null
    }

    private fun decodeDiagramContent(mxfileXml: String): String? {
        // Decode the base64+zlib compressed content from <diagram> tags
        // Returns the readable mxGraphModel XML for MCP clients
        try {
            val diagramMatch = Regex("""<diagram[^>]*>([^<]+)</diagram>""").find(mxfileXml)
            if (diagramMatch == null) {
                return null
            }

            val base64Content = diagramMatch.groupValues[1].trim()
            if (base64Content.isEmpty()) {
                return null
            }

            // Base64 decode
            val compressed = Base64.getDecoder().decode(base64Content)

            // Decompress with zlib (using raw inflate, no zlib header)
            val inflater = Inflater(true) // true = nowrap mode (raw deflate)
            inflater.setInput(compressed)
            val decompressed = ByteArray(compressed.size * 10) // Allocate buffer
            val decompressedLength = inflater.inflate(decompressed)
            inflater.end()

            // Convert to string and URL decode
            val urlEncoded = String(decompressed, 0, decompressedLength, Charsets.UTF_8)
            val decoded = URLDecoder.decode(urlEncoded, "UTF-8")

            return decoded
        } catch (e: Exception) {
            LOG.warn("Failed to decode diagram content", e)
            return null
        }
    }

    fun startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            LOG.info("MCP HTTP server started successfully on port $port")
        } catch (e: IOException) {
            LOG.error("Failed to start MCP HTTP server", e)
            throw e
        }
    }

    fun stopServer() {
        stop()
        LOG.info("MCP HTTP server stopped")
    }
}
