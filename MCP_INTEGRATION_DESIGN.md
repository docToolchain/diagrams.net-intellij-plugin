# MCP Integration Design for diagrams.net IntelliJ Plugin

## Overview

This document describes the design for adding a REST API service that acts as an MCP (Model Context Protocol) server, enabling LLMs like Claude to interact with diagrams.net diagrams directly within IntelliJ IDEA.

## Goals

1. Enable LLMs to read diagram content (XML embedded in SVG/PNG or standalone XML)
2. Allow LLMs to modify diagrams and push changes back to the editor
3. Support real-time diagram updates in the IntelliJ editor
4. Provide a configurable port for the REST service (to support multiple IntelliJ instances)
5. Support file-based navigation (by relative or absolute path)
6. Auto-open diagrams on demand if not already open
7. Follow MCP protocol patterns for tool discovery and invocation

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    IntelliJ IDEA Process                     │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         Diagrams.net Plugin                            │ │
│  │                                                        │ │
│  │  ┌──────────────────┐      ┌──────────────────────┐  │ │
│  │  │ DiagramsEditor   │◄────►│ DiagramsWebView      │  │ │
│  │  │                  │      │ (JCEF Browser)       │  │ │
│  │  └────────┬─────────┘      └──────────────────────┘  │ │
│  │           │                                           │ │
│  │           │                                           │ │
│  │  ┌────────▼──────────────────────────────────────┐   │ │
│  │  │     DiagramMcpService                         │   │ │
│  │  │  (Application Service)                        │   │ │
│  │  │  - Track open editors                         │   │ │
│  │  │  - Extract diagram XML                        │   │ │
│  │  │  - Update diagram content                     │   │ │
│  │  │  - Trigger reloads                            │   │ │
│  │  └────────┬──────────────────────────────────────┘   │ │
│  │           │                                           │ │
│  │  ┌────────▼──────────────────────────────────────┐   │ │
│  │  │     DiagramMcpHttpServer                      │   │ │
│  │  │  (Embedded HTTP Server - Ktor/NanoHTTPD)     │   │ │
│  │  │  - REST API endpoints                         │   │ │
│  │  │  - Port configuration                         │   │ │
│  │  │  - CORS handling                              │   │ │
│  │  └───────────────────────────────────────────────┘   │ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
└─────────────────────────────┬────────────────────────────────┘
                              │ HTTP/REST
                              │ (localhost:configurable-port)
                              │
┌─────────────────────────────▼────────────────────────────────┐
│                       LLM Client                              │
│                    (Claude Desktop with MCP)                  │
│                                                               │
│  - Discovers available diagrams                              │
│  - Reads diagram XML content                                 │
│  - Modifies diagram structure                                │
│  - Pushes updates back to IntelliJ                           │
└───────────────────────────────────────────────────────────────┘
```

## REST API Design

### Base URL
```
http://localhost:{configurable-port}/api/diagrams
```

### Endpoints

#### 1. Server Status & Info
```
GET /api/status
```

**Response:**
```json
{
  "status": "running",
  "port": 8765,
  "version": "0.2.7",
  "projectRoot": "/absolute/path/to/project",
  "projectName": "my-project-name",
  "openDiagrams": 3
}
```

#### 2. List Open Diagrams
```
GET /api/diagrams
```

**Query Parameters:**
- `includeAll` (boolean, default: false) - If true, includes all diagram files in the project, not just open ones

**Response:**
```json
{
  "diagrams": [
    {
      "id": "unique-id-based-on-file-path",
      "filePath": "/absolute/path/to/diagram.drawio.svg",
      "relativePath": "docs/architecture/diagram.drawio.svg",
      "fileName": "diagram.drawio.svg",
      "fileType": "svg",
      "project": "my-project-name",
      "isOpen": true,
      "isModified": false
    }
  ]
}
```

#### 3. Find Diagram by Path
```
GET /api/diagrams/by-path?path={relativePath}
```

**Query Parameters:**
- `path` (required) - Relative path from project root (e.g., `docs/architecture/system.drawio.svg`)
- `open` (boolean, default: true) - If true, opens the diagram in the editor if not already open

**Response:**
```json
{
  "id": "unique-id",
  "filePath": "/absolute/path/to/diagram.drawio.svg",
  "relativePath": "docs/architecture/diagram.drawio.svg",
  "fileName": "diagram.drawio.svg",
  "fileType": "svg",
  "project": "my-project-name",
  "isOpen": true,
  "isModified": false,
  "openedByRequest": true
}
```

**Use Case:**
Claude Code can say: "Let me open and analyze the diagram at `docs/architecture/system.drawio.svg`"

#### 4. Get Diagram Content by ID
```
GET /api/diagrams/{id}
```

**Response:**
```json
{
  "id": "unique-id",
  "filePath": "/absolute/path/to/diagram.drawio.svg",
  "relativePath": "docs/architecture/diagram.drawio.svg",
  "fileName": "diagram.drawio.svg",
  "fileType": "svg",
  "xml": "<?xml version=\"1.0\"?>...",
  "isModified": false,
  "metadata": {
    "pages": 1,
    "lastModified": "2025-10-17T10:30:00Z"
  }
}
```

**Note:** For SVG/PNG files, the embedded XML is extracted. For XML files, the content is returned directly.

#### 5. Get Diagram Content by Path
```
GET /api/diagrams/by-path/{relativePath}/content
```

**Alternative:**
```
POST /api/diagrams/get-by-path
Content-Type: application/json

