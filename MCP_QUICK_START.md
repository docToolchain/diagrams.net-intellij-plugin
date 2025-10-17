# MCP Integration Quick Start Guide

## Overview

The diagrams.net IntelliJ Plugin can expose a REST API (MCP server) that allows LLMs like Claude Code to interact with diagrams directly within your IDE.

## Key Capabilities

- **Read Diagrams**: LLMs can read the XML content of any diagram file in your project
- **Modify Diagrams**: LLMs can update diagram structure, add/remove components, change connections
- **Auto-Open Files**: Diagrams are automatically opened in the editor when accessed via file path
- **Real-time Updates**: Changes made by LLM are immediately visible in the IntelliJ editor
- **Multi-Instance Support**: Multiple IntelliJ instances can run MCP servers on different ports

## File-Based Navigation

The MCP server supports intuitive file-based navigation:

```
# Get diagram by relative path
GET /api/diagrams/by-path?path=docs/architecture/system.drawio.svg

# Update diagram by relative path
PUT /api/diagrams/by-path/docs/architecture/system.drawio.svg
```

This allows Claude Code to work naturally:
- "Analyze the diagram at `docs/architecture/system.drawio.svg`"
- "Add a cache layer to `docs/architecture/system.drawio.svg`"
- "Update all diagrams in `docs/` to use the new color scheme"

## Setup Instructions

### 1. Enable MCP Server in IntelliJ

1. Open IntelliJ IDEA Settings
2. Navigate to: **Settings → Languages & Frameworks → Diagrams.net**
3. Check: **Enable MCP Server**
4. Configure: **Port** (default: 8765)
5. Click: **Apply** / **OK**

The server will start automatically when enabled.

### 2. Configure Claude Desktop

Edit your Claude Desktop config file:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

Add the MCP server configuration:

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

### 3. Verify Connection

Restart Claude Desktop and in Claude Code, you should be able to:

```
User: "List all diagram files in this project"

Claude: [Calls list_diagrams tool via MCP]
```

## Multiple IntelliJ Instances

If you have multiple IntelliJ instances open:

1. Each instance uses a different port (8765, 8766, 8767, etc.)
2. Each instance writes its info to `~/.diagrams-net-intellij-mcp/instances.json`
3. Configure multiple MCP servers in Claude Desktop:

```json
{
  "mcpServers": {
    "diagrams-main-project": {
      "url": "http://localhost:8765/api/mcp/info",
      "type": "rest",
      "description": "Main project"
    },
    "diagrams-plugin-dev": {
      "url": "http://localhost:8766/api/mcp/info",
      "type": "rest",
      "description": "Plugin development"
    }
  }
}
```

## Example Workflows

### Analyzing Architecture

```
User: "Analyze the system architecture in docs/architecture/system.drawio.svg"

Claude: [Opens and analyzes the diagram]
"The system architecture shows:
- 3-tier architecture with web, API, and database layers
- Message queue for async processing
- Redis cache for session management
- 5 microservices with well-defined boundaries"
```

### Updating Diagrams

```
User: "Add a new microservice called 'Notification Service' to the architecture diagram"

Claude: [Reads current diagram, generates updated XML with new component, updates the diagram]
"I've added the Notification Service to the architecture diagram at docs/architecture/system.drawio.svg.
It's connected to the Message Queue and has its own database. The diagram has been saved."
```

### Documentation Generation

```
User: "Create documentation for all diagrams in the docs/ folder"

Claude: [Iterates through all diagrams, analyzes each, generates markdown]
Creates: docs/DIAGRAMS.md with comprehensive documentation
```

### Consistency Checking

```
User: "Verify that all services listed in SERVICES.md are represented in the architecture diagram"

Claude: [Reads service list, reads diagram, compares]
"Found 2 services not represented in the diagram:
- Analytics Service
- Reporting Service
Would you like me to add them?"
```

## API Reference

### Core Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/status` | GET | Server status and project info |
| `/api/diagrams` | GET | List open diagrams |
| `/api/diagrams/by-path?path={path}` | GET | Find diagram by relative path |
| `/api/diagrams/by-path/{path}/content` | GET | Get diagram XML content |
| `/api/diagrams/update-by-path` | POST | Update diagram by path |
| `/api/mcp/info` | GET | MCP tool discovery |

### Key Parameters

- `path`: Relative path from project root (e.g., `docs/architecture/system.drawio.svg`)
- `open`: Auto-open diagram if not already open (default: `true`)
- `autoSave`: Automatically save after update (default: `true`)
- `includeAll`: Include all diagrams in project, not just open ones (default: `false`)

## Troubleshooting

### Server Not Starting

1. Check IntelliJ Event Log for errors
2. Verify port is not already in use
3. Check firewall settings
4. Try a different port number

### Claude Code Can't Connect

1. Verify MCP server is enabled in IntelliJ settings
2. Check Claude Desktop config file syntax
3. Restart Claude Desktop after config changes
4. Verify URL in config matches actual port
5. Check `~/.diagrams-net-intellij-mcp/instances.json` for actual port

### Diagram Not Updating

1. Ensure diagram is editable (not read-only)
2. Check IntelliJ Event Log for errors
3. Verify XML structure is valid
4. Try opening the diagram manually first

### Wrong Instance Connected

1. Check `~/.diagrams-net-intellij-mcp/instances.json`
2. Match project root to ensure correct instance
3. Use explicit port in Claude Desktop config
4. Close unused IntelliJ instances

## Supported File Formats

- `.drawio` - Draw.io XML files
- `.drawio.svg` - SVG files with embedded diagram
- `.drawio.png` - PNG files with embedded diagram
- `.drawio.xml` - XML diagram files
- `.dio`, `.dio.svg`, `.dio.png`, `.dio.xml` - Alternative extensions
- Auto-detected SVG/PNG files with embedded diagrams.net data

## Security Notes

- Server only listens on `127.0.0.1` (localhost)
- No authentication required (local-only access)
- Only diagrams in the project directory are accessible
- File system writes go through IntelliJ's VFS (respects IDE permissions)

## Performance Considerations

- Minimal overhead when server is idle
- Asynchronous request handling
- Efficient XML parsing for large diagrams
- File watching for external changes

## Future Enhancements

- WebSocket support for real-time bidirectional updates
- Diagram diff/preview before applying changes
- Multi-page diagram support
- Template-based diagram creation
- Diagram validation and linting
- Natural language diagram descriptions
- Automatic layout optimization

## Getting Help

- Plugin issues: https://github.com/docToolchain/diagrams.net-intellij-plugin/issues
- MCP protocol: https://modelcontextprotocol.io/
- diagrams.net format: https://www.diagrams.net/doc/
