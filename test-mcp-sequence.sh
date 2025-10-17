#!/bin/bash

# Test the exact MCP sequence that Claude Desktop would use

echo "=== Step 1: Initialize ==="
echo '{"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "claude-desktop", "version": "1.0"}}}' | python3 mcp-server-wrapper.py 8765

echo -e "\n\n=== Step 2: List Tools ==="
echo '{"jsonrpc": "2.0", "id": 2, "method": "tools/list"}' | python3 mcp-server-wrapper.py 8765 | python3 -m json.tool

echo -e "\n\n=== Step 3: Call update_diagram tool ==="
DIAGRAM_ID="7003d1fe"
XML_CONTENT='<mxfile><diagram>test</diagram></mxfile>'

echo "{\"jsonrpc\": \"2.0\", \"id\": 3, \"method\": \"tools/call\", \"params\": {\"name\": \"update_diagram\", \"arguments\": {\"id\": \"$DIAGRAM_ID\", \"xml\": \"$XML_CONTENT\"}}}" | python3 mcp-server-wrapper.py 8765 | python3 -m json.tool