{
  "path": "docs/architecture/system.drawio.svg",
  "open": true
}
```

**Response:** Same as endpoint #4

**Use Case:**
Claude Code can directly request: "Get me the content of `docs/architecture/system.drawio.svg`"

#### 6. Update Diagram Content by ID
```
PUT /api/diagrams/{id}
```

**Request Body:**
```json
{
  "xml": "<?xml version=\"1.0\"?>...",
  "autoSave": true
}
```

**Response:**
```json
{
  "success": true,
  "id": "unique-id",
  "message": "Diagram updated successfully"
}
```

**Behavior:**
- Updates the diagram in the editor
- If `autoSave` is true, saves the file immediately
- If `autoSave` is false, marks the file as modified (user must save manually)
- Triggers the webview to reload and display the updated diagram

#### 7. Update Diagram Content by Path
```
PUT /api/diagrams/by-path/{relativePath}
```

**Alternative:**
```
POST /api/diagrams/update-by-path
Content-Type: application/json

{
  "path": "docs/architecture/system.drawio.svg",
  "xml": "<?xml version=\"1.0\"?>...",
  "autoSave": true,
  "openIfClosed": true
}
```

**Response:**
```json
{
  "success": true,
  "id": "unique-id",
  "relativePath": "docs/architecture/system.drawio.svg",
  "message": "Diagram updated successfully",
  "wasOpened": false
}
```

**Use Case:**
Claude Code can say: "I'll update the diagram at `docs/architecture/system.drawio.svg` with these changes..."

#### 8. Get Diagram as SVG Export
```
GET /api/diagrams/{id}/export/svg
```

**Response:**
```xml
<svg>...</svg>
```

**Content-Type:** `image/svg+xml`

#### 9. Get Diagram as PNG Export
```
GET /api/diagrams/{id}/export/png
```

**Response:** Binary PNG data

**Content-Type:** `image/png`

#### 10. Server Info (MCP Discovery)
```
GET /api/mcp/info
```

**Response:**
```json
{
  "name": "diagrams-net-intellij-mcp",
  "version": "0.2.7",
  "description": "MCP server for diagrams.net integration in IntelliJ IDEA",
  "projectName": "my-project-name",
  "projectRoot": "/absolute/path/to/project",
  "port": 8765,
  "tools": [
    {
      "name": "list_diagrams",
      "description": "List all currently open diagram files in IntelliJ IDEA",
      "inputSchema": {
        "type": "object",
        "properties": {
          "includeAll": {
            "type": "boolean",
            "description": "Include all diagrams in project, not just open ones",
            "default": false
          }
        }
      }
    },
    {
      "name": "find_diagram_by_path",
      "description": "Find a diagram by its relative path from project root",
      "inputSchema": {
        "type": "object",
        "properties": {
          "path": {
            "type": "string",
            "description": "Relative path from project root (e.g., 'docs/architecture/system.drawio.svg')"
          },
          "open": {
            "type": "boolean",
            "description": "Open the diagram in editor if not already open",
            "default": true
          }
        },
        "required": ["path"]
      }
    },
    {
      "name": "get_diagram_by_id",
      "description": "Get the XML content of a specific diagram by its ID",
      "inputSchema": {
        "type": "object",
        "properties": {
          "id": {
            "type": "string",
            "description": "The unique identifier of the diagram"
          }
        },
        "required": ["id"]
      }
    },
    {
      "name": "get_diagram_by_path",
      "description": "Get the XML content of a diagram by its relative path",
      "inputSchema": {
        "type": "object",
        "properties": {
          "path": {
            "type": "string",
            "description": "Relative path from project root (e.g., 'docs/architecture/system.drawio.svg')"
          },
          "open": {
            "type": "boolean",
            "description": "Open the diagram in editor if not already open",
            "default": true
          }
        },
        "required": ["path"]
      }
    },
    {
      "name": "update_diagram_by_id",
      "description": "Update the content of a diagram with new XML using its ID",
      "inputSchema": {
        "type": "object",
        "properties": {
          "id": {
            "type": "string",
            "description": "The unique identifier of the diagram"
          },
          "xml": {
            "type": "string",
            "description": "The new XML content for the diagram"
          },
          "autoSave": {
            "type": "boolean",
            "description": "Whether to automatically save the file after update",
            "default": true
          }
        },
        "required": ["id", "xml"]
      }
    },
    {
      "name": "update_diagram_by_path",
      "description": "Update the content of a diagram with new XML using its relative path",
      "inputSchema": {
        "type": "object",
        "properties": {
          "path": {
            "type": "string",
            "description": "Relative path from project root (e.g., 'docs/architecture/system.drawio.svg')"
          },
          "xml": {
            "type": "string",
            "description": "The new XML content for the diagram"
          },
          "autoSave": {
            "type": "boolean",
            "description": "Whether to automatically save the file after update",
            "default": true
          },
          "openIfClosed": {
            "type": "boolean",
            "description": "Open the diagram in editor if not already open",
            "default": true
          }
        },
        "required": ["path", "xml"]
      }
    },
    {
      "name": "export_diagram_svg",
      "description": "Export a diagram as SVG",
      "inputSchema": {
        "type": "object",
        "properties": {
          "id": {
            "type": "string",
            "description": "The unique identifier of the diagram"
          }
        },
        "required": ["id"]
      }
    }
  ]
}
```

## Implementation Plan

### Phase 1: Core Infrastructure

#### 1.1 Add HTTP Server Dependency
Add to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-jackson:2.3.7")
    implementation("io.ktor:ktor-server-cors:2.3.7")
}
```

