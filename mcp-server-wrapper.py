#!/usr/bin/env python3
"""
MCP Server Wrapper for diagrams.net IntelliJ Plugin

This script can run in two modes:
1. MCP Protocol Mode (default): Acts as an MCP server for Claude Desktop
2. CLI Mode: Manual testing with commands like --show-diagrams

Usage:
  # MCP Protocol Mode (for Claude Desktop)
  ./mcp-server-wrapper.py           # Uses default port 8765
  ./mcp-server-wrapper.py 9000      # Uses custom port 9000

  # CLI Mode (for manual testing)
  ./mcp-server-wrapper.py --help
  ./mcp-server-wrapper.py --show-diagrams
  ./mcp-server-wrapper.py --show-diagrams --port 9000
  ./mcp-server-wrapper.py --get-diagram <id>
  ./mcp-server-wrapper.py --update-diagram <id> <xml-file>
  ./mcp-server-wrapper.py --status
"""

import sys
import json
import urllib.request
import urllib.error
import argparse

DEFAULT_PORT = "8765"

def call_api(base_url, endpoint, method="GET", data=None):
    """Call the HTTP API"""
    try:
        url = f"{base_url}{endpoint}"
        if method == "GET":
            req = urllib.request.Request(url)
        else:
            req = urllib.request.Request(
                url,
                data=json.dumps(data).encode('utf-8') if data else None,
                headers={"Content-Type": "application/json"},
                method=method
            )

        with urllib.request.urlopen(req, timeout=5) as response:
            return json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        try:
            error_body = json.loads(e.read().decode('utf-8'))
            return {"error": error_body.get("error", str(e)), "message": error_body.get("message", "")}
        except:
            return {"error": f"HTTP {e.code}", "message": str(e)}
    except Exception as e:
        return {"error": str(e)}

def get_mcp_tools():
    """Define available MCP tools"""
    return [
        {
            "name": "list_diagrams",
            "description": "List all open diagrams in IntelliJ IDEA",
            "inputSchema": {
                "type": "object",
                "properties": {},
                "required": []
            }
        },
        {
            "name": "get_diagram_by_id",
            "description": "Get diagram content and metadata by ID",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "id": {
                        "type": "string",
                        "description": "The diagram ID (from list_diagrams)"
                    }
                },
                "required": ["id"]
            }
        },
        {
            "name": "update_diagram",
            "description": "Update diagram content with new XML",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "id": {
                        "type": "string",
                        "description": "The diagram ID"
                    },
                    "xml": {
                        "type": "string",
                        "description": "The new diagram XML content"
                    }
                },
                "required": ["id", "xml"]
            }
        }
    ]

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
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "tools": get_mcp_tools()
        }
    }

def handle_list_diagrams(request_id, base_url):
    """Handle list_diagrams tool call"""
    result = call_api(base_url, "/api/diagrams")

    if "error" in result:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": -32000,
                "message": result.get("message", result.get("error", "Failed to list diagrams"))
            }
        }

    diagrams = result.get("diagrams", [])

    text = f"Open diagrams: {len(diagrams)}\n"
    for d in diagrams:
        text += f"- {d['fileName']} (ID: {d['id']}, Path: {d.get('relativePath', 'N/A')})\n"

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

def handle_get_diagram_by_id(request_id, base_url, arguments):
    """Handle get_diagram_by_id tool call"""
    diagram_id = arguments.get("id")
    if not diagram_id:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": -32602,
                "message": "Missing required parameter: id"
            }
        }

    result = call_api(base_url, f"/api/diagrams/{diagram_id}")

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
    xml = result.get("xml", "")
    decodedXml = result.get("decodedXml", "")

    text = f"Diagram: {result.get('fileName', 'Unknown')}\n"
    text += f"ID: {result.get('id', 'N/A')}\n"
    text += f"Path: {result.get('filePath', 'N/A')}\n"
    text += f"Type: {result.get('fileType', 'N/A')}\n"
    text += f"Project: {result.get('project', 'N/A')}\n\n"

    # Show decoded diagram structure if available (human-readable)
    if decodedXml:
        text += f"Diagram Structure (decoded):\n{decodedXml}\n\n"

    # Include full XML for update operations
    text += f"Full XML (for updates):\n{xml}"

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

