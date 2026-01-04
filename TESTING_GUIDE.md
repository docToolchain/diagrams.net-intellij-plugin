# MCP Integration Testing Guide - Phase 1 + 2

## Prerequisites

- Plugin is built (BUILD SUCCESSFUL)
- Test IDE should be running or ready to run

## Step-by-Step Testing

### Step 1: Start/Restart the Test IDE

If the IDE from earlier is still running, you can continue using it. Otherwise:

```bash
cd /Users/ascheman/wrk/docToolchain/diagrams.net-intellij-plugin
source ~/.sdkman/bin/sdkman-init.sh && sdk env
./gradlew runIde
```

Wait for the IDE to fully load (shows welcome screen or project).

### Step 2: Enable MCP Server

1. In the test IDE, open **Settings** (⌘, on macOS or Ctrl+Alt+S on other platforms)
2. Navigate to: **Languages & Frameworks → Diagrams.net**
3. You should see the MCP settings:
   - ☑ **Enable MCP Server** checkbox
   - **MCP Server Port**: 8765
   - **Server Status**: label
4. **Check** the "Enable MCP Server" checkbox
5. Click **Apply**
6. Verify the status changes to: **"Server Status: Running on port 8765"**

### Step 3: Test Phase 1 (Core Infrastructure)

Open a terminal and run:

```bash
./test-phase2.sh 8765
```

You should see:
- ✓ Health Check passes
- ✓ Server Status passes
- ✓ MCP Info passes
- ⚠ No diagrams open (expected at this point)

**Expected Output:**
```
======================================
MCP Server Testing - Phase 1 + Phase 2
======================================
Port: 8765

=== Phase 1: Core Infrastructure ===

Testing: Health Check... ✓ PASS

Testing: Server Status... ✓ PASS

Testing: MCP Info... ✓ PASS

=== Phase 2: Editor Integration ===

Test: List Open Diagrams
   curl http://localhost:8765/api/diagrams
{
  "diagrams": []
}

⚠ No diagrams open
```

### Step 4: Create/Open a Diagram

In the test IDE:

**Option A: Create a New Diagram**
1. Right-click in the project tree (or use File → New)
2. Select **New → DiagramsNet**
3. Choose any format (e.g., "Diagram (*.drawio.svg)")
4. Name it: `test-diagram.drawio.svg`
5. The diagram editor should open with a blank canvas

**Option B: Open Existing Diagram**
1. If you have existing `.drawio`, `.drawio.svg`, or `.drawio.png` files, open one
2. The diagrams.net editor should appear

### Step 5: Verify Diagram is Tracked

Run the test script again:

```bash
./test-phase2.sh 8765
```

**Expected Output:**
```
=== Phase 2: Editor Integration ===

Test: List Open Diagrams
   curl http://localhost:8765/api/diagrams
{
  "diagrams": [
    {
      "id": "a1b2c3d4",
      "filePath": "/path/to/test-diagram.drawio.svg",
      "relativePath": "test-diagram.drawio.svg",
      "fileName": "test-diagram.drawio.svg",
      "fileType": "svg",
      "project": "diagrams.net-intellij-plugin",
      "isOpen": true,
      "isModified": false
    }
  ]
}

✓ Found 1 open diagram(s)

Testing with diagram ID: a1b2c3d4

Test: Get Diagram Content
   curl http://localhost:8765/api/diagrams/a1b2c3d4
✓ Successfully retrieved diagram with XML content
{
  "id": "a1b2c3d4",
  "filePath": "/path/to/test-diagram.drawio.svg",
  "relativePath": "test-diagram.drawio.svg",
  "fileName": "test-diagram.drawio.svg",
  "fileType": "svg",
  "project": "diagrams.net-intellij-plugin",
  "xml": "<?xml version=\"1.0\"?><mxfile>...</mxfile>",
  ...
}
```

### Step 6: Test Real-Time Diagram Update

The test script will automatically update your diagram. Watch the IDE window!

**Expected:**
1. The script updates the diagram with new content
2. ✓ Successfully updated diagram message
3. **IN THE IDE**: The diagram should instantly show:
   - A green box with text "API Test - Success!"
   - A blue box with text "Phase 2 Working!"

**If you see the boxes appear in the IDE, Phase 2 is working perfectly!** 🎉

### Step 7: Manual Update Test

Let's manually update the diagram to add another shape:

