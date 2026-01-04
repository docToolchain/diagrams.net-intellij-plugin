# MCP Integration Implementation Summary

## What Has Been Designed

A comprehensive MCP (Model Context Protocol) integration for the diagrams.net IntelliJ plugin that enables Claude Code and other LLMs to interact with diagrams directly within the IDE.

## Key Features

### 1. File-Based Navigation ✅
- Access diagrams by relative path: `docs/architecture/system.drawio.svg`
- Auto-open files on demand
- Project-aware file resolution
- Works naturally with Claude Code's file-centric workflow

### 2. Multi-Instance Support ✅
- Configurable port (default: 8765)
- Automatic port collision handling (tries 8766, 8767, etc.)
- Instance discovery file: `~/.diagrams-net-intellij-mcp/instances.json`
- Claude Code can match project directory to correct instance

### 3. Comprehensive API ✅
- **10 REST endpoints** for diagram operations
- Get/update by ID or by path
- List all diagrams (open or all in project)
- Export to SVG/PNG
- Server status and discovery

### 4. Real-Time Updates ✅
- Changes pushed by LLM appear immediately in editor
- Supports auto-save or manual save
- Triggers webview reload on update

## Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│                   Claude Code                         │
│                                                       │
│  "Add cache layer to docs/arch/system.drawio.svg"   │
│                                                       │
└───────────────────┬───────────────────────────────────┘
                    │ MCP Tools
                    │ - find_diagram_by_path
                    │ - get_diagram_by_path
                    │ - update_diagram_by_path
                    │
                    ▼
