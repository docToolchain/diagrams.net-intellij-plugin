package de.docs_as_co.intellij.plugin.drawio.mcp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.docs_as_co.intellij.plugin.drawio.editor.DiagramsEditor
import de.docs_as_co.intellij.plugin.drawio.settings.DiagramsApplicationSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Application-level service that manages the MCP HTTP server and tracks open diagram editors.
 */
@Service(Service.Level.APP)
class DiagramMcpService : Disposable {
    private val LOG = Logger.getInstance(DiagramMcpService::class.java)

    private val openEditors = ConcurrentHashMap<String, DiagramEditorReference>()
    private var httpServer: DiagramMcpHttpServer? = null
    private var actualPort: Int = 0

    init {
        LOG.info("DiagramMcpService initializing")
        // Clear any inherited port env var - only set it when server actually starts
        McpPortManager.clearCurrentPort()

        // Start server if enabled in settings
        val settings = DiagramsApplicationSettings.instance.getDiagramsSettings()
        // Calculate effective port based on IDE product type
        val effectivePort = McpPortManager.calculatePort(settings.mcpServerPort)
        LOG.info("MCP server enabled: ${settings.mcpServerEnabled}, settings port: ${settings.mcpServerPort}, effective port: $effectivePort (${McpPortManager.getProductCode()})")
        if (settings.mcpServerEnabled) {
            startServer(effectivePort)
        }
        LOG.info("DiagramMcpService initialized")
    }

    /**
     * Register an editor when it's opened.
     * @param id Unique identifier for the editor (based on file path)
     * @param editor The DiagramsEditor instance
     */
    fun registerEditor(id: String, editor: DiagramsEditor, project: Project, file: VirtualFile) {
        LOG.info("Registering editor: $id for file: ${file.path}")
        openEditors[id] = DiagramEditorReference(editor, project, file)
    }

    /**
     * Unregister an editor when it's closed.
     * @param id Unique identifier for the editor
     */
    fun unregisterEditor(id: String) {
        LOG.info("Unregistering editor: $id")
        openEditors.remove(id)
    }

    /**
     * Get an editor by its ID.
     * @param id The editor ID
     * @return The editor reference, or null if not found
     */
    fun getEditor(id: String): DiagramEditorReference? {
        return openEditors[id]
    }

    /**
     * List all currently open diagrams.
     * @return List of diagram info for all open editors
     */
    fun listDiagrams(): List<DiagramInfo> {
        return openEditors.map { (id, ref) ->
            DiagramInfo(
                id = id,
                filePath = ref.file.path,
                relativePath = getRelativePath(ref.project, ref.file),
                fileName = ref.file.name,
                fileType = getFileType(ref.file),
                project = ref.project.name,
                isOpen = true,
                isModified = false // TODO: Implement isModified tracking; currently always false
            )
        }
    }

    /**
     * Get the relative path of a file from the project root.
     */
    private fun getRelativePath(project: Project, file: VirtualFile): String {
        val projectPath = project.basePath ?: return file.path
        val filePath = file.path
        return if (filePath.startsWith(projectPath)) {
            filePath.substring(projectPath.length + 1)
        } else {
            filePath
        }
    }

    /**
     * Extract file type from filename.
     */
    private fun getFileType(file: VirtualFile): String {
        return when {
            file.name.endsWith(".svg") -> "svg"
            file.name.endsWith(".png") -> "png"
            file.name.endsWith(".xml") -> "xml"
            else -> "unknown"
        }
    }

    /**
     * Start the MCP HTTP server.
     * @param preferredPort The preferred port to use
     * @param onStarted Optional callback invoked on EDT when server starts successfully.
     *                  Receives the actual port the server started on (may differ from preferred if fallback occurred).
     * @return true if server start was initiated
     */
    fun startServer(preferredPort: Int, onStarted: ((actualPort: Int) -> Unit)? = null): Boolean {
        if (httpServer != null) {
            LOG.warn("Server already running on port $actualPort")
            return false
        }

        try {
            LOG.debug("startServer called, will start in background thread")
            // Start server in a background thread to avoid blocking plugin initialization
            Thread {
                try {
                    LOG.debug("Background thread starting MCP server")
                    // Try preferred port first, then try next ports if busy
                    var port = preferredPort
                    var started = false
                    var lastError: Exception? = null

                    for (attempt in 0..9) {
                        try {
                            LOG.info("Attempting to start MCP server on port $port")
                            val server = DiagramMcpHttpServer(port, this@DiagramMcpService)
                            server.startServer()
                            httpServer = server
                            actualPort = port
                            started = true
                            LOG.info("MCP server started successfully on port $port")
                            // Export port for Claude Code discovery
                            McpPortManager.exportCurrentPort(port)
                            LOG.info(McpPortManager.getPortDescription(port))

                            // Invoke callback on EDT if provided, passing actual port
                            val finalPort = port
                            onStarted?.let {
                                ApplicationManager.getApplication().invokeLater { it(finalPort) }
                            }
                            break
                        } catch (e: Exception) {
                            LOG.warn("Port $port is busy, trying next port: ${e.message}")
                            lastError = e
                            port++
                        }
                    }

                    if (!started) {
                        LOG.error("Failed to start MCP server after 10 attempts", lastError)
                    }
                } catch (e: Exception) {
                    LOG.error("Failed to start MCP server in background thread", e)
                }
            }.start()

            return true
        } catch (e: Exception) {
            LOG.error("Failed to start MCP server", e)
            return false
        }
    }

