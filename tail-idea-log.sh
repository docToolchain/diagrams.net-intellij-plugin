#!/bin/bash
# Tail the IntelliJ sandbox IDE log file
# Useful for watching MCP server logs in real-time

LOG_FILE="build/idea-sandbox/system/log/idea.log"

if [ ! -f "$LOG_FILE" ]; then
    echo "Log file not found: $LOG_FILE"
    echo "Make sure the IDE has been started at least once."
    exit 1
fi

echo "Tailing IntelliJ IDEA log: $LOG_FILE"
echo "Press Ctrl+C to stop"
echo "========================================"
tail -f "$LOG_FILE" | grep --line-buffered -i "mcp\|diagram"