```bash
# Get the diagram ID from the previous test
DIAGRAM_ID="a1b2c3d4"  # Replace with your actual ID

# Create a new diagram with 3 boxes
curl -X PUT "http://localhost:8765/api/diagrams/$DIAGRAM_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "xml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<mxfile host=\"app.diagrams.net\">\n  <diagram name=\"Page-1\">\n    <mxGraphModel dx=\"800\" dy=\"800\">\n      <root>\n        <mxCell id=\"0\"/>\n        <mxCell id=\"1\" parent=\"0\"/>\n        <mxCell id=\"2\" value=\"Box 1\" style=\"rounded=1;whiteSpace=wrap;html=1;fillColor=#d5e8d4;\" vertex=\"1\" parent=\"1\">\n          <mxGeometry x=\"100\" y=\"100\" width=\"120\" height=\"60\" as=\"geometry\"/>\n        </mxCell>\n        <mxCell id=\"3\" value=\"Box 2\" style=\"rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;\" vertex=\"1\" parent=\"1\">\n          <mxGeometry x=\"300\" y=\"100\" width=\"120\" height=\"60\" as=\"geometry\"/>\n        </mxCell>\n        <mxCell id=\"4\" value=\"Box 3\" style=\"rounded=1;whiteSpace=wrap;html=1;fillColor=#fff2cc;\" vertex=\"1\" parent=\"1\">\n          <mxGeometry x=\"200\" y=\"200\" width=\"120\" height=\"60\" as=\"geometry\"/>\n        </mxCell>\n      </root>\n    </mxGraphModel>\n  </diagram>\n</mxfile>",
    "autoSave": true
  }' | jq
```

**Watch the IDE** - the diagram should update to show 3 boxes!

### Step 8: Test Multiple Diagrams

1. Open another diagram in the IDE (create a new one or open existing)
2. Run: `curl http://localhost:8765/api/diagrams | jq`
3. You should see **both** diagrams listed

### Step 9: Test Diagram Close

1. Close one of the diagram tabs in the IDE
2. Run: `curl http://localhost:8765/api/diagrams | jq`
3. The closed diagram should no longer appear in the list

### Step 10: Test Port Configuration

1. In IDE Settings, change port from 8765 to 8766
2. Click Apply
3. Status should show: "Server Status: Running on port 8766"
4. Test: `curl http://localhost:8766/api/status | jq`
5. Old port should fail: `curl http://localhost:8765/api/status` (Connection refused)

### Step 11: Test Server Disable

1. In IDE Settings, uncheck "Enable MCP Server"
2. Click Apply
3. Status should show: "Server Status: Stopped"
4. Test: `curl http://localhost:8766/api/status` (Should fail with connection refused)

## Success Criteria Checklist

### Phase 1 ✅
- [ ] Server starts when enabled in settings
- [ ] Server stops when disabled in settings
- [ ] Port configuration works
- [ ] Health check responds
- [ ] Status endpoint returns correct info
- [ ] MCP info endpoint returns tool list

### Phase 2 ✅
- [ ] Open diagrams appear in list
- [ ] Can retrieve diagram XML content
- [ ] Can update diagram content
- [ ] Updates appear immediately in IDE
- [ ] Multiple diagrams can be tracked
- [ ] Closed diagrams are removed from list
- [ ] Relative paths are calculated correctly
- [ ] Project name is included
- [ ] Error handling works (404 for missing diagrams)

## Troubleshooting

### Server Won't Start
- Check IDE Event Log: **View → Tool Windows → Event Log**
- Look for errors mentioning DiagramMcpService
- Try a different port (e.g., 9000)

### Diagrams Don't Appear in List
- Verify the diagram is actually open (tab visible in IDE)
- Check that it's a supported format (.drawio, .drawio.svg, .drawio.png, .dio, etc.)
- Restart the IDE and try again

### Updates Don't Appear in IDE
- Verify the XML is valid diagrams.net format
- Check IDE logs for errors
- Try a simpler diagram structure

### Connection Refused
- Verify server is actually running (check settings status)
- Verify you're using the correct port
- Check firewall settings

## Next Steps

Once testing is complete, you can:

1. **Continue to Phase 3** - Path-based access and auto-open
2. **Test with real diagrams** - Use existing project diagrams
3. **Test with Claude Desktop** - Configure Claude to use the MCP server
4. **Provide feedback** - Report any issues or suggestions

## Quick Reference

**Test Script:**
```bash
./test-phase2.sh [port]
```

**Manual Tests:**
```bash
# Status
curl http://localhost:8765/api/status | jq

# List diagrams
curl http://localhost:8765/api/diagrams | jq

# Get diagram
curl http://localhost:8765/api/diagrams/{id} | jq

# Update diagram
curl -X PUT http://localhost:8765/api/diagrams/{id} \
  -H "Content-Type: application/json" \
  -d '{"xml": "...", "autoSave": true}' | jq
```
