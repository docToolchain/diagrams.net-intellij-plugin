package de.docs_as_co.intellij.plugin.drawio.mcp

import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64
import java.util.zip.Deflater
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
            // Detect if incoming XML is decoded (mxGraphModel) or encoded (mxfile)
            // MCP clients send decoded XML for easier editing
            val decodedXml = extractDecodedXmlIfNeeded(xml)
            val xmlToSave = if (decodedXml != null) {
                LOG.debug("Received decoded XML from MCP client (extracted from wrapper), encoding to mxfile format")
                // Save the decoded XML to temp file for debugging (only when debug logging is enabled)
                if (LOG.isDebugEnabled) {
                    try {
                        val tmpDir = System.getProperty("java.io.tmpdir")
                        val debugFile = java.io.File(tmpDir, "mcp-decoded-xml-${System.currentTimeMillis()}.xml")
                        debugFile.writeText(decodedXml)
                        LOG.debug("Saved decoded XML to ${debugFile.absolutePath} for debugging")
                    } catch (e: Exception) {
                        LOG.warn("Failed to save debug XML file", e)
                    }
                }
                try {
                    encodeDiagramContent(decodedXml)
                } catch (e: Exception) {
                    LOG.error("Failed to encode decoded XML", e)
                    return jsonResponse(
                        Response.Status.BAD_REQUEST,
                        mapOf(
                            "error" to "Invalid XML format",
                            "code" to "ENCODING_ERROR",
                            "message" to "Failed to encode decoded XML: ${e.message}"
                        )
                    )
                }
            } else {
                LOG.debug("Received properly encoded mxfile XML, using as-is")
                xml
            }

            editorRef.editor.updateAndSaveXmlContent(xmlToSave)
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

            // Use a dynamically growing buffer to ensure we get all data
            val outputStream = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            val decompressed = outputStream.toByteArray()

            // Convert to string and URL decode
            val urlEncoded = String(decompressed, Charsets.UTF_8)
            val decoded = URLDecoder.decode(urlEncoded, "UTF-8")

            return decoded
        } catch (e: Exception) {
            LOG.warn("Failed to decode diagram content", e)
            return null
        }
    }

    /**
     * Extract decoded XML from incoming content if needed.
     * Handles cases where MCP clients send:
     * 1. Raw mxGraphModel XML (decoded) - return as-is
     * 2. mxfile wrapper with decoded content - extract and return decoded content
     * 3. mxfile wrapper with properly encoded base64+zlib - return null (use as-is)
     *
     * @param xml The incoming XML content
     * @return Decoded mxGraphModel XML if found, null if properly encoded
     */
    private fun extractDecodedXmlIfNeeded(xml: String): String? {
        val trimmed = xml.trim()

        // Case 1: Raw mxGraphModel - already decoded
        if (trimmed.startsWith("<mxGraphModel")) {
            LOG.debug("Detected raw mxGraphModel XML (decoded)")
            return trimmed
        }

        // Case 2: mxfile wrapper - check if content is decoded or encoded
        if (trimmed.startsWith("<mxfile")) {
            try {
                // Find the diagram tag opening
                val diagramStartMatch = Regex("""<diagram[^>]*>""").find(trimmed)
                if (diagramStartMatch != null) {
                    val startIdx = diagramStartMatch.range.last + 1

                    // Find the LAST </diagram> closing tag (not the first!)
                    val diagramEndIdx = trimmed.lastIndexOf("</diagram>")

                    if (diagramEndIdx > startIdx) {
                        val content = trimmed.substring(startIdx, diagramEndIdx).trim()
                        LOG.debug("Extracted content from diagram tags: length=${content.length}, first 100 chars: ${content.take(100)}")

                        // Check if content looks like decoded XML vs base64
                        // Base64 should not contain < or > characters
                        if (content.contains("<") || content.contains(">")) {
                            LOG.debug("Detected mxfile wrapper with decoded XML content (contains < or >)")
                            return content
                        }

                        // If content is very long, it's likely decoded XML that just doesn't have < >
                        // Properly encoded/compressed content is typically 1-3KB, decoded is 10-15KB+
                        if (content.length > 5000) {
                            LOG.debug("Detected mxfile wrapper with likely decoded content (length=${content.length})")
                            return content
                        }

                        // Otherwise, it's properly base64 encoded
                        LOG.debug("Content appears to be properly base64 encoded (length=${content.length})")
                        return null
                    }
                }
            } catch (e: Exception) {
                LOG.warn("Error extracting content from mxfile wrapper", e)
            }
        }

        // Unknown format or properly encoded
        return null
    }

    /**
     * Encode decoded mxGraphModel XML back to mxfile format with base64+zlib compression.
     * This is the reverse of decodeDiagramContent().
     *
     * @param decodedXml The decoded mxGraphModel XML
     * @return Encoded mxfile XML with compressed diagram data
     */
    private fun encodeDiagramContent(decodedXml: String): String {
        try {
            // URL encode using URI component encoding (not form encoding)
            // This encodes special characters but preserves more characters than URLEncoder
            val urlEncoded = URLEncoder.encode(decodedXml, "UTF-8")
                .replace("+", "%20")  // URLEncoder encodes space as +, but we need %20
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~")

            // Compress with zlib (using raw deflate, no zlib header)
            val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true) // true = nowrap mode
            deflater.setInput(urlEncoded.toByteArray(Charsets.UTF_8))
            deflater.finish()

            val compressedBuffer = ByteArray(urlEncoded.length * 2) // Allocate sufficient buffer
            val compressedLength = deflater.deflate(compressedBuffer)
            deflater.end()

            // Base64 encode
            val base64Encoded = Base64.getEncoder().encodeToString(compressedBuffer.copyOf(compressedLength))

            // Wrap in mxfile structure
            return """<mxfile host="drawio-plugin" modified="${System.currentTimeMillis()}" agent="MCP-Client" version="22.1.22" type="embed"><diagram name="Page-1" id="0">$base64Encoded</diagram></mxfile>"""
        } catch (e: Exception) {
            LOG.error("Failed to encode diagram content", e)
            throw e
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
