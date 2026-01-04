package de.docs_as_co.intellij.plugin.drawio.mcp

import com.google.gson.Gson
import com.google.gson.JsonObject
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
 * JSON-RPC 2.0 error codes.
 */
object JsonRpcError {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
}

/**
 * HTTP server using NanoHTTPD that exposes the MCP JSON-RPC API.
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
            // MCP Streamable HTTP Transport endpoint (JSON-RPC 2.0)
            uri == "/mcp" && method == Method.POST -> handleMcpRequest(session)

            // Health check endpoint
            uri == "/" && method == Method.GET -> handleRoot()

            else -> {
                val response = mapOf(
                    "jsonrpc" to "2.0",
                    "id" to null,
                    "error" to mapOf(
                        "code" to -32601,
                        "message" to "Endpoint not found: $uri. Use POST /mcp for MCP JSON-RPC requests."
                    )
                )
                newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", gson.toJson(response))
            }
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

    // ========================================================================
    // MCP Streamable HTTP Transport - JSON-RPC 2.0 Handlers
    // ========================================================================

    /**
     * Handle MCP JSON-RPC 2.0 requests on POST /mcp endpoint.
     * This implements the MCP Streamable HTTP Transport specification.
     */
    private fun handleMcpRequest(session: IHTTPSession): Response {
        LOG.debug("Handling MCP JSON-RPC request")

        val body = readRequestBody(session)
        if (body.isEmpty() || body == "{}") {
            return mcpErrorResponse(null, JsonRpcError.PARSE_ERROR, "Empty request body")
        }

        return try {
            val jsonObject = gson.fromJson(body, JsonObject::class.java)

            // Validate JSON-RPC 2.0 structure
            val jsonrpc = jsonObject.get("jsonrpc")?.asString
            if (jsonrpc != "2.0") {
                return mcpErrorResponse(null, JsonRpcError.INVALID_REQUEST, "Invalid JSON-RPC version, expected '2.0'")
            }

            val method = jsonObject.get("method")?.asString
            if (method == null) {
                return mcpErrorResponse(null, JsonRpcError.INVALID_REQUEST, "Missing 'method' field")
            }

            // Extract id (can be string, number, or null for notifications)
            val idElement = jsonObject.get("id")
            val id: Any? = when {
                idElement == null || idElement.isJsonNull -> null
                idElement.isJsonPrimitive -> {
                    val primitive = idElement.asJsonPrimitive
                    when {
                        primitive.isNumber -> primitive.asNumber
                        primitive.isString -> primitive.asString
                        else -> primitive.toString()
                    }
                }
                else -> idElement.toString()
            }

            val params = jsonObject.getAsJsonObject("params")

            LOG.debug("MCP request: method=$method, id=$id")

            // Dispatch to appropriate handler
            when (method) {
                "initialize" -> handleMcpInitialize(id, params)
                "initialized" -> handleMcpInitialized(id)
                "tools/list" -> handleMcpToolsList(id)
                "tools/call" -> handleMcpToolsCall(id, params)
                "ping" -> handleMcpPing(id)
                else -> mcpErrorResponse(id, JsonRpcError.METHOD_NOT_FOUND, "Unknown method: $method")
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            LOG.warn("Failed to parse MCP JSON-RPC request", e)
            mcpErrorResponse(null, JsonRpcError.PARSE_ERROR, "Invalid JSON: ${e.message}")
        } catch (e: Exception) {
            LOG.error("Error handling MCP request", e)
            mcpErrorResponse(null, JsonRpcError.INTERNAL_ERROR, "Internal error: ${e.message}")
        }
    }

    /**
     * Handle MCP initialize request.
     * Returns server capabilities and protocol version.
     */
    private fun handleMcpInitialize(id: Any?, params: JsonObject?): Response {
        LOG.debug("Handling MCP initialize request")

        val result = mapOf(
            "protocolVersion" to "2024-11-05",
            "capabilities" to mapOf(
                "tools" to emptyMap<String, Any>()
            ),
            "serverInfo" to mapOf(
                "name" to "diagrams-net-intellij",
                "version" to "0.2.7"
            )
        )

        return mcpSuccessResponse(id, result)
    }

    /**
     * Handle MCP initialized notification.
     * This is a notification (no response expected), but we acknowledge it.
     */
    private fun handleMcpInitialized(id: Any?): Response {
        LOG.debug("Received MCP initialized notification")
        // For notifications with no id, we still return an empty success response
        // to acknowledge receipt (some clients expect this)
        return if (id != null) {
            mcpSuccessResponse(id, emptyMap<String, Any>())
        } else {
            // Pure notification - return minimal acknowledgment
            newFixedLengthResponse(Response.Status.OK, "application/json", "{}")
        }
    }

    /**
     * Handle MCP tools/list request.
     * Returns the list of available tools with their schemas.
     */
    private fun handleMcpToolsList(id: Any?): Response {
        LOG.debug("Handling MCP tools/list request")

        val tools = listOf(
            mapOf(
                "name" to "list_diagrams",
                "description" to "List all open diagrams in IntelliJ IDEA. Returns diagram IDs, file names, paths, and project information.",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to emptyMap<String, Any>(),
                    "required" to emptyList<String>()
                )
            ),
            mapOf(
                "name" to "get_diagram_by_id",
                "description" to "Get diagram content and metadata by ID. Returns the diagram XML (both raw and decoded mxGraphModel format), file path, and other metadata.",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "id" to mapOf(
                            "type" to "string",
                            "description" to "The diagram ID (obtained from list_diagrams)"
                        )
                    ),
                    "required" to listOf("id")
                )
            ),
            mapOf(
                "name" to "update_diagram",
                "description" to "Update diagram content and save changes. Accepts either decoded mxGraphModel XML or encoded mxfile XML. Changes appear immediately in the IntelliJ editor.",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "id" to mapOf(
                            "type" to "string",
                            "description" to "The diagram ID (obtained from list_diagrams)"
                        ),
                        "xml" to mapOf(
                            "type" to "string",
                            "description" to "The new diagram XML content (mxGraphModel or mxfile format)"
                        )
                    ),
                    "required" to listOf("id", "xml")
                )
            )
        )

        val result = mapOf("tools" to tools)
        return mcpSuccessResponse(id, result)
    }

    /**
     * Handle MCP tools/call request.
     * Dispatches to the appropriate tool implementation.
     */
    private fun handleMcpToolsCall(id: Any?, params: JsonObject?): Response {
        if (params == null) {
            return mcpErrorResponse(id, JsonRpcError.INVALID_PARAMS, "Missing 'params' for tools/call")
        }

        val toolName = params.get("name")?.asString
        if (toolName == null) {
            return mcpErrorResponse(id, JsonRpcError.INVALID_PARAMS, "Missing 'name' in params")
        }

        val arguments = params.getAsJsonObject("arguments")

        LOG.debug("Handling MCP tools/call: tool=$toolName")

        return when (toolName) {
            "list_diagrams" -> handleMcpListDiagrams(id)
            "get_diagram_by_id" -> handleMcpGetDiagram(id, arguments)
            "update_diagram" -> handleMcpUpdateDiagram(id, arguments)
            else -> mcpErrorResponse(id, JsonRpcError.INVALID_PARAMS, "Unknown tool: $toolName")
        }
    }

    /**
     * Handle MCP ping request.
     */
    private fun handleMcpPing(id: Any?): Response {
        LOG.debug("Handling MCP ping request")
        return mcpSuccessResponse(id, emptyMap<String, Any>())
    }

    /**
     * MCP tool: list_diagrams
     */
    private fun handleMcpListDiagrams(id: Any?): Response {
        val diagrams = ApplicationManager.getApplication().runReadAction<List<DiagramInfo>> {
            service.listDiagrams()
        }

        val text = if (diagrams.isEmpty()) {
            "No diagrams are currently open in the IDE."
        } else {
            val sb = StringBuilder("Open diagrams (${diagrams.size}):\n")
            diagrams.forEach { diagram ->
                sb.append("- ID: ${diagram.id}, File: ${diagram.fileName}, Path: ${diagram.filePath}, Project: ${diagram.project}\n")
            }
            sb.toString()
        }

        val result = mapOf(
            "content" to listOf(
                mapOf("type" to "text", "text" to text)
            )
        )
        return mcpSuccessResponse(id, result)
    }

    /**
     * MCP tool: get_diagram_by_id
     */
    private fun handleMcpGetDiagram(id: Any?, arguments: JsonObject?): Response {
        val diagramId = arguments?.get("id")?.asString
        if (diagramId == null) {
            return mcpErrorResponse(id, JsonRpcError.INVALID_PARAMS, "Missing required argument 'id'")
        }

        val editorRef = ApplicationManager.getApplication().runReadAction<DiagramEditorReference?> {
            service.getEditor(diagramId)
        }

        if (editorRef == null) {
            val errorText = "Diagram not found with ID: $diagramId"
            val result = mapOf(
                "content" to listOf(
                    mapOf("type" to "text", "text" to errorText)
                ),
                "isError" to true
            )
            return mcpSuccessResponse(id, result)
        }

        // Read fresh from disk
        val rawXml = try {
            ApplicationManager.getApplication().runReadAction<String> {
                editorRef.file.inputStream.reader().readText()
            }
        } catch (e: Exception) {
            LOG.warn("Failed to read diagram from disk, falling back to cached content", e)
            editorRef.editor.getXmlContent()
        }

        // For SVG files, extract the embedded mxfile XML
        val xml = if (editorRef.file.name.endsWith(".svg")) {
            extractMxfileFromSvg(rawXml ?: "")
        } else {
            rawXml
        }

        // Decode the diagram content for MCP clients
        val decodedXml = xml?.let { decodeDiagramContent(it) }

        val sb = StringBuilder()
        sb.append("Diagram: ${editorRef.file.name}\n")
        sb.append("Path: ${editorRef.file.path}\n")
        sb.append("Project: ${editorRef.project.name}\n")
        sb.append("File Type: ${getFileType(editorRef.file.name)}\n\n")

        if (decodedXml != null) {
            sb.append("=== Decoded XML (mxGraphModel) ===\n")
            sb.append(decodedXml)
        } else if (xml != null) {
            sb.append("=== Raw XML ===\n")
            sb.append(xml)
        } else {
            sb.append("(No content available)")
        }

        val result = mapOf(
            "content" to listOf(
                mapOf("type" to "text", "text" to sb.toString())
            )
        )
        return mcpSuccessResponse(id, result)
    }

    /**
     * MCP tool: update_diagram
     */
    private fun handleMcpUpdateDiagram(id: Any?, arguments: JsonObject?): Response {
        val diagramId = arguments?.get("id")?.asString
        val xml = arguments?.get("xml")?.asString

        if (diagramId == null) {
            return mcpErrorResponse(id, JsonRpcError.INVALID_PARAMS, "Missing required argument 'id'")
        }
        if (xml == null) {
            return mcpErrorResponse(id, JsonRpcError.INVALID_PARAMS, "Missing required argument 'xml'")
        }

        val editorRef = ApplicationManager.getApplication().runReadAction<DiagramEditorReference?> {
            service.getEditor(diagramId)
        }

        if (editorRef == null) {
            val errorText = "Diagram not found with ID: $diagramId"
            val result = mapOf(
                "content" to listOf(
                    mapOf("type" to "text", "text" to errorText)
                ),
                "isError" to true
            )
            return mcpSuccessResponse(id, result)
        }

        // Detect if incoming XML is decoded (mxGraphModel) or encoded (mxfile)
        val decodedXml = extractDecodedXmlIfNeeded(xml)
        val xmlToSave = if (decodedXml != null) {
            LOG.debug("Received decoded XML from MCP client, encoding to mxfile format")
            try {
                encodeDiagramContent(decodedXml)
            } catch (e: Exception) {
                LOG.error("Failed to encode decoded XML", e)
                val errorText = "Failed to encode XML: ${e.message}"
                val result = mapOf(
                    "content" to listOf(
                        mapOf("type" to "text", "text" to errorText)
                    ),
                    "isError" to true
                )
                return mcpSuccessResponse(id, result)
            }
        } else {
            LOG.debug("Received properly encoded mxfile XML, using as-is")
            xml
        }

        editorRef.editor.updateAndSaveXmlContent(xmlToSave)

        val successText = "Diagram '${editorRef.file.name}' updated successfully."
        val result = mapOf(
            "content" to listOf(
                mapOf("type" to "text", "text" to successText)
            )
        )
        return mcpSuccessResponse(id, result)
    }

    /**
     * Create a successful JSON-RPC response.
     */
    private fun mcpSuccessResponse(id: Any?, result: Any): Response {
        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to result
        )
        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response))
    }

    /**
     * Create an error JSON-RPC response.
     */
    private fun mcpErrorResponse(id: Any?, code: Int, message: String): Response {
        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "error" to mapOf(
                "code" to code,
                "message" to message
            )
        )
        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response))
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