def handle_update_diagram(request_id, base_url, arguments):
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

    result = call_api(base_url, f"/api/diagrams/{diagram_id}", "PUT", {"xml": xml})

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

def mcp_protocol_mode(port):
    """Run in MCP protocol mode - read JSON-RPC from stdin, write to stdout"""
    base_url = f"http://localhost:{port}"

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

                if tool_name == "list_diagrams":
                    response = handle_list_diagrams(request_id, base_url)
                elif tool_name == "get_diagram_by_id":
                    response = handle_get_diagram_by_id(request_id, base_url, arguments)
                elif tool_name == "update_diagram":
                    response = handle_update_diagram(request_id, base_url, arguments)
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

def cli_mode():
    """Run in CLI mode for manual testing"""
    parser = argparse.ArgumentParser(
        description="diagrams.net IntelliJ MCP Server Wrapper",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Show all open diagrams
  %(prog)s --show-diagrams

  # Get diagram by ID
  %(prog)s --get-diagram abc123

  # Update diagram from XML file
  %(prog)s --update-diagram abc123 diagram.xml

  # Check server status
  %(prog)s --status

  # Use custom port
  %(prog)s --show-diagrams --port 9000
        """
    )

    parser.add_argument("--port", type=int, default=8765,
                        help="Server port (default: 8765)")
    parser.add_argument("--show-diagrams", action="store_true",
                        help="List all open diagrams")
    parser.add_argument("--get-diagram", metavar="ID",
                        help="Get diagram content by ID")
    parser.add_argument("--update-diagram", nargs=2, metavar=("ID", "XML_FILE"),
                        help="Update diagram with XML from file")
    parser.add_argument("--status", action="store_true",
                        help="Show server status")

    args = parser.parse_args()
    base_url = f"http://localhost:{args.port}"

    if args.status:
        result = call_api(base_url, "/api/status")
        print(json.dumps(result, indent=2))

    elif args.show_diagrams:
        result = call_api(base_url, "/api/diagrams")
        if "error" in result:
            print(f"Error: {result.get('message', result.get('error'))}", file=sys.stderr)
            sys.exit(1)

        diagrams = result.get("diagrams", [])
        print(f"Open diagrams: {len(diagrams)}")
        for d in diagrams:
            print(f"  • {d['fileName']}")
            print(f"    ID: {d['id']}")
            print(f"    Path: {d.get('relativePath', 'N/A')}")
            print(f"    Project: {d.get('project', 'N/A')}")
            print()

    elif args.get_diagram:
        result = call_api(base_url, f"/api/diagrams/{args.get_diagram}")
        if "error" in result:
            print(f"Error: {result.get('message', result.get('error'))}", file=sys.stderr)
            sys.exit(1)

        print(f"Diagram: {result.get('fileName')}")
        print(f"ID: {result.get('id')}")
        print(f"Path: {result.get('filePath')}")
        print(f"Type: {result.get('fileType')}")
        print(f"Project: {result.get('project')}")

        # Show decoded diagram structure if available
        decodedXml = result.get('decodedXml')
        if decodedXml:
            print("\nDiagram Structure (decoded):")
            print(decodedXml)

        print("\nFull XML Content:")
        print(result.get('xml', '(no content)'))

    elif args.update_diagram:
        diagram_id, xml_file = args.update_diagram
        try:
            with open(xml_file, 'r') as f:
                xml = f.read()
        except FileNotFoundError:
            print(f"Error: File not found: {xml_file}", file=sys.stderr)
            sys.exit(1)

        result = call_api(base_url, f"/api/diagrams/{diagram_id}", "PUT", {"xml": xml})
        if "error" in result:
            print(f"Error: {result.get('message', result.get('error'))}", file=sys.stderr)
            sys.exit(1)

        print(f"✓ Successfully updated diagram {diagram_id}")
        if result.get("message"):
            print(f"  {result['message']}")

    else:
        parser.print_help()
        sys.exit(1)

def main():
    """Main entry point"""
    # Check if running in CLI mode (any argument starting with -)
    if len(sys.argv) > 1 and sys.argv[1].startswith('-'):
        cli_mode()
    else:
        # MCP protocol mode
        port = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_PORT
        mcp_protocol_mode(port)

if __name__ == "__main__":
    main()
