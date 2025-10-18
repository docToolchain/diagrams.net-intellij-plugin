#!/bin/bash

# Comprehensive test script for MCP Phase 1 + Phase 2

PORT=${1:-8765}
BASE_URL="http://localhost:$PORT"

echo "========================================"
echo "MCP Server Testing - Phase 1 + Phase 2"
echo "========================================"
echo "Port: $PORT"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

test_passed=0
test_failed=0

# Function to run test
run_test() {
    local test_name="$1"
    local expected="$2"
    shift 2

    echo -n "Testing: $test_name... "

    output=$("$@" 2>&1)
    exit_code=$?

    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}"
        ((test_passed++))
        if [ -n "$expected" ]; then
            echo "Response: $output" | head -3
        fi
    else
        echo -e "${RED}✗ FAIL${NC}"
        ((test_failed++))
        echo "Error: $output"
    fi
    echo ""
}

# Phase 1 Tests
echo "=== Phase 1: Core Infrastructure ==="
echo ""

run_test "Health Check" "" curl -s -f "$BASE_URL/"

run_test "Server Status" "running" curl -s -f "$BASE_URL/api/status"

run_test "MCP Info" "diagrams.net MCP Server" curl -s -f "$BASE_URL/api/mcp/info"

# Phase 2 Tests
echo "=== Phase 2: Editor Integration ==="
echo ""

echo "Test: List Open Diagrams"
echo "   curl $BASE_URL/api/diagrams"
DIAGRAMS_RESPONSE=$(curl -s "$BASE_URL/api/diagrams")
echo "$DIAGRAMS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$DIAGRAMS_RESPONSE"
echo ""

# Parse diagram count
DIAGRAM_COUNT=$(echo "$DIAGRAMS_RESPONSE" | python3 -c "import sys, json; print(len(json.load(sys.stdin).get('diagrams', [])))" 2>/dev/null || echo "0")

if [ "$DIAGRAM_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}⚠ No diagrams open${NC}"
    echo "To test Phase 2 functionality:"
    echo "  1. Open a diagram in the test IDE"
    echo "  2. Run this script again"
    echo ""
else
    echo -e "${GREEN}✓ Found $DIAGRAM_COUNT open diagram(s)${NC}"
    echo ""

    # Get first diagram ID
    DIAGRAM_ID=$(echo "$DIAGRAMS_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['diagrams'][0]['id'])" 2>/dev/null)

    if [ -n "$DIAGRAM_ID" ]; then
        echo "Testing with diagram ID: $DIAGRAM_ID"
        echo ""

        # Test GET diagram content
        echo "Test: Get Diagram Content"
        echo "   curl $BASE_URL/api/diagrams/$DIAGRAM_ID"
        DIAGRAM_CONTENT=$(curl -s "$BASE_URL/api/diagrams/$DIAGRAM_ID")

        if echo "$DIAGRAM_CONTENT" | grep -q "xml"; then
            echo -e "${GREEN}✓ Successfully retrieved diagram with XML content${NC}"
            echo "$DIAGRAM_CONTENT" | python3 -m json.tool 2>/dev/null | head -20
            echo "..."
        else
            echo -e "${RED}✗ Failed to retrieve diagram content${NC}"
            echo "$DIAGRAM_CONTENT"
        fi
        echo ""

        # Test UPDATE diagram
        echo "Test: Update Diagram Content"
        echo "   Creating a simple test diagram..."

        TEST_XML='<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net">
  <diagram name="Page-1">
    <mxGraphModel dx="800" dy="800">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
        <mxCell id="2" value="API Test - Success!" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;" vertex="1" parent="1">
          <mxGeometry x="200" y="150" width="200" height="80" as="geometry"/>
        </mxCell>
        <mxCell id="3" value="Phase 2 Working!" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="1">
          <mxGeometry x="200" y="280" width="200" height="80" as="geometry"/>
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>'

        UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/diagrams/$DIAGRAM_ID" \
            -H "Content-Type: application/json" \
            -d "{\"xml\": $(echo "$TEST_XML" | python3 -c "import sys, json; print(json.dumps(sys.stdin.read()))"), \"autoSave\": true}")

        if echo "$UPDATE_RESPONSE" | grep -q "success"; then
            echo -e "${GREEN}✓ Successfully updated diagram${NC}"
            echo "$UPDATE_RESPONSE" | python3 -m json.tool 2>/dev/null
            echo ""
            echo -e "${YELLOW}Check the IDE - the diagram should now show:${NC}"
            echo "  - Green box: 'API Test - Success!'"
            echo "  - Blue box: 'Phase 2 Working!'"
        else
            echo -e "${RED}✗ Failed to update diagram${NC}"
            echo "$UPDATE_RESPONSE"
        fi
        echo ""
    fi
fi

# Error handling tests
echo "=== Error Handling Tests ==="
echo ""

echo "Test: Get Non-Existent Diagram"
ERROR_RESPONSE=$(curl -s "$BASE_URL/api/diagrams/nonexistent123")
if echo "$ERROR_RESPONSE" | grep -q "NOT_FOUND"; then
    echo -e "${GREEN}✓ Correctly returns 404 for missing diagram${NC}"
else
    echo -e "${RED}✗ Unexpected error response${NC}"
fi
echo "$ERROR_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$ERROR_RESPONSE"
echo ""

echo "Test: Update Non-Existent Diagram"
UPDATE_ERROR=$(curl -s -X PUT "$BASE_URL/api/diagrams/nonexistent123" \
    -H "Content-Type: application/json" \
    -d '{"xml": "test"}')
if echo "$UPDATE_ERROR" | grep -q "NOT_FOUND"; then
    echo -e "${GREEN}✓ Correctly returns 404 for missing diagram${NC}"
else
    echo -e "${RED}✗ Unexpected error response${NC}"
fi
echo "$UPDATE_ERROR" | python3 -m json.tool 2>/dev/null || echo "$UPDATE_ERROR"
echo ""

# Summary
echo "========================================"
echo "Test Summary"
echo "========================================"
echo -e "Passed: ${GREEN}$test_passed${NC}"
echo -e "Failed: ${RED}$test_failed${NC}"
echo ""

if [ "$DIAGRAM_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}Note: Phase 2 tests skipped - no diagrams open${NC}"
    echo "To test Phase 2:"
    echo "  1. In the test IDE, create a new diagram:"
    echo "     Right-click → New → DiagramsNet → Diagram (*.drawio.svg)"
    echo "  2. Run this script again: ./test-phase2.sh"
    echo ""
fi

echo "========================================"
