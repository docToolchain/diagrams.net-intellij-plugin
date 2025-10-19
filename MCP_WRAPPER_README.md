# MCP Server Wrapper

This wrapper enables Claude Desktop (and other MCP clients) to interact with diagrams.net diagrams in IntelliJ IDEA.

## Features

.
**Dual Mode Operation:**
  * MCP Protocol Mode for Claude Desktop integration
  * CLI Mode for manual testing and debugging

.
**Available MCP Tools:**
  * `list_diagrams` - List all open diagrams
  * `get_diagram_by_id` - Get diagram content with decoded, readable XML
  * `update_diagram` - Update diagram with new XML content

.
**XML Decoding:**
  * Automatically decodes base64+zlib compressed diagram data
  * Provides human-readable mxGraphModel XML structure
  * No need for MCP clients to implement decompression
  * Shows cells, geometry, styles, and connections clearly

.
**Real-Time Updates:**
  * Changes appear immediately in IntelliJ editor
  * Supports SVG, PNG, and XML diagram formats

## Installation

### Prerequisites

.
IntelliJ IDEA with diagrams.net plugin installed
.
MCP server enabled in plugin settings:
  ** Settings → Tools → Diagrams.net Integration
  ** Check "Enable MCP Server"
  ** Configure port (default: 8765)
.
At least one diagram open in the IDE

### Claude Desktop Configuration

Add this to your Claude Desktop MCP settings file:

**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

**Linux:** `~/.config/Claude/claude_desktop_config.json`

**Option 1: Default Port (8765)**

[source,json]
----
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
----

**Option 2: Custom Port**

[source,json]
----
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "python3",
      "args": [
        "/absolute/path/to/diagrams.net-intellij-plugin/mcp-server-wrapper.py",
        "9000"
      ]
    }
  }
}
----

**Note:** Replace `/absolute/path/to/` with the actual path to this repository.
The port argument is optional and defaults to 8765 if not specified.

### Claude Code Configuration

Create `.mcp.json` in your project root:

[source,json]
----
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "/absolute/path/to/diagrams.net-intellij-plugin/mcp-server-wrapper.py",
      "args": []
    }
  }
}
----

Then add to `~/.claude/settings.json`:

[source,json]
----
{
  "enableAllProjectMcpServers": true
}
----

**Important:**
.
Add `.mcp.json` to your `.gitignore` to keep it user-local
.
Restart Claude Code session after creating `.mcp.json`
.
Custom ports: Add port as argument in `args`: `["9000"]`

## CLI Mode (Manual Testing)

The wrapper includes a CLI mode for manual testing and debugging.

### Check Server Status

[source,bash]
----
./mcp-server-wrapper.py --status
----

**Output:**
[source,json]
----
{
  "status": "running",
  "port": 8765,
  "version": "0.2.7",
  "openDiagrams": 1
}
----

### List Open Diagrams

[source,bash]
----
./mcp-server-wrapper.py --show-diagrams
----

**Output:**
----
Open diagrams: 1
  • architecture.drawio.svg
    ID: abc123
    Path: docs/architecture.drawio.svg
    Project: my-project
----

### Get Diagram Content

[source,bash]
----
./mcp-server-wrapper.py --get-diagram abc123
----

**Output:**
----
Diagram: architecture.drawio.svg
ID: abc123
Path: /Users/you/project/docs/architecture.drawio.svg
Type: svg
Project: my-project

Diagram Structure (decoded):
<mxGraphModel dx="643" dy="706" grid="1" gridSize="10" guides="1">
  <root>
    <mxCell id="0"/>
    <mxCell id="1" parent="0"/>
    <mxCell id="2" value="API Gateway" style="rounded=1;..." vertex="1" parent="1">
      <mxGeometry x="100" y="100" width="120" height="60" as="geometry"/>
    </mxCell>
  </root>
</mxGraphModel>

Full XML Content (for updates):
<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="drawio-plugin">
  <diagram name="Page-1" id="0">jZNNb4MwDIZ/DcdK...</diagram>
</mxfile>
----

