# Diagrams.net Integration for IntelliJ

[![Build Status (GitHub Workflow Build)](https://github.com/docToolchain/diragrams.net-intellij-plugin/workflows/Build/badge.svg?branch=main)](https://github.com/docToolchain/diragrams.net-intellij-plugin/actions?query=workflow%3ABuild+branch%3Amain)
[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/15635-diagrams-net-integration.svg)](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15635-diagrams-net-integration.svg)](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration)

<!-- Plugin description -->
This unofficial extension integrates [diagrams.net](https://app.diagrams.net/) (formerly known as draw.io) directly into IntelliJ and other JetBrains IDEs based on it like PyCharm, RubyMine and WebStorm.
It supports diagram files with the extensions `.drawio.(svg|png|xml)` and `.dio.(svg|png|xml)`.
It also auto-detects editable PNGs and SVGs created with diagrams.net.

The editor uses an offline version of diagrams.net by default, therefore it works without an internet connection and content stays local in your IDE.
<!-- Plugin description end -->

## About

This plugin is still an early version and experimental.
If you like, you can help to evolve it.

![screenshot](images/drawioscreenshot.jpg)

## Installation

Releases are available on the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration). 
Use [Install plugin from repository](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_repo) to install them.

For pre-releases, either 
- download them from the [GitHub releases](https://github.com/docToolchain/diagrams.net-intellij-plugin/releases) and use [Install plugin from disk](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk) or 
- add the URL `https://plugins.jetbrains.com/plugins/eap/list?pluginId=15635` as a [custom plugin repository to your IDE](https://www.jetbrains.com/help/idea/managing-plugins.html#repos).

## References

* https://desk.draw.io/support/solutions/articles/16000042544-embed-mode
* https://github.com/jgraph/drawio-integration
* https://github.com/hediet/vscode-drawio

## Authors

[![](https://img.shields.io/twitter/follow/RalfDMueller.svg?style=social)](https://twitter.com/intent/follow?screen_name=RalfDMueller)

[![](https://img.shields.io/twitter/follow/hediet_dev.svg?style=social)](https://twitter.com/intent/follow?screen_name=hediet_dev)

[![](https://img.shields.io/twitter/follow/ahus1de.svg?style=social)](https://twitter.com/intent/follow?screen_name=ahus1de)

## Docs

An architecture overview can be found at https://drawio-intellij-plugin.netlify.app/ .

## FAQ

### How do I build and run this project?

For development purpose, clone the project locally and start it with the command

`./gradlew runIde`

This will build the plugin and start an Instance of IntelliJ with the plugin already installed.
You can even start this in debug mode.

## MCP Integration (Model Context Protocol)

This plugin exposes a Model Context Protocol (MCP) server that allows AI assistants like Claude to interact with diagrams in your IDE.
The MCP server provides tools to list, view, and update diagrams programmatically.

### Features

**Available MCP Tools:**
- `list_diagrams` - List all open diagrams in the IDE
- `get_diagram_by_id` - Get diagram content with **decoded, readable XML**
- `update_diagram` - Update diagram content and save changes

**XML Decoding:**
- Automatically decodes base64+zlib compressed diagram data
- Provides human-readable mxGraphModel XML structure
- No need for MCP clients to implement decompression
- Shows cells, geometry, styles, and connections clearly

**Real-Time Updates:**
- Changes appear immediately in the IntelliJ editor
- Supports SVG, PNG, and XML diagram formats

### Quick Setup

#### 1. Enable MCP Server in IntelliJ

1. Open IntelliJ IDEA Settings (Preferences on macOS)
2. Navigate to: **Settings → Tools → Diagrams.net Integration**
3. Check: **Enable MCP Server**
4. Configure: **MCP Server Port** (default: 8765)
5. Click: **Apply** / **OK**

The server will start automatically when enabled and a diagram file is open.

#### 2. Configure Your MCP Client

##### Claude Code (Recommended: Direct HTTP)

Claude Code supports HTTP transport directly. Add to `.claude/mcp.json` in your project:

```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "type": "http",
      "url": "http://localhost:${DIAGRAMS_NET_MCP_PORT_CURRENT:-8765}/mcp"
    }
  }
}
```

Or via CLI (use single quotes to preserve `${}`):
```bash
claude mcp add --transport http diagrams-net-intellij 'http://localhost:${DIAGRAMS_NET_MCP_PORT_CURRENT:-8765}/mcp'
```

##### Claude Desktop (Requires Wrapper)

Claude Desktop only supports stdio transport. Use the wrapper script.

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS):

```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "python3",
      "args": [
        "/absolute/path/to/diagrams.net-intellij-plugin/mcp-server-wrapper.py"
      ],
      "env": {
        "DIAGRAMS_NET_MCP_PORT": "8765"
      }
    }
  }
}
```

The wrapper acts as a stdio ↔ HTTP proxy, forwarding JSON-RPC messages to the `/mcp` endpoint.

#### Per-IDE Port Assignment

Each JetBrains IDE type automatically gets a unique port to allow running multiple IDEs simultaneously:

| IDE | Product Code | Default Port |
|-----|--------------|--------------|
| IntelliJ IDEA | IC/IU | 8765 (base) |
| WebStorm | WS | 8766 |
| PyCharm | PY/PC | 8767 |
| GoLand | GO | 8768 |
| Rider | RD | 8769 |
| RubyMine | RM | 8770 |
| CLion | CL | 8771 |
| PhpStorm | PS | 8772 |

#### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DIAGRAMS_NET_MCP_PORT` | Base port in IDE settings | `8765` |
| `DIAGRAMS_NET_MCP_PORT_<CODE>` | Override port for specific IDE (e.g., `_WS`, `_PY`) | - |
| `DIAGRAMS_NET_MCP_PORT_CURRENT` | Exported by plugin for Claude Code discovery | - |

**Claude Code Integration:**

The plugin exports `DIAGRAMS_NET_MCP_PORT_CURRENT` to the IDE environment.
Terminals opened in IntelliJ inherit this, so Claude Code can use:

```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "type": "http",
      "url": "http://localhost:${DIAGRAMS_NET_MCP_PORT_CURRENT:-8765}/mcp"
    }
  }
}
```

### MCP Streamable HTTP Transport (Direct Access)

The plugin supports the **MCP Streamable HTTP Transport** specification, allowing MCP clients to connect directly via HTTP without the Python wrapper.

**Endpoint:** `POST /mcp`

This endpoint accepts JSON-RPC 2.0 requests and implements the full MCP protocol:
- `initialize` - Start MCP session, returns server capabilities
- `tools/list` - List available tools with JSON Schema
- `tools/call` - Execute a tool (list_diagrams, get_diagram_by_id, update_diagram)
- `ping` - Health check

**Example:**
```bash
# Initialize MCP session
curl -X POST http://localhost:8765/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {"name": "test", "version": "1.0"}
    }
  }'

# List available tools
curl -X POST http://localhost:8765/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "id": 2, "method": "tools/list"}'

# Call list_diagrams tool
curl -X POST http://localhost:8765/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {"name": "list_diagrams", "arguments": {}}
  }'
```

For testing, use the included `mcp-test-requests.http` file with IntelliJ's HTTP Client.
