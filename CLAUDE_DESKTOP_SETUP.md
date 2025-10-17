# Claude Desktop MCP Integration Guide

## Overview

This guide will help you test the diagrams.net IntelliJ plugin MCP server with Claude Desktop.

## Prerequisites

✅ MCP server is running in IntelliJ (port 8765)
✅ At least one diagram is open in the test IDE
✅ Claude Desktop is installed

## Configuration

The MCP server has been configured in Claude Desktop:

**Location:** `~/Library/Application Support/Claude/claude_desktop_config.json`

**Configuration:**
```json
{
  "globalShortcut": "",
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "/Users/ascheman/wrk/docToolchain/diagrams.net-intellij-plugin/mcp-server-wrapper.sh",
      "args": ["8765"]
    }
  }
}
```

## Testing Steps

### Step 1: Verify IntelliJ MCP Server is Running

```bash
# Check server status
curl http://localhost:8765/api/status | jq

# List open diagrams
curl http://localhost:8765/api/diagrams | jq
```

Expected: You should see the server running and at least one diagram listed.

### Step 2: Restart Claude Desktop

**IMPORTANT:** You must restart Claude Desktop for the MCP configuration to take effect.

1. Quit Claude Desktop completely (⌘Q)
2. Reopen Claude Desktop
3. Wait for it to fully load

### Step 3: Verify MCP Server Connection

In Claude Desktop, check the MCP server status:

1. Look for the **hammer icon** (🔨) in the bottom-left corner
2. Click it to see available MCP servers
3. You should see **"diagrams-net-intellij"** listed

If you see any errors:
- Check that the test IDE is still running
- Verify port 8765 is accessible: `lsof -i :8765`
- Check Claude Desktop logs in Console.app

### Step 4: Test with Claude

Try these prompts in Claude Desktop:

#### Prompt 1: List Diagrams
```
Can you list all the diagrams that are currently open in IntelliJ?
```

**Expected Response:**
Claude should use the `list_diagrams` tool and show you:
- Diagram ID
- File path
- File name
- Project name
- File type (svg/png/xml)

#### Prompt 2: Get Diagram Content
```
Can you show me the XML content of the diagram with ID <diagram-id>?
```

(Replace `<diagram-id>` with the actual ID from the previous response)

**Expected Response:**
Claude should use the `get_diagram_by_id` tool and display:
- Full diagram metadata
- XML content of the diagram
- Information about the diagram structure

#### Prompt 3: Analyze Diagram
```
Based on the diagram XML, can you describe what this diagram shows?
```

**Expected Response:**
Claude should analyze the XML structure and describe:
- What shapes/elements are in the diagram
- How they're connected
- The overall structure/purpose

## Available MCP Tools

The following tools are available to Claude:

### 1. `list_diagrams`
**Description:** List all currently open diagram files in IntelliJ IDEA
**Parameters:** None
**Returns:** Array of diagram metadata

### 2. `get_diagram_by_id`
**Description:** Get the XML content of a specific diagram by its ID
**Parameters:**
- `id` (string, required): The unique identifier of the diagram
**Returns:** Full diagram details including XML content

## Troubleshooting

### Issue: MCP Server Not Showing in Claude Desktop

**Solution:**
1. Verify configuration file is correct:
   ```bash
   cat ~/Library/Application\ Support/Claude/claude_desktop_config.json
   ```
2. Check script is executable:
   ```bash
   ls -l /Users/ascheman/wrk/docToolchain/diagrams.net-intellij-plugin/mcp-server-wrapper.sh
   ```
3. Restart Claude Desktop completely

### Issue: "Server Not Responding" Error

**Solution:**
1. Verify IntelliJ test IDE is running
2. Check MCP server is enabled in IntelliJ settings
3. Verify port 8765 is listening:
   ```bash
   curl http://localhost:8765/api/status
   ```

### Issue: "Tool Execution Failed" Error

**Solution:**
1. Check the diagram is still open in IntelliJ
2. Verify the diagram ID is correct
3. Check IDE logs for errors:
   ```bash
   tail -50 /Users/ascheman/wrk/docToolchain/diagrams.net-intellij-plugin/build/idea-sandbox/IC-2025.1/log/idea.log
   ```

## Direct Testing (Without Claude Desktop)

You can also test the MCP wrapper directly:

```bash
# Test initialize
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | /Users/ascheman/wrk/docToolchain/diagrams.net-intellij-plugin/mcp-server-wrapper.sh

# Test list diagrams
echo '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"list_diagrams","arguments":{}}}' | /Users/ascheman/wrk/docToolchain/diagrams.net-intellij-plugin/mcp-server-wrapper.sh
```

## Next Steps

Once you've verified the MCP integration works with Claude Desktop:

1. **Try complex interactions** - Ask Claude to compare multiple diagrams
2. **Test diagram updates** - Ask Claude to suggest improvements (Phase 3 will enable actual updates via MCP)
3. **Explore use cases** - Documentation generation, diagram analysis, architecture review

## Example Session

Here's an example conversation you could have with Claude Desktop:

**You:** "What diagrams are currently open in my IntelliJ project?"

**Claude:** *Uses list_diagrams tool*
"You have 1 diagram open:
- File: tst.drawio.svg
- Project: test-diagrams-net-mcp
- Type: SVG format
- ID: 2136f581"

**You:** "Can you analyze that diagram and tell me what it shows?"

**Claude:** *Uses get_diagram_by_id tool*
"Based on the XML content, this diagram contains:
- A green rounded rectangle labeled 'API Test - Success!'
- A blue rounded rectangle labeled 'Phase 2 Working!'
- These elements appear to be test/demo shapes arranged vertically"

**You:** "Can you suggest improvements to make this diagram more informative?"

**Claude:** "Here are some suggestions:
1. Add arrows to show relationships between components
2. Include labels describing what each element represents
3. Add a title block explaining the diagram's purpose
4. Use consistent styling and colors for related elements"

## Support

If you encounter issues:

1. Check the test IDE is running with an open diagram
2. Verify the MCP server status in IntelliJ settings
3. Review the wrapper script logs
4. Check Claude Desktop logs in Console.app

For more information, see:
- `TESTING_GUIDE.md` - Comprehensive testing documentation
- `MCP_INTEGRATION_DESIGN.md` - Technical design details
- `MCP_API_REFERENCE.md` - Complete API documentation
