#!/bin/bash

# Test script for MCP server endpoints

PORT=${1:-8765}
BASE_URL="http://localhost:$PORT"

echo "Testing MCP Server on port $PORT"
echo "================================"
echo ""

# Test 1: Health check
echo "1. Testing health check (GET /)"
echo "   curl $BASE_URL/"
curl -s "$BASE_URL/" && echo "" || echo "FAILED"
echo ""

# Test 2: Server status
echo "2. Testing server status (GET /api/status)"
echo "   curl $BASE_URL/api/status"
curl -s "$BASE_URL/api/status" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/api/status"
echo ""

# Test 3: List diagrams
echo "3. Testing list diagrams (GET /api/diagrams)"
echo "   curl $BASE_URL/api/diagrams"
curl -s "$BASE_URL/api/diagrams" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/api/diagrams"
echo ""

# Test 4: MCP info
echo "4. Testing MCP info (GET /api/mcp/info)"
echo "   curl $BASE_URL/api/mcp/info"
curl -s "$BASE_URL/api/mcp/info" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/api/mcp/info"
echo ""

# Test 5: Get diagram by ID (should fail - no diagrams open)
echo "5. Testing get diagram by ID (GET /api/diagrams/test123)"
echo "   curl $BASE_URL/api/diagrams/test123"
curl -s "$BASE_URL/api/diagrams/test123" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/api/diagrams/test123"
echo ""

# Test 6: Find by path (not implemented yet)
echo "6. Testing find by path (GET /api/diagrams/by-path?path=test.drawio.svg)"
echo "   curl $BASE_URL/api/diagrams/by-path?path=test.drawio.svg"
curl -s "$BASE_URL/api/diagrams/by-path?path=test.drawio.svg" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/api/diagrams/by-path?path=test.drawio.svg"
echo ""

echo "================================"
echo "Testing complete!"
