# Testing Instructions for MCP Server (Phase 1)

## Step 1: Start the Test IDE

The IDE should now be starting with `./gradlew runIde`. Wait for it to fully load.

## Step 2: Enable MCP Server in Settings

1. In the test IntelliJ IDEA window, go to: **Settings** (or **Preferences** on macOS)
2. Navigate to: **Languages & Frameworks → Diagrams.net**
3. You should see:
   - **UI Theme** dropdown (existing setting)
   - **UI Mode** dropdown (existing setting)
   - **☑ Enable MCP Server** checkbox (NEW!)
   - **MCP Server Port** field with "8765" (NEW!)
   - **Server Status** label showing "Server Status: Stopped" (NEW!)

4. Check the **Enable MCP Server** checkbox
5. Click **Apply**
6. The status should change to: **"Server Status: Running on port 8765"**

## Step 3: Test the Endpoints

Open a new terminal and run the test script:

```bash
cd /Users/ascheman/wrk/docToolchain/diagrams.net-intellij-plugin
./test-mcp-server.sh 8765
```

Or test manually with curl:

### Test 1: Health Check
```bash
curl http://localhost:8765/
```
**Expected:** `diagrams.net MCP Server - Running`

### Test 2: Server Status
```bash
curl http://localhost:8765/api/status | jq
```
**Expected:**
```json
{
  "status": "running",
  "port": 8765,
  "version": "0.2.7",
  "openDiagrams": 0
}
```

### Test 3: List Diagrams
```bash
curl http://localhost:8765/api/diagrams | jq
```
**Expected:**
```json
{
  "diagrams": []
}
```
*(Empty because we haven't implemented editor tracking yet)*

### Test 4: MCP Discovery
```bash
curl http://localhost:8765/api/mcp/info | jq
```
**Expected:**
```json
{
  "name": "diagrams-net-intellij-mcp",
  "version": "0.2.7",
  "description": "MCP server for diagrams.net integration in IntelliJ IDEA",
  "port": 8765,
  "tools": [
    {
      "name": "list_diagrams",
      "description": "List all currently open diagram files in IntelliJ IDEA",
      ...
    },
    ...
  ]
}
```

### Test 5: Get Diagram by ID (Should Fail - No Diagrams)
```bash
curl http://localhost:8765/api/diagrams/test123 | jq
```
**Expected:**
```json
{
  "error": "Diagram not found",
  "code": "DIAGRAM_NOT_FOUND",
  "message": "No diagram found with ID: test123"
}
```

### Test 6: Find by Path (Not Implemented Yet)
```bash
curl "http://localhost:8765/api/diagrams/by-path?path=test.drawio.svg" | jq
```
**Expected:**
```json
{
  "error": "Not yet implemented",
  "message": "Path-based lookup will be implemented in Phase 3"
}
```

## Step 4: Test Port Configuration

1. In Settings, change port from **8765** to **8766**
2. Click **Apply**
3. Status should show: **"Server Status: Running on port 8766"**
4. Test the new port:
```bash
curl http://localhost:8766/api/status | jq
```
5. Old port should no longer work:
```bash
curl http://localhost:8765/api/status
# Should fail with: "Connection refused"
```

## Step 5: Test Server Disable

1. In Settings, **uncheck** "Enable MCP Server"
2. Click **Apply**
3. Status should show: **"Server Status: Stopped"**
4. Test that server is stopped:
```bash
curl http://localhost:8766/api/status
# Should fail with: "Connection refused"
```

## Step 6: Test Port Collision Handling

1. Start a dummy server on port 8765:
```bash
# In a separate terminal
python3 -m http.server 8765
```

2. In Settings, enable MCP Server with port **8765**
3. Click **Apply**
4. Status should show: **"Server Status: Running on port 8766"** (automatically moved to next port)
5. Test:
```bash
curl http://localhost:8766/api/status | jq
```

## Success Criteria

✅ All endpoints respond with correct JSON
✅ Server can start/stop via settings
✅ Port configuration works
✅ Port collision handling works (tries 8766, 8767, etc.)
✅ Server status is displayed correctly
✅ CORS headers are present
✅ Server only listens on localhost (127.0.0.1)

## Troubleshooting

### Server Won't Start
- Check IntelliJ Event Log (View → Tool Windows → Event Log)
- Look for error messages from DiagramMcpService
- Try a different port (e.g., 9000)

### Can't Connect to Server
- Verify server status shows "Running"
- Check firewall settings
- Try: `lsof -i :8765` to see if port is in use
- Check IDE logs: Help → Show Log in Finder/Explorer

### Settings Don't Save
- Make sure to click "Apply" or "OK"
- Check file: `~/Library/Application Support/JetBrains/.../options/diagramsNet.xml`

## Next Steps After Testing

Once Phase 1 is verified working:
1. **Phase 2**: Implement editor registration so diagrams appear in the list
2. **Phase 3**: Implement path-based access and auto-open
3. **Phase 4**: Implement XML extraction from SVG/PNG
4. **Phase 5**: Implement multi-instance support with discovery file

## Current Limitations (Expected)

- ❌ No diagrams appear in list (editor tracking not implemented)
- ❌ Can't get diagram content (XML access not implemented)
- ❌ Can't update diagrams (XML update not implemented)
- ❌ Path-based access returns "not implemented"
- ❌ No project information in responses
- ❌ No instance discovery file

These are all expected and will be implemented in subsequent phases!