**Alternative (Lighter):** Use NanoHTTPD if Ktor is too heavy:
```kotlin
dependencies {
    implementation("org.nanohttpd:nanohttpd:2.3.1")
}
```

#### 1.2 Create Settings for MCP Server
Extend `DiagramsSettings.kt`:
```kotlin
data class DiagramsSettings(
    val uiTheme: DiagramsUiTheme = DiagramsUiTheme.DEFAULT,
    val uiMode: DiagramsUiMode = DiagramsUiMode.AUTO,
    val mcpServerEnabled: Boolean = false,
    val mcpServerPort: Int = 8765
) {
    companion object {
        val DEFAULT = DiagramsSettings()
    }
}
```

Update `DiagramsSettingsForm.kt` to add UI controls:
- Checkbox: "Enable MCP Server"
- Number field: "MCP Server Port" (default: 8765)
- Status indicator: "Server Status: Running/Stopped"

#### 1.3 Create DiagramMcpService (Application Service)
```kotlin
package de.docs_as_co.intellij.plugin.drawio.mcp

@Service
class DiagramMcpService : Disposable {
    private val openEditors = ConcurrentHashMap<String, DiagramEditorReference>()
    private var httpServer: DiagramMcpHttpServer? = null

    fun registerEditor(id: String, editor: DiagramsEditor) { }
    fun unregisterEditor(id: String) { }
    fun getEditor(id: String): DiagramsEditor? { }
    fun listDiagrams(): List<DiagramInfo> { }
    fun getDiagramContent(id: String): String? { }
    fun updateDiagramContent(id: String, xml: String, autoSave: Boolean): Boolean { }
    fun exportDiagramSvg(id: String): Promise<String> { }
    fun exportDiagramPng(id: String): Promise<ByteArray> { }

    fun startServer(port: Int) { }
    fun stopServer() { }

    companion object {
        val instance: DiagramMcpService
            get() = ApplicationManager.getApplication().getService(DiagramMcpService::class.java)
    }
}
```