┌──────────────────────────────────────────────────────┐
│           REST API (localhost:8765)                   │
│                                                       │
│  GET  /api/diagrams/by-path?path=...                │
│  POST /api/diagrams/update-by-path                   │
│  GET  /api/status                                    │
│  GET  /api/mcp/info                                  │
│                                                       │
└───────────────────┬───────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────┐
│          DiagramMcpService (IntelliJ)                │
│                                                       │
│  - Track open diagram editors                        │
│  - Resolve paths (relative → absolute)               │
│  - Extract/update XML from SVG/PNG/XML              │
│  - Open diagrams on demand                           │
│  - Trigger editor reloads                            │
│                                                       │
└───────────────────┬───────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────┐
│         DiagramsEditor / DiagramsWebView             │
│                                                       │
│  - JCEF browser with diagrams.net                    │
│  - Load/save XML content                             │
│  - Export to SVG/PNG                                 │
│  - Real-time rendering                               │
│                                                       │
└──────────────────────────────────────────────────────┘
```

## Implementation Components

### New Classes to Create

1. **DiagramMcpService** (Application Service)
   - Central coordination point
   - Editor registration/tracking
   - Path resolution
   - XML extraction/update
   - File operations

2. **DiagramMcpHttpServer** (HTTP Server)
   - Ktor or NanoHTTPD embedded server
   - REST endpoint handlers
   - JSON serialization
   - CORS handling

3. **DiagramMcpSettings** (Settings Extension)
   - Enable/disable MCP server
   - Port configuration
   - Server status display
   - Multi-instance management

4. **McpInstanceRegistry** (Instance Coordination)
   - Writes to `~/.diagrams-net-intellij-mcp/instances.json`
   - Reads other instances
   - Port collision detection
   - Stale instance cleanup

5. **DiagramPathResolver** (Path Resolution)
   - Relative → absolute path conversion
   - Project root detection
   - File existence validation
   - Diagram file detection

6. **DiagramXmlExtractor** (XML Operations)
   - Extract XML from SVG (content attribute)
   - Extract XML from PNG (metadata)
   - Update XML in SVG/PNG
   - Preserve file format specifics

### Modified Classes

1. **DiagramsEditor**
   - Register with MCP service on open
   - Unregister on close
   - Expose XML content getter
   - Expose XML content setter
   - Add file-save trigger method

2. **DiagramsSettings** / **DiagramsApplicationSettings**
   - Add MCP server enabled flag
   - Add MCP server port setting
   - Add settings changed listener

3. **DiagramsSettingsForm**
   - Add MCP server enable checkbox
   - Add port number field
   - Add server status indicator
   - Add "View All Instances" button

## API Endpoints Summary

| # | Endpoint | Method | Purpose |
|---|----------|--------|---------|
| 1 | `/api/status` | GET | Server info, project name, port |
| 2 | `/api/diagrams` | GET | List open/all diagrams |
| 3 | `/api/diagrams/by-path` | GET | Find diagram by path |
| 4 | `/api/diagrams/{id}` | GET | Get diagram by ID |
| 5 | `/api/diagrams/by-path/{path}/content` | GET | Get diagram by path |
| 6 | `/api/diagrams/{id}` | PUT | Update by ID |
| 7 | `/api/diagrams/update-by-path` | POST | Update by path |
| 8 | `/api/diagrams/{id}/export/svg` | GET | Export as SVG |
| 9 | `/api/diagrams/{id}/export/png` | GET | Export as PNG |
| 10 | `/api/mcp/info` | GET | MCP tool discovery |
| 11 | `/api/discover/all-instances` | GET | List all instances |

## MCP Tools

| Tool Name | Description | Primary Use Case |
|-----------|-------------|------------------|
| `list_diagrams` | List all diagrams | Discovery |
| `find_diagram_by_path` | Find by relative path | Open specific file |
| `get_diagram_by_id` | Get XML by ID | Read content |
| `get_diagram_by_path` | Get XML by path | Read content |
| `update_diagram_by_id` | Update by ID | Modify diagram |
| `update_diagram_by_path` | Update by path | Modify diagram |
| `export_diagram_svg` | Export as SVG | Save rendered output |

## Implementation Phases

### Phase 1: Core Infrastructure (2-3 days)
- [ ] Add HTTP server dependency (Ktor)
- [ ] Create `DiagramMcpSettings` class
- [ ] Update settings UI
- [ ] Create `DiagramMcpService` skeleton
- [ ] Create `DiagramMcpHttpServer` skeleton
- [ ] Implement basic server start/stop

### Phase 2: Editor Integration (1-2 days)
- [ ] Modify `DiagramsEditor` for registration
- [ ] Add XML content getters/setters
- [ ] Implement editor tracking in service
- [ ] Add lifecycle management

### Phase 3: Path-Based Access (2-3 days)
- [ ] Create `DiagramPathResolver`
- [ ] Implement path-to-file resolution
- [ ] Add auto-open functionality
- [ ] Implement `/api/diagrams/by-path` endpoint
- [ ] Implement update-by-path endpoint

### Phase 4: XML Extraction (2-3 days)
- [ ] Create `DiagramXmlExtractor`
- [ ] Extract XML from SVG files
- [ ] Extract XML from PNG files
- [ ] Update XML in SVG files
- [ ] Update XML in PNG files
- [ ] Handle XML files directly

### Phase 5: Multi-Instance Support (1-2 days)
- [ ] Create `McpInstanceRegistry`
- [ ] Implement instance file writing
- [ ] Implement port collision detection
- [ ] Add discovery endpoint
- [ ] Update settings UI for multi-instance

### Phase 6: MCP Protocol (1 day)
- [ ] Implement `/api/mcp/info` endpoint
- [ ] Define all tool schemas
- [ ] Test with Claude Desktop
- [ ] Document MCP configuration

### Phase 7: Testing & Documentation (1-2 days)
- [ ] Unit tests for path resolution
- [ ] Unit tests for XML extraction
- [ ] Integration tests for API
- [ ] Manual testing with Claude Code
- [ ] Update CLAUDE.md
- [ ] Create user documentation

**Total Estimated Effort: 10-16 days**

## Dependencies

### Required
- HTTP server library (Ktor recommended)
- Jackson for JSON (already in use)
- IntelliJ Platform SDK (already present)

### Optional
- WebSocket library (for future enhancements)
- XML validation library

## Configuration Files

### Plugin Settings
Stored in IntelliJ settings: `diagramsNet.xml`

```xml
<DiagramsApplicationSettings>
  <myPreviewSettings>
    <uiTheme>DEFAULT</uiTheme>
    <uiMode>AUTO</uiMode>
    <mcpServerEnabled>true</mcpServerEnabled>
    <mcpServerPort>8765</mcpServerPort>
  </myPreviewSettings>