**Key Features:**
.
**Decoded XML** shows human-readable diagram structure with cells, geometry, and styles
.
**Full XML** contains the complete mxfile with base64+zlib compressed data for updates

### Update Diagram

Create an XML file with the new diagram content:

[source,bash]
----
cat > new-diagram.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net">
  <diagram name="Page-1">
    <mxGraphModel>
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
        <mxCell id="2" value="Updated!" style="rounded=1;whiteSpace=wrap;html=1;" vertex="1" parent="1">
          <mxGeometry x="100" y="100" width="120" height="60" as="geometry"/>
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
EOF

./mcp-server-wrapper.py --update-diagram abc123 new-diagram.xml
----

**Output:**
----
✓ Successfully updated diagram abc123
  Diagram updated successfully
----

### Custom Port

If your MCP server runs on a different port:

[source,bash]
----
./mcp-server-wrapper.py --show-diagrams --port 9000
----

## Usage with Claude Desktop

Once configured, Claude Desktop will have access to three tools:

### Example Conversation

**You:** "List all open diagrams"

**Claude:** Uses `list_diagrams` tool
----
I can see you have 1 open diagram:
- architecture.drawio.svg (ID: abc123, Path: docs/architecture.drawio.svg)
----

**You:** "Show me the content of the architecture diagram"

**Claude:** Uses `get_diagram_by_id` tool with id="abc123"
----
Here's the architecture diagram content:
[Shows diagram metadata and XML]
----

**You:** "Add a new component called 'API Gateway' to the diagram"

**Claude:**
.
Uses `get_diagram_by_id` to get current content
.
Modifies the XML to add the new component
.
Uses `update_diagram` to push changes back
----
I've added an 'API Gateway' component to your diagram.
The changes should now be visible in IntelliJ IDEA.
----

## Troubleshooting

### Server Not Running

**Error:** `Connection refused` or `Failed to connect`

**Solution:**
.
Open IntelliJ IDEA
.
Go to Settings → Tools → Diagrams.net Integration
.
Check "Enable MCP Server"
.
Configure the port number (default: 8765)
.
Ensure at least one diagram file is open

### No Diagrams Found

**Error:** `Open diagrams: 0`

**Solution:**
Open at least one `.drawio.svg`, `.drawio.png`, or `.drawio.xml` file in IntelliJ.

### Update Not Appearing

**Solution:**
.
Check that the diagram file is still open in IntelliJ
.
Verify the XML is valid diagrams.net format
.
Check IntelliJ logs for errors: `~/Library/Logs/JetBrains/*/idea.log`

### Claude Desktop Not Finding Tools

**Solution:**
.
Restart Claude Desktop after adding MCP configuration
.
Verify the absolute path in `claude_desktop_config.json` is correct
.
Check Claude Desktop logs for MCP errors

## Architecture

[source]
----
┌─────────────────────────────────────────────┐
│            Claude Desktop                    │
│         (MCP Client)                         │
└─────────────────┬───────────────────────────┘
                  │ JSON-RPC over stdio
                  │
┌─────────────────▼───────────────────────────┐
│       mcp-server-wrapper.py                  │
│    (Translates MCP ↔ REST)                  │
└─────────────────┬───────────────────────────┘
                  │ HTTP REST API
                  │ (localhost:8765)
┌─────────────────▼───────────────────────────┐
│        IntelliJ IDEA                         │
│   ┌─────────────────────────────────────┐   │
│   │  diagrams.net Plugin                │   │
│   │  - DiagramMcpHttpServer             │   │
│   │  - DiagramsEditor tracking          │   │
│   │  - Real-time updates                │   │
│   └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
----

## Files

.
`mcp-server-wrapper.py` - Main wrapper script (Python 3)
.
`mcp-server-wrapper.sh` - Deprecated shell wrapper (forwards to Python)
.
`MCP_WRAPPER_README.md` - This file

## See Also

.
`MCP_INTEGRATION_DESIGN.md` - Overall MCP integration design
.
`MCP_PHASE2_COMPLETE.md` - Implementation status
.
`test-phase2.sh` - Test script for REST API
