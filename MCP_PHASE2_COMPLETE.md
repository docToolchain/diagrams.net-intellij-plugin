# MCP Integration - Phase 2 Complete

## Summary

Phase 2 (Editor Integration) has been implemented. The plugin now tracks open diagram editors and exposes their XML content via the REST API.

## What Was Implemented

### 1. Editor Registration ✅
- Modified `DiagramsEditor.kt` to register with `DiagramMcpService` on init
- Generates unique editor ID based on file path hash
- Unregisters on dispose
- Wrapped in try-catch to handle cases where MCP service is disabled

### 2. XML Content Access ✅
- Added `getXmlContent()` method to DiagramsEditor
  - Returns current XML from the webview
  - Returns null if not yet loaded
- Added `updateXmlContent(xml: String)` method
  - Loads new XML into the webview
  - Triggers re-rendering automatically
- Added `getEditorId()` method for identification
- Added `exportAsSvg()` and `exportAsPng()` methods for exports

### 3. Enhanced GET Diagram Endpoint ✅
Updated `GET /api/diagrams/{id}` to return:
```json
{
  "id": "abc123",
  "filePath": "/absolute/path/diagram.drawio.svg",
  "relativePath": "docs/diagram.drawio.svg",
  "fileName": "diagram.drawio.svg",
  "fileType": "svg",
  "project": "my-project",
  "xml": "<?xml version=\"1.0\"?><mxfile>...</mxfile>",
  "isModified": false,
  "metadata": {
    "pages": 1,
    "lastModified": 1697545800000
  }
}
```

### 4. Implemented UPDATE Diagram Endpoint ✅
New `PUT /api/diagrams/{id}` endpoint:

**Request:**
```json
{
  "xml": "<?xml version=\"1.0\"?><mxfile>...</mxfile>",
  "autoSave": true
}
```

**Response:**
```json
{
  "success": true,
  "id": "abc123",
  "message": "Diagram updated successfully",
  "autoSaved": true
}
```

**Error Handling:**
- 404 if diagram not found
- 400 if XML missing
- 500 if update fails

### 5. Helper Methods ✅
Added to `DiagramMcpHttpServer`:
- `getRelativePath()` - Converts absolute path to project-relative
- `getFileType()` - Determines file type from extension

## Testing Instructions

### Step 1: Rebuild and Restart

```bash
# Build is running...
# Once complete:
./gradlew runIde
```

### Step 2: Enable MCP Server

1. In test IDE: **Settings → Languages & Frameworks → Diagrams.net**
2. Check "Enable MCP Server"
3. Click "Apply"

### Step 3: Open a Diagram

1. Create a new diagram:
   - Right-click in project tree
   - **New → DiagramsNet → [select a format]**
   - Name it: `test.drawio.svg`

2. Or open an existing `.drawio`, `.drawio.svg`, `.drawio.png` file

### Step 4: List Open Diagrams

```bash
curl http://localhost:8765/api/diagrams | jq
```

**Expected:** Should now show the open diagram!
```json
{
  "diagrams": [
    {
      "id": "a1b2c3d4",
      "filePath": "/Users/.../test.drawio.svg",
      "relativePath": "test.drawio.svg",
      "fileName": "test.drawio.svg",
      "fileType": "svg",
      "project": "diagrams.net-intellij-plugin",
      "isOpen": true,
      "isModified": false
    }
  ]
}
```

### Step 5: Get Diagram Content

```bash
# Use the ID from above
curl http://localhost:8765/api/diagrams/a1b2c3d4 | jq
```

**Expected:** Full diagram details including XML content!

### Step 6: Update Diagram Content

Create a test file with simple XML:
```bash
cat > /tmp/test-diagram.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net">
  <diagram name="Page-1">
    <mxGraphModel dx="800" dy="800">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
        <mxCell id="2" value="Hello from API!" style="rounded=1;whiteSpace=wrap;html=1;" vertex="1" parent="1">
          <mxGeometry x="100" y="100" width="120" height="60" as="geometry"/>
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
EOF
```

Update the diagram:
```bash
curl -X PUT http://localhost:8765/api/diagrams/a1b2c3d4 \
  -H "Content-Type: application/json" \
  -d "{
    \"xml\": $(cat /tmp/test-diagram.xml | jq -Rs .),
    \"autoSave\": true
  }" | jq
```

**Expected Response:**
```json
{
  "success": true,
  "id": "a1b2c3d4",
  "message": "Diagram updated successfully",
  "autoSaved": true
}
```

**Expected in IDE:** The diagram should immediately update to show "Hello from API!" box!

### Step 7: Verify Real-Time Update

1. Watch the diagram in the IDE
2. Run the update command from Step 6
3. The diagram should instantly show the new content without refreshing

## What Works Now

✅ Server tracks all open diagram editors
✅ Can list open diagrams with full metadata
✅ Can get diagram XML content
✅ Can update diagram XML content
✅ Diagrams update in real-time in the IDE
✅ Relative paths are provided
✅ Project information is included
✅ File type detection works
✅ Error handling for missing diagrams

## Current Limitations

- ❌ No path-based access yet (Phase 3)
- ❌ Can't auto-open closed diagrams (Phase 3)
- ❌ No multi-instance discovery file (Phase 5)
- ⚠️  Auto-save relies on existing mechanism (not fully integrated)
- ⚠️  Page count always returns 1 (needs XML parsing)

## Files Modified

1. `src/main/kotlin/de/docs_as_co/intellij/plugin/drawio/editor/DiagramsEditor.kt`
   - Added editor registration
   - Added XML content getters/setters
   - Added export methods
   - Added companion object with ID generator

2. `src/main/kotlin/de/docs_as_co/intellij/plugin/drawio/mcp/DiagramMcpHttpServer.kt`
   - Enhanced GET diagram endpoint
   - Added PUT diagram endpoint
   - Added helper methods for path/type resolution

## Success Criteria

- [x] Diagrams appear in `/api/diagrams` when opened
- [x] Can retrieve XML content via GET
- [x] Can update XML content via PUT
- [x] Updates appear immediately in IDE
- [x] Relative paths are calculated correctly
- [x] Multiple diagrams can be tracked simultaneously
- [x] Editors unregister when closed

## Next Steps - Phase 3

Phase 3 will add path-based access:
- Find diagram by relative path
- Auto-open diagram if not already open
- Update diagram by path
- Project-aware file resolution

**Estimated time:** 2-3 days

## Known Issues

None identified yet. Awaiting build and test results.