</DiagramsApplicationSettings>
```

### Instance Registry
`~/.diagrams-net-intellij-mcp/instances.json`

```json
{
  "instances": [
    {
      "port": 8765,
      "projectName": "main-project",
      "projectRoot": "/Users/john/projects/main-project",
      "pid": 12345,
      "lastSeen": "2025-10-17T10:30:00Z"
    }
  ]
}
```

### Claude Desktop Config
`~/Library/Application Support/Claude/claude_desktop_config.json`

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

## Usage Scenarios

### Scenario 1: Diagram Analysis
```
User: "Analyze docs/architecture/system.drawio.svg"
→ Claude calls get_diagram_by_path
→ Parses XML, understands structure
→ Provides natural language analysis
```

### Scenario 2: Diagram Modification
```
User: "Add Redis cache to the architecture"
→ Claude calls get_diagram_by_path
→ Generates updated XML with new component
→ Claude calls update_diagram_by_path
→ Diagram updates in IntelliJ automatically
```

### Scenario 3: Batch Operations
```
User: "Update all diagrams to use new colors"
→ Claude calls list_diagrams(includeAll=true)
→ For each diagram:
  - get_diagram_by_path
  - modify XML
  - update_diagram_by_path
→ All diagrams updated
```

### Scenario 4: Documentation
```
User: "Document all architecture diagrams"
→ Claude calls list_diagrams
→ Filters for docs/architecture/
→ For each: get content, analyze, document
→ Creates comprehensive markdown
```

## Next Steps

1. **Review and Approve Design**
   - Confirm API design meets requirements
   - Verify multi-instance strategy
   - Approve file-based navigation approach

2. **Create Prototype**
   - Implement Phase 1 (basic server)
   - Test with simple endpoint
   - Verify Claude Desktop connection

3. **Iterate and Refine**
   - Add endpoints incrementally
   - Test each with Claude Code
   - Gather feedback and adjust

4. **Production Readiness**
   - Complete test coverage
   - Performance optimization
   - Security review
   - User documentation

## Questions to Resolve

1. **HTTP Server Library**: Ktor vs NanoHTTPD?
   - Ktor: More features, larger dependency
   - NanoHTTPD: Lightweight, simpler

2. **MCP Transport**: HTTP/REST vs stdio?
   - Current design assumes HTTP/REST
   - May need stdio adapter if required

3. **Authentication**: Add in v1 or defer?
   - Current: No auth (localhost only)
   - Future: Token-based auth for remote access?

4. **WebSocket**: Add in v1 or defer?
   - Current: HTTP polling
   - Future: WebSocket for real-time updates

## Success Metrics

- [ ] Claude Code can list diagrams in project
- [ ] Claude Code can read diagram XML by path
- [ ] Claude Code can update diagram and see changes in IntelliJ
- [ ] Multiple IntelliJ instances work simultaneously
- [ ] Port configuration works correctly
- [ ] Auto-open functionality works
- [ ] All file formats (SVG, PNG, XML) supported
- [ ] Performance overhead < 10MB memory, < 1% CPU when idle
- [ ] Documentation complete and accurate

## Resources

- **Design Doc**: `MCP_INTEGRATION_DESIGN.md` (detailed technical design)
- **Quick Start**: `MCP_QUICK_START.md` (user-facing guide)
- **This Summary**: `MCP_IMPLEMENTATION_SUMMARY.md` (developer overview)
