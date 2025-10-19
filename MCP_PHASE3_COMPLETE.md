# MCP Integration - Phase 3 Complete

## Summary

Phase 3 (XML Decoding for MCP Clients) has been implemented.
The API now provides human-readable, decoded diagram XML alongside the compressed data, making diagrams accessible to any MCP client without requiring base64+zlib decompression implementation.

## What Was Implemented

### 1. XML Decoding in DiagramMcpHttpServer ✅

Added `decodeDiagramContent()` method in `DiagramMcpHttpServer.kt`:
- Extracts base64 content from `<diagram>` tags
- Base64 decodes the content
- Decompresses using zlib (raw inflate mode, no zlib header)
- URL decodes the decompressed data
- Returns readable mxGraphModel XML structure

**Location:** `DiagramMcpHttpServer.kt:389-422`

### 2. SVG Content Extraction ✅

Added `extractMxfileFromSvg()` method in `DiagramMcpHttpServer.kt`:
- Extracts mxfile XML from SVG's `content` attribute
- Decodes HTML entities (`&lt;`, `&gt;`, `&quot;`, `&amp;`)
- Enables proper decoding of SVG-embedded diagrams

**Location:** `DiagramMcpHttpServer.kt:369-387`

### 3. Enhanced GET Diagram Response ✅

Updated `GET /api/diagrams/{id}` to return both XML formats:

```json
{
  "id": "abc123",
  "filePath": "/absolute/path/diagram.drawio.svg",
  "fileName": "diagram.drawio.svg",
  "fileType": "svg",
  "project": "my-project",
  "xml": "<mxfile>...<diagram>base64+zlib...</diagram></mxfile>",
  "decodedXml": "<mxGraphModel dx=\"643\" dy=\"706\" grid=\"1\"...><root><mxCell id=\"0\"/>...</root></mxGraphModel>"
}
```

**Fields:**
- `xml`: Full mxfile with compressed data (for updates)
- `decodedXml`: Human-readable mxGraphModel (for reading/understanding)

**Location:** `DiagramMcpHttpServer.kt:136-190`

### 4. MCP Wrapper Updates ✅

Updated `mcp-server-wrapper.py` to display decoded XML:

**MCP Protocol Mode:**
- `get_diagram_by_id` tool now shows both decoded structure and full XML
- Formatted output separates "Diagram Structure (decoded)" from "Full XML (for updates)"

**CLI Mode:**
- `--get-diagram` command shows decoded XML first
- Makes manual testing and debugging easier

**Location:** `mcp-server-wrapper.py:162-215, 372-391`

### 5. Documentation Updates ✅

Updated all MCP documentation:
- **MCP_QUICK_START.md**: Added Claude Code setup instructions
- **MCP_API_REFERENCE.md**: Documented `decodedXml` field
- **MCP_WRAPPER_README.md**: Updated examples with decoded XML output
- **.gitignore**: Added `.mcp.json` for user-local MCP configuration

## Benefits

### For MCP Clients
- **No decompression needed**: Clients can read diagram structure directly
- **Universal compatibility**: Works with any MCP client (Claude Desktop, Claude Code, custom clients)
- **Clear structure**: Shows cells, geometry, styles, and connections in readable XML
- **Update support**: Full compressed XML still provided for diagram updates

### For Developers
- **Easy debugging**: CLI mode shows decoded structure immediately
- **Better understanding**: Clear visibility into diagram content
- **Testing**: Manual verification of diagram structure without custom tools

## Testing Results

### Verified Scenarios ✅

1. **HTTP API Direct**
   ```bash
   curl http://localhost:8765/api/diagrams/{id} | jq
   ```
   - Returns both `xml` and `decodedXml` fields
   - Decoded XML is properly formatted

2. **CLI Wrapper**
   ```bash
   ./mcp-server-wrapper.py --get-diagram {id}
   ```
   - Shows "Diagram Structure (decoded)" section
   - Shows "Full XML (for updates)" section

3. **MCP Protocol Mode**
   ```bash
   echo '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_diagram_by_id","arguments":{"id":"..."}}}' | ./mcp-server-wrapper.py
   ```
   - Returns properly formatted JSON-RPC response
   - Includes decoded XML in result content

4. **All Diagram Formats**
   - ✅ SVG files with embedded mxfile
   - ✅ PNG files with embedded mxfile
   - ✅ Raw XML (.drawio, .dio) files

## Implementation Quality

### Error Handling
- Graceful fallback if decoding fails
- Warning logged to IntelliJ console
- Returns null for `decodedXml` if decoding impossible
- Original `xml` field always provided

### Performance
- Minimal overhead (single-pass decoding)
- Only decoded on-demand (GET requests)
- No caching (always fresh from disk)

### Code Quality
- Clean separation of concerns
- Well-documented functions
- Consistent with existing code style
- No breaking changes to existing API

## Configuration Updates

### IntelliJ Plugin Settings
MCP server must be enabled in IntelliJ settings:
1. Settings → Tools → Diagrams.net Integration
2. Check "Enable MCP Server"
3. Configure port (default: 8765)
4. Server starts when enabled and a diagram file is open

### Claude Desktop
No changes needed - existing configuration works with new feature automatically.

### Claude Code
New project-level configuration added:
```json
// .mcp.json (in project root, added to .gitignore)
{
  "mcpServers": {
    "diagrams-net-intellij": {
      "command": "/path/to/mcp-server-wrapper.py",
      "args": []
    }
  }
}
```

Global setting:
```json
// ~/.claude/settings.json
{
  "enableAllProjectMcpServers": true
}
```

## Future Enhancements

Potential Phase 4 features:
- Diagram validation (structure, references, styles)
- Semantic understanding (extract component names, relationships)
- Natural language diagram descriptions
- Multi-page diagram support
- Diff/preview before applying changes
- Template-based diagram creation

## Breaking Changes

**None** - This is a purely additive change:
- New `decodedXml` field added to response
- Existing `xml` field unchanged
- All existing clients continue to work
- New clients can use decoded XML for easier parsing

## Documentation

All documentation updated:
- ✅ MCP_QUICK_START.md - Setup for Claude Desktop and Claude Code
- ✅ MCP_API_REFERENCE.md - API documentation with decodedXml field
- ✅ MCP_WRAPPER_README.md - CLI examples with decoded output
- ✅ .gitignore - User-local .mcp.json exclusion
- ✅ MCP_PHASE3_COMPLETE.md - This document

## Commits

Related commits:
- XML decoding implementation
- MCP wrapper updates
- Documentation updates
- .gitignore updates

## Status: ✅ COMPLETE

Phase 3 is complete and ready for use with any MCP client.
All tests pass, documentation is updated, and the feature is fully functional.
