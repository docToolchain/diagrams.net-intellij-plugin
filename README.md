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

This plugin exposes a Model Context Protocol (MCP) server that allows AI assistants like Claude to interact with diagrams in your IDE. The MCP server provides tools to create, list, view, and update diagrams programmatically.

### Features

The MCP server provides the following tools:
- `create_diagram` - Create a new diagram file (supports .drawio.svg, .drawio.png, .drawio.xml)
- `list_diagrams` - List all open diagrams in the IDE
- `get_diagram_by_id` - Retrieve XML content of a specific diagram
- `update_diagram` - Update diagram content and save changes

### Configuration

The plugin automatically starts an HTTP server on port 8765 (configurable in settings). Two integration methods are available:

#### Method 1: Direct SSE Integration (Claude Code)

Claude Code supports HTTP/SSE MCP servers natively. Add this to your MCP settings:

```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "url": "http://localhost:8765/mcp/sse"
    }
  }
}
```

#### Method 2: Python Wrapper (Claude Desktop)

Claude Desktop requires stdio-based MCP servers. Use the included Python wrapper:

1. Add to `claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "python3",
      "args": ["/path/to/diagrams.net-intellij-plugin/mcp-server-wrapper.py", "8765"]
    }
  }
}
```

2. Ensure the IntelliJ IDE with the plugin is running
3. Restart Claude Desktop to load the MCP server

### Multiple IDE Instances

To use multiple IDE instances simultaneously, configure each instance to use a different port in the plugin settings, then update your MCP configuration accordingly.

### REST API

The plugin also exposes a REST API for direct HTTP access:

- `GET /api/status` - Server status
- `POST /api/diagrams` - Create new diagram
- `GET /api/diagrams` - List all diagrams
- `GET /api/diagrams/{id}` - Get diagram by ID
- `PUT /api/diagrams/{id}` - Update diagram content
- `GET /api/mcp/info` - MCP tool definitions

Example:
```bash
curl http://localhost:8765/api/status
```