#### 1.4 Create DiagramMcpHttpServer
```kotlin
package de.docs_as_co.intellij.plugin.drawio.mcp

class DiagramMcpHttpServer(private val port: Int, private val service: DiagramMcpService) {
    private var server: NettyApplicationEngine? = null

    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                jackson()
            }
            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
            }

            routing {
                get("/api/diagrams") { /* list diagrams */ }
                get("/api/diagrams/{id}") { /* get diagram */ }
                put("/api/diagrams/{id}") { /* update diagram */ }
                get("/api/diagrams/{id}/export/svg") { /* export svg */ }
                get("/api/diagrams/{id}/export/png") { /* export png */ }
                get("/api/mcp/info") { /* mcp info */ }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
    }
}
```

### Phase 2: Editor Integration

#### 2.1 Modify DiagramsEditor
Add registration/unregistration in the editor lifecycle:

```kotlin
class DiagramsEditor(private val project: Project, private val file: VirtualFile) : FileEditor, ... {
    private val editorId = generateEditorId(file)

    init {
        // ... existing init code ...
        DiagramMcpService.instance.registerEditor(editorId, this)
    }

    override fun dispose() {
        DiagramMcpService.instance.unregisterEditor(editorId)
        lifetimeDef.terminate(true)
    }

    // Public API for MCP service
    fun getXmlContent(): String? {
        return view.xmlContent.value
    }

    fun updateXmlContent(xml: String, autoSave: Boolean) {
        view.loadXmlLike(xml)
        if (autoSave) {
            // Trigger save
        }
    }

    fun exportAsSvg(): Promise<String> {
        return view.exportSvg()
    }

    fun exportAsPng(): Promise<ByteArray> {
        return view.exportPng()
    }

    companion object {
        fun generateEditorId(file: VirtualFile): String {
            return file.path.hashCode().toString(16)
        }
    }
}
```

#### 2.2 Add DiagramEditorReference
```kotlin
data class DiagramEditorReference(
    val editor: DiagramsEditor,
    val project: Project,
    val file: VirtualFile
)

data class DiagramInfo(
    val id: String,
    val filePath: String,
    val fileName: String,
    val fileType: String,
    val project: String,
    val isModified: Boolean
)
```

### Phase 3: MCP Protocol Support

#### 3.1 Create MCP Tool Definitions
```kotlin
data class McpToolInfo(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any>
)

data class McpServerInfo(
    val name: String,
    val version: String,
    val description: String,
    val tools: List<McpToolInfo>
)
```

#### 3.2 Implement MCP Endpoint Handlers
Map REST endpoints to MCP tool invocations

### Phase 4: Configuration UI

#### 4.1 Update DiagramsSettingsForm
Add UI components for:
- Enable/disable MCP server
- Port configuration
- Server status display
- Start/stop button (optional)

#### 4.2 Settings Change Listener
Listen for settings changes and start/stop server accordingly

### Phase 5: XML Extraction and Update

#### 5.1 Enhance DiagramsFileUtil
Add methods to extract and update XML from different file formats:
```kotlin
fun extractDiagramXml(file: VirtualFile): String?
fun updateDiagramXml(file: VirtualFile, xml: String, fileType: String): ByteArray
```

#### 5.2 Handle Different File Formats
- **XML files**: Direct read/write
- **SVG files**: Extract from `content` attribute, re-embed after update
- **PNG files**: Extract from metadata, re-embed after update

## Multi-Instance Support

### Port Management Strategy

**Problem:** Users may have multiple IntelliJ instances running simultaneously (different projects), each with the MCP server enabled.

**Solution:**

1. **Primary Port Configuration**: User configures a preferred port (default: 8765)

2. **Port Collision Handling**:
   - If the configured port is in use, automatically try the next available port (8766, 8767, etc.)
   - Display the actual port being used in the settings UI
   - Log the port to the IDE's event log