    /**
     * Stop the MCP HTTP server.
     */
    fun stopServer() {
        httpServer?.let {
            LOG.info("Stopping MCP server on port $actualPort")
            it.stopServer()
            httpServer = null
            actualPort = 0
            // Clear the exported port so new terminals don't see stale value
            McpPortManager.clearCurrentPort()
        }
    }

    /**
     * Check if server is running.
     */
    fun isServerRunning(): Boolean {
        return httpServer != null
    }

    /**
     * Get the actual port the server is running on.
     */
    fun getActualPort(): Int {
        return actualPort
    }

    /**
     * Create a new diagram file and open it in the editor.
     * @param project The project to create the diagram in
     * @param relativePath The relative path from project root (e.g., "diagrams/my-diagram.drawio.svg")
     * @param fileType The file type: "svg", "png", or "xml" (defaults to "svg")
     * @param initialContent Optional initial XML content (if null, creates empty diagram)
     * @return The editor reference for the new diagram, or null if creation failed
     */
    fun createDiagram(
        project: Project,
        relativePath: String,
        fileType: String = "svg",
        initialContent: String? = null
    ): DiagramEditorReference? {
        return try {
            ApplicationManager.getApplication().runWriteAction<DiagramEditorReference?> {
                val projectPath = project.basePath ?: return@runWriteAction null
                val fullPath = "$projectPath/$relativePath"

                // Create directory if needed
                val file = java.io.File(fullPath)
                file.parentFile?.mkdirs()

                // Create initial content
                val content = initialContent ?: getEmptyDiagramXml()

                // Write file
                file.writeText(content)

                // Refresh VFS to pick up the new file
                val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .refreshAndFindFileByPath(fullPath)
                    ?: return@runWriteAction null

                // Open the file in the editor
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                    .openFile(virtualFile, true)

                // The editor will register itself when opened
                // Wait a bit for registration
                Thread.sleep(100)

                // Generate ID from file path (same as DiagramsEditor does)
                val id = Integer.toHexString(virtualFile.path.hashCode())
                getEditor(id)
            }
        } catch (e: Exception) {
            LOG.error("Failed to create diagram: $relativePath", e)
            null
        }
    }

    /**
     * Get empty diagram XML template.
     */
    private fun getEmptyDiagramXml(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" style="background-color: rgb(255, 255, 255);" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" width="1px" height="1px" viewBox="-0.5 -0.5 1 1" content="&lt;mxfile&gt;&lt;diagram id=&quot;new&quot; name=&quot;Page-1&quot;&gt;jZJNT4QwEIZ/DUcToOyHRxVdvZh4MN56qUNpaUKnJi2L/HuLFNi4xksSpp3pzNN3aKGEd9l41KrNngVIEm1FT8gjidYPd3u6zuAyg13sDKDRQgxoO4Nc/AKCNtFeCOhnRoOojOxmWHFdQ2VmTGvNx9lcYTLz2ukGFpBXSi71VyFMO9BHe+j8CaRp02S7PQ4nld6YE+lbLfjxhkiKifc0ijPPK0fQc3hTLkPe4xPazRv/kP8l8z/AOf8L&lt;/diagram&gt;&lt;/mxfile&gt;"><defs/></svg>"""
    }

    override fun dispose() {
        LOG.info("Disposing DiagramMcpService")
        stopServer()
        openEditors.clear()
    }

    companion object {
        val instance: DiagramMcpService
            get() = ApplicationManager.getApplication().getService(DiagramMcpService::class.java)
    }
}

/**
 * Reference to an open diagram editor.
 */
data class DiagramEditorReference(
    val editor: DiagramsEditor,
    val project: Project,
    val file: VirtualFile
)

/**
 * Information about a diagram.
 */
data class DiagramInfo(
    val id: String,
    val filePath: String,
    val relativePath: String,
    val fileName: String,
    val fileType: String,
    val project: String,
    val isOpen: Boolean,
    val isModified: Boolean
)
