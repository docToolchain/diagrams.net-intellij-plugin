#!/usr/bin/env python3
"""
MCP Server Wrapper for Claude Desktop
This script acts as an MCP server that proxies to the IntelliJ plugin's HTTP API
"""

import sys
import json
import urllib.request
import urllib.error

PORT = sys.argv[1] if len(sys.argv) > 1 else "8765"
BASE_URL = f"http://localhost:{PORT}"

def call_api(endpoint, method="GET", data=None):
    """Call the HTTP API"""
    try:
        url = f"{BASE_URL}{endpoint}"
        if method == "GET":
            req = urllib.request.Request(url)
        else:
            req = urllib.request.Request(
                url,
                data=json.dumps(data).encode('utf-8') if data else None,
                headers={"Content-Type": "application/json"},
                method=method
            )

        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        return {"error": str(e)}

def handle_initialize(request_id):
    """Handle initialize request"""
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "protocolVersion": "2024-11-05",
            "capabilities": {
                "tools": {}
            },
            "serverInfo": {
                "name": "diagrams-net-intellij-mcp",
                "version": "0.2.7"
            }
        }
    }

def handle_tools_list(request_id):
    """Handle tools/list request"""
    info = call_api("/api/mcp/info")
    tools = info.get("tools", [])
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "tools": tools
        }
    }

def handle_list_diagrams(request_id):
    """Handle list_diagrams tool call"""
    result = call_api("/api/diagrams")
    diagrams = result.get("diagrams", [])

    text = f"Open diagrams: {len(diagrams)}\n"
    for d in diagrams:
        text += f"- {d['fileName']} ({d['id']})\n"

    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "content": [
                {
                    "type": "text",
                    "text": text.strip()
                }
            ]
        }
    }

def handle_get_diagram_by_id(request_id, arguments):
    """Handle get_diagram_by_id tool call"""
    diagram_id = arguments.get("id")
    result = call_api(f"/api/diagrams/{diagram_id}")

    if "error" in result:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": -32000,
                "message": result.get("message", "Diagram not found")
            }
        }

    # Format the diagram info as text
    text = json.dumps(result, indent=2)

    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "content": [
                {
                    "type": "text",
                    "text": text
                }
            ]
        }
    }

def handle_create_diagram(request_id, arguments):
    """Handle create_diagram tool call"""
    project = arguments.get("project")
    path = arguments.get("path")
    file_type = arguments.get("fileType", "svg")
    content = arguments.get("content")

    if not project or not path:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": -32602,
                "message": "Missing required parameters: project and path"
            }
        }

    payload = {
        "project": project,
        "path": path,
        "fileType": file_type
    }
    if content:
        payload["content"] = content

    result = call_api("/api/diagrams", "POST", payload)

    if "error" in result:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": -32000,
                "message": result.get("message", "Failed to create diagram")
            }
        }

    text = f"Successfully created diagram: {result.get('fileName', path)}"
    if result.get("success"):
        text += f"\nID: {result.get('id')}"
        text += f"\nFile path: {result.get('filePath')}"
        text += f"\nFile type: {result.get('fileType')}"

    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "content": [
                {
                    "type": "text",
                    "text": text
                }
            ]
        }
    }

def handle_update_diagram(request_id, arguments):
    """Handle update_diagram tool call"""
    diagram_id = arguments.get("id")
    xml = arguments.get("xml")

    if not diagram_id or not xml:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": -32602,
                "message": "Missing required parameters: id and xml"
            }
        }

    result = call_api(f"/api/diagrams/{diagram_id}", "PUT", {"xml": xml})

    if "error" in result:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": -32000,
                "message": result.get("message", "Failed to update diagram")
            }
        }

    text = f"Successfully updated diagram {diagram_id}"
    if result.get("success"):
        text += f"\nMessage: {result.get('message', 'Update completed')}"

    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "content": [
                {
                    "type": "text",
                    "text": text
                }
            ]
        }
    }

def main():
    """Main loop - read JSON-RPC requests from stdin, write responses to stdout"""
    for line in sys.stdin:
        try:
            request = json.loads(line)
            method = request.get("method")
            request_id = request.get("id")
            params = request.get("params", {})

            response = None

            if method == "initialize":
                response = handle_initialize(request_id)
            elif method == "tools/list":
                response = handle_tools_list(request_id)
            elif method == "tools/call":
                tool_name = params.get("name")
                arguments = params.get("arguments", {})

                if tool_name == "create_diagram":
                    response = handle_create_diagram(request_id, arguments)
                elif tool_name == "list_diagrams":
                    response = handle_list_diagrams(request_id)
                elif tool_name == "get_diagram_by_id":
                    response = handle_get_diagram_by_id(request_id, arguments)
                elif tool_name == "update_diagram":
                    response = handle_update_diagram(request_id, arguments)
                else:
                    response = {
                        "jsonrpc": "2.0",
                        "id": request_id,
                        "error": {
                            "code": -32601,
                            "message": f"Unknown tool: {tool_name}"
                        }
                    }
            else:
                response = {
                    "jsonrpc": "2.0",
                    "id": request_id,
                    "error": {
                        "code": -32601,
                        "message": f"Unknown method: {method}"
                    }
                }

            if response:
                print(json.dumps(response), flush=True)

        except Exception as e:
            # Log error to stderr (will go to Claude logs)
            print(f"Error processing request: {e}", file=sys.stderr, flush=True)
            continue

if __name__ == "__main__":
    main()