3. **Port Discovery File**:
   - Create a file in user's home directory: `~/.diagrams-net-intellij-mcp/instances.json`
   - Each instance registers itself with: `{ "port": 8765, "projectName": "my-project", "projectRoot": "/path/to/project", "pid": 12345 }`
   - Claude Code can read this file to discover all running instances

4. **Instance Selection**:
   - Claude Code working in project `/path/to/project` can match it to the correct port
   - Settings UI shows all running instances

### Example Discovery File
`~/.diagrams-net-intellij-mcp/instances.json`:
```json
{
  "instances": [
    {
      "port": 8765,
      "projectName": "my-main-project",
      "projectRoot": "/Users/john/projects/my-main-project",
      "pid": 12345,
      "lastSeen": "2025-10-17T10:30:00Z"
    },
    {
      "port": 8766,
      "projectName": "diagrams.net-intellij-plugin",
      "projectRoot": "/Users/john/projects/diagrams.net-intellij-plugin",
      "pid": 12346,
      "lastSeen": "2025-10-17T10:30:00Z"
    }
  ]
}
```

### Discovery Endpoint
```
GET /api/discover/all-instances
```

**Response:**
```json
{
  "currentInstance": {
    "port": 8765,
    "projectName": "my-main-project",
    "projectRoot": "/Users/john/projects/my-main-project"
  },
  "allInstances": [
    {
      "port": 8765,
      "projectName": "my-main-project",
      "projectRoot": "/Users/john/projects/my-main-project",
      "reachable": true
    },
    {
      "port": 8766,
      "projectName": "other-project",
      "projectRoot": "/Users/john/projects/other-project",
      "reachable": true
    }
  ]
}
```

## Configuration for Claude Desktop

### MCP Configuration File
`~/Library/Application Support/Claude/claude_desktop_config.json` (macOS):

**Option 1: Single Instance (Simple)**
```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "url": "http://localhost:8765/api/mcp/info",
      "type": "rest"
    }
  }
}
```

**Option 2: Multiple Instances (Advanced)**
```json
{
  "mcpServers": {
    "diagrams-net-intellij-main": {
      "url": "http://localhost:8765/api/mcp/info",
      "type": "rest",
      "description": "Main project diagrams"
    },
    "diagrams-net-intellij-plugin": {
      "url": "http://localhost:8766/api/mcp/info",
      "type": "rest",
      "description": "Plugin development diagrams"
    }
  }
}
```

**Option 3: Dynamic Discovery (Future Enhancement)**
Claude Code could automatically discover instances by:
1. Reading `~/.diagrams-net-intellij-mcp/instances.json`
2. Matching current working directory to project root
3. Automatically connecting to the correct instance

### Alternative: Stdio Transport
If MCP requires stdio transport instead of HTTP, we could implement:
- A separate process that communicates via stdio
- Bridge between stdio and the HTTP server

## Usage Examples with Claude Code

### Example 1: Analyzing an Existing Diagram

**User:** "Please analyze the system architecture diagram at `docs/architecture/system.drawio.svg`"

**Claude Code Flow:**
1. Calls `find_diagram_by_path` with path `docs/architecture/system.drawio.svg` and `open=true`
2. Receives diagram ID and metadata
3. Calls `get_diagram_by_path` with the same path
4. Receives the XML content
5. Parses the XML to understand:
   - Components (boxes, shapes)
   - Connections (arrows, lines)
   - Labels and text
   - Layers and grouping
6. Provides natural language analysis to user

**Response:** "The system architecture diagram shows a 3-tier architecture with:
- Frontend layer with React components
- Backend API layer with REST services
- Database layer with PostgreSQL
The diagram shows 5 main components with 8 connections..."

### Example 2: Modifying a Diagram

**User:** "Add a new 'Cache Layer' component between the API and Database in `docs/architecture/system.drawio.svg`"

**Claude Code Flow:**
1. Calls `get_diagram_by_path` to get current XML
2. Parses the XML structure
3. Identifies the API and Database components
4. Generates new XML with:
   - New rectangle shape for "Cache Layer"
   - Positioned between API and Database
   - New connections from API to Cache
   - New connections from Cache to Database
   - Updated existing connections
5. Calls `update_diagram_by_path` with modified XML and `autoSave=true`
6. Diagram automatically updates in IntelliJ

