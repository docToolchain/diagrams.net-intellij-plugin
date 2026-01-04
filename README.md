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

##### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS):

```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "python3",
      "args": [
        "/absolute/path/to/diagrams.net-intellij-plugin/mcp-server-wrapper.py"
      ]
    }
  }
}
```

##### Claude Code

Create `.mcp.json` in your project root:

```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "/absolute/path/to/diagrams.net-intellij-plugin/mcp-server-wrapper.py",
      "args": []
    }
  }
}
```

Add to `~/.claude/settings.json`:

```json
{
  "enableAllProjectMcpServers": true
}
```

**Important:** Add `.mcp.json` to your `.gitignore` to keep it user-local.

### REST API

The plugin exposes a REST API for direct HTTP access:

**Core Endpoints:**
- `GET /api/status` - Server status and info
- `GET /api/diagrams` - List all open diagrams
- `GET /api/diagrams/{id}` - Get diagram with decoded XML
- `PUT /api/diagrams/{id}` - Update diagram content

**Example:**
```bash
# Check server status
curl http://localhost:8765/api/status

# Get diagram with decoded XML
curl http://localhost:8765/api/diagrams/{id} | jq
```

### Documentation

Detailed documentation:
- [MCP Quick Start Guide](MCP_QUICK_START.md) - Setup and usage
- [MCP API Reference](MCP_API_REFERENCE.md) - Complete API documentation
- [MCP Wrapper README](MCP_WRAPPER_README.md) - CLI usage and testing
- [Phase 3 Complete](MCP_PHASE3_COMPLETE.md) - XML decoding implementation details


