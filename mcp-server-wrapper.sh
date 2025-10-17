#!/bin/bash

# MCP Server Wrapper for Claude Desktop
# This script acts as an MCP server that proxies to the IntelliJ plugin's HTTP API

PORT=${1:-8765}
BASE_URL="http://localhost:$PORT"

# Function to call the API and convert to MCP format
call_api() {
    local endpoint=$1
    local method=${2:-GET}
    local data=$3

    if [ "$method" = "GET" ]; then
        curl -s "$BASE_URL$endpoint"
    else
        curl -s -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data"
    fi
}

# MCP protocol handler - read from stdin, write to stdout
while IFS= read -r line; do
    # Parse JSON-RPC request - preserve ID as-is (could be string or number)
    method=$(echo "$line" | jq -r '.method')
    id=$(echo "$line" | jq '.id')  # Don't use -r to preserve type

    case "$method" in
        "initialize")
            # Return server capabilities
            echo "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"diagrams-net-intellij-mcp\",\"version\":\"0.2.7\"}}}"
            ;;
        "tools/list")
            # Return available tools
            TOOLS=$(call_api "/api/mcp/info" | jq '.tools')
            echo "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"tools\":$TOOLS}}"
            ;;
        "tools/call")
            # Execute tool
            tool_name=$(echo "$line" | jq -r '.params.name')
            tool_args=$(echo "$line" | jq -c '.params.arguments')

            case "$tool_name" in
                "list_diagrams")
                    result=$(call_api "/api/diagrams" | jq -c '.diagrams')
                    count=$(echo "$result" | jq length)
                    diagrams_list=$(echo "$result" | jq -r '.[] | "- \(.fileName) (\(.id))"')
                    text="Open diagrams: $count\n$diagrams_list"
                    echo "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"content\":[{\"type\":\"text\",\"text\":$(echo "$text" | jq -Rs '.')}]}}"
                    ;;
                "get_diagram_by_id")
                    diagram_id=$(echo "$tool_args" | jq -r '.id')
                    result=$(call_api "/api/diagrams/$diagram_id" | jq -c '.')
                    if echo "$result" | jq -e '.error' > /dev/null; then
                        error_msg=$(echo "$result" | jq -r '.message')
                        echo "{\"jsonrpc\":\"2.0\",\"id\":$id,\"error\":{\"code\":-32000,\"message\":\"$error_msg\"}}"
                    else
                        echo "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"content\":[{\"type\":\"text\",\"text\":$(echo "$result" | jq -c '.' | jq -Rs '.')}]}}"
                    fi
                    ;;
                *)
                    echo "{\"jsonrpc\":\"2.0\",\"id\":$id,\"error\":{\"code\":-32601,\"message\":\"Unknown tool: $tool_name\"}}"
                    ;;
            esac
            ;;
        *)
            echo "{\"jsonrpc\":\"2.0\",\"id\":$id,\"error\":{\"code\":-32601,\"message\":\"Unknown method: $method\"}}"
            ;;
    esac
done