**Response:** "I've added a Cache Layer component (Redis) between the API and Database layers, with appropriate connections. The diagram has been updated and saved."

### Example 3: Creating Documentation from Diagrams

**User:** "Generate a markdown document describing all diagrams in the `docs/architecture/` folder"

**Claude Code Flow:**
1. Calls `list_diagrams` with `includeAll=true`
2. Filters for diagrams in `docs/architecture/`
3. For each diagram:
   - Calls `get_diagram_by_path`
   - Parses XML to extract component information
   - Analyzes structure and relationships
4. Generates comprehensive markdown documentation

**Response:** Creates a markdown file with:
- List of all architecture diagrams
- Description of each diagram's purpose
- Components and their relationships
- Cross-references between diagrams

### Example 4: Diagram Consistency Check

**User:** "Check if all services mentioned in `docs/services-list.md` are represented in `docs/architecture/system.drawio.svg`"

**Claude Code Flow:**
1. Reads `docs/services-list.md` to extract service names
2. Calls `get_diagram_by_path` for the architecture diagram
3. Parses XML to find all labeled shapes
4. Compares lists
5. Reports missing services
6. Optionally: offers to add missing services to the diagram

### Example 5: Batch Diagram Updates

**User:** "Update all diagrams in the project to use the new company color scheme: primary=#1E40AF, secondary=#10B981"

**Claude Code Flow:**
1. Calls `list_diagrams` with `includeAll=true`
2. For each diagram:
   - Calls `get_diagram_by_path`
   - Parses XML to find style attributes
   - Updates color values
   - Calls `update_diagram_by_path`
3. Reports completion

### Example 6: Working with Multiple Instances

**Scenario:** User has two IntelliJ instances open:
- Instance A (port 8765): main-project
- Instance B (port 8766): plugin-development

**User in Claude Code (in main-project directory):** "Show me the database schema diagram"

**Claude Code Flow:**
1. Reads `~/.diagrams-net-intellij-mcp/instances.json`
2. Matches current working directory to main-project
3. Connects to port 8765
4. Lists diagrams and finds `database-schema.drawio.svg`
5. Retrieves and displays content

## Security Considerations

1. **Localhost Only**: Server should only bind to `127.0.0.1`
2. **No Authentication (Initial)**: Since it's localhost, authentication is optional for v1
3. **CORS**: Allow only localhost origins
4. **Input Validation**: Validate XML structure before applying updates
5. **File System Access**: Only allow access to files already open in the editor

## Testing Strategy

1. **Unit Tests**: Test XML extraction/update logic
2. **Integration Tests**: Test HTTP endpoints with mock editors
3. **Manual Testing**: Test with actual Claude Desktop MCP integration
4. **Performance Testing**: Ensure minimal overhead when server is running

## Future Enhancements

1. **WebSocket Support**: Real-time updates when diagrams change in the editor
2. **Diff Support**: Show what changed when LLM updates a diagram
3. **Multiple Pages**: Support for multi-page diagrams
4. **Diagram Analysis**: Provide LLM-friendly descriptions of diagram structure
5. **Template Support**: Allow LLM to create diagrams from templates
6. **Authentication**: Token-based auth for remote access scenarios

## Implementation Checklist

- [ ] Add HTTP server dependency
- [ ] Create MCP settings UI
- [ ] Implement DiagramMcpService
- [ ] Implement DiagramMcpHttpServer
- [ ] Register/unregister editors
- [ ] Implement list diagrams endpoint
- [ ] Implement get diagram content endpoint
- [ ] Implement update diagram content endpoint
- [ ] Implement export endpoints
- [ ] Implement MCP info endpoint
- [ ] Add XML extraction for SVG
- [ ] Add XML extraction for PNG
- [ ] Add XML update for SVG
- [ ] Add XML update for PNG
- [ ] Test with Claude Desktop
- [ ] Update CLAUDE.md with MCP information
- [ ] Add documentation for end users

## Estimated Effort

- Phase 1: 2-3 days
- Phase 2: 1-2 days
- Phase 3: 1 day
- Phase 4: 1 day
- Phase 5: 2-3 days
- Testing & Documentation: 1-2 days

**Total: ~8-12 days of development**
