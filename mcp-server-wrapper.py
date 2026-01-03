#!/usr/bin/env python3
"""
MCP Server Wrapper for diagrams.net IntelliJ Plugin

Stdio ↔ HTTP proxy for MCP clients that only support stdio transport.
All communication uses the /mcp JSON-RPC endpoint.

Usage:
  # MCP Protocol Mode (for Claude Desktop) - no arguments, reads JSON-RPC from stdin
  ./mcp-server-wrapper.py

  # CLI Mode (for manual testing)
  ./mcp-server-wrapper.py --status [--port PORT]
  ./mcp-server-wrapper.py --list [--port PORT]
  ./mcp-server-wrapper.py --get ID [--port PORT]
  ./mcp-server-wrapper.py --tools [--port PORT]

Environment Variables:
  DIAGRAMS_NET_MCP_PORT  Server port (default: 8765) - used by both modes
"""

import sys
import json
import urllib.request
import urllib.error
import os

DEFAULT_PORT = 8765


def mcp_call(url, method, params=None, request_id=1):
    """Send JSON-RPC request to /mcp endpoint."""
    request = {
        "jsonrpc": "2.0",
        "id": request_id,
        "method": method
    }
    if params:
        request["params"] = params

    try:
        req = urllib.request.Request(
            url,
            data=json.dumps(request).encode('utf-8'),
            headers={"Content-Type": "application/json", "Accept": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.loads(response.read().decode('utf-8'))
    except urllib.error.URLError as e:
        return {"error": {"code": -1, "message": f"Connection failed: {e.reason}"}}
    except Exception as e:
        return {"error": {"code": -1, "message": str(e)}}


def tool_call(url, tool_name, arguments=None):
    """Call an MCP tool via JSON-RPC."""
    return mcp_call(url, "tools/call", {"name": tool_name, "arguments": arguments or {}})


def mcp_proxy_mode(port):
    """Proxy stdin JSON-RPC to /mcp endpoint, write responses to stdout."""
    url = f"http://localhost:{port}/mcp"

    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue

        # Parse to extract request id for error responses
        request_id = None
        try:
            parsed = json.loads(line)
            request_id = parsed.get("id")
        except json.JSONDecodeError:
            pass

        try:
            req = urllib.request.Request(url, line.encode('utf-8'),
                {"Content-Type": "application/json", "Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=30) as resp:
                print(resp.read().decode('utf-8'), flush=True)
        except json.JSONDecodeError as e:
            print(json.dumps({"jsonrpc": "2.0", "id": request_id,
                "error": {"code": -32700, "message": f"Parse error: {e}"}}), flush=True)
        except urllib.error.URLError as e:
            print(json.dumps({"jsonrpc": "2.0", "id": request_id,
                "error": {"code": -32603, "message": f"Connection failed: {e.reason}"}}), flush=True)
        except Exception as e:
            print(json.dumps({"jsonrpc": "2.0", "id": request_id,
                "error": {"code": -32603, "message": f"Internal error: {e}"}}), flush=True)


def cli_mode(args):
    """CLI mode - translate commands to JSON-RPC calls."""
    import argparse
    parser = argparse.ArgumentParser(description="diagrams.net MCP CLI")
    parser.add_argument("--port", type=int,
        default=int(os.environ.get("DIAGRAMS_NET_MCP_PORT", DEFAULT_PORT)),
        help=f"Server port (default: {DEFAULT_PORT})")
    parser.add_argument("--status", action="store_true", help="Check server status")
    parser.add_argument("--tools", action="store_true", help="List available tools")
    parser.add_argument("--list", action="store_true", help="List open diagrams")
    parser.add_argument("--get", metavar="ID", help="Get diagram by ID")

    opts = parser.parse_args(args)
    url = f"http://localhost:{opts.port}/mcp"

    if opts.status:
        resp = mcp_call(url, "ping")
        if "error" in resp:
            print(f"Server not reachable: {resp['error'].get('message', 'unknown')}")
            sys.exit(1)
        print(f"Server OK at {url}")

    elif opts.tools:
        resp = mcp_call(url, "tools/list")
        if "error" in resp:
            print(f"Error: {resp['error'].get('message')}", file=sys.stderr)
            sys.exit(1)
        for tool in resp.get("result", {}).get("tools", []):
            print(f"  {tool['name']}: {tool['description']}")

    elif opts.list:
        resp = tool_call(url, "list_diagrams")
        if "error" in resp:
            print(f"Error: {resp['error'].get('message')}", file=sys.stderr)
            sys.exit(1)
        content = resp.get("result", {}).get("content", [])
        if content:
            print(content[0].get("text", "No diagrams"))

    elif opts.get:
        resp = tool_call(url, "get_diagram_by_id", {"id": opts.get})
        if "error" in resp:
            print(f"Error: {resp['error'].get('message')}", file=sys.stderr)
            sys.exit(1)
        content = resp.get("result", {}).get("content", [])
        if content:
            print(content[0].get("text", "No content"))

    else:
        parser.print_help()


def main():
    # CLI mode if any --flag is present
    if any(arg.startswith('--') for arg in sys.argv[1:]):
        cli_mode(sys.argv[1:])
    else:
        # MCP proxy mode
        port = int(os.environ.get("DIAGRAMS_NET_MCP_PORT", DEFAULT_PORT))
        mcp_proxy_mode(port)


if __name__ == "__main__":
    main()
