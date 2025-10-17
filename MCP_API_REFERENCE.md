# MCP API Quick Reference

## Base URL
```
http://localhost:{port}/api
```

## Authentication
None (localhost only)

## Common Headers
```
Content-Type: application/json
Accept: application/json
```

---

## Endpoints

### 1. Server Status
```http
GET /api/status
```

**Response:**
```json
{
  "status": "running",
  "port": 8765,
  "version": "0.2.7",
  "projectRoot": "/path/to/project",
  "projectName": "my-project",
  "openDiagrams": 3
}
```

---

### 2. List Diagrams
```http
GET /api/diagrams?includeAll=false
```

**Query Parameters:**
- `includeAll` (boolean): Include all diagrams in project, not just open ones

**Response:**
```json
{
  "diagrams": [
    {
      "id": "abc123",
      "filePath": "/absolute/path/diagram.drawio.svg",
      "relativePath": "docs/diagram.drawio.svg",
      "fileName": "diagram.drawio.svg",
      "fileType": "svg",
      "project": "my-project",
      "isOpen": true,
      "isModified": false
    }
  ]
}
```

---

### 3. Find Diagram by Path
```http
GET /api/diagrams/by-path?path=docs/diagram.drawio.svg&open=true
```

**Query Parameters:**
- `path` (required): Relative path from project root
- `open` (boolean): Auto-open if not already open (default: true)

**Response:**
```json
{
  "id": "abc123",
  "filePath": "/absolute/path/diagram.drawio.svg",
  "relativePath": "docs/diagram.drawio.svg",
  "fileName": "diagram.drawio.svg",
  "fileType": "svg",
  "project": "my-project",
  "isOpen": true,
  "isModified": false,
  "openedByRequest": true
}
```

---

### 4. Get Diagram Content by ID
```http
GET /api/diagrams/{id}
```

**Response:**
```json
{
  "id": "abc123",
  "filePath": "/absolute/path/diagram.drawio.svg",
  "relativePath": "docs/diagram.drawio.svg",
  "fileName": "diagram.drawio.svg",
  "fileType": "svg",
  "xml": "<?xml version=\"1.0\"?><mxfile>...</mxfile>",
  "isModified": false,
  "metadata": {
    "pages": 1,
    "lastModified": "2025-10-17T10:30:00Z"
  }
}
```

---

### 5. Get Diagram Content by Path
```http
POST /api/diagrams/get-by-path
Content-Type: application/json

{
  "path": "docs/diagram.drawio.svg",
  "open": true
}
```

**Response:** Same as endpoint #4

---

### 6. Update Diagram by ID
```http
PUT /api/diagrams/{id}
Content-Type: application/json

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
  "message": "Diagram updated successfully"
}
```

---

### 7. Update Diagram by Path
```http
POST /api/diagrams/update-by-path
Content-Type: application/json

{
  "path": "docs/diagram.drawio.svg",
  "xml": "<?xml version=\"1.0\"?><mxfile>...</mxfile>",
  "autoSave": true,
  "openIfClosed": true
}
```

**Response:**
```json
{
  "success": true,
  "id": "abc123",
  "relativePath": "docs/diagram.drawio.svg",
  "message": "Diagram updated successfully",
  "wasOpened": false
}
```

---

### 8. Export as SVG
```http
GET /api/diagrams/{id}/export/svg
```

**Response:**
```
Content-Type: image/svg+xml

<svg xmlns="http://www.w3.org/2000/svg">...</svg>
```

---

### 9. Export as PNG
```http
GET /api/diagrams/{id}/export/png
```

**Response:**
```
Content-Type: image/png

[Binary PNG data]
```

---

### 10. MCP Discovery
```http
GET /api/mcp/info
```

**Response:**
```json
{
  "name": "diagrams-net-intellij-mcp",
  "version": "0.2.7",
  "description": "MCP server for diagrams.net integration",
  "projectName": "my-project",
  "projectRoot": "/path/to/project",
  "port": 8765,
  "tools": [
    {
      "name": "list_diagrams",
      "description": "List all diagrams",
      "inputSchema": { ... }
    },
    ...
  ]
}
```

---

### 11. Discover All Instances
```http
GET /api/discover/all-instances
```

**Response:**
```json
{
  "currentInstance": {
    "port": 8765,
    "projectName": "my-project",
    "projectRoot": "/path/to/project"
  },
  "allInstances": [
    {
      "port": 8765,
      "projectName": "my-project",
      "projectRoot": "/path/to/project",
      "reachable": true
    },
    {
      "port": 8766,
      "projectName": "other-project",
      "projectRoot": "/path/to/other",
      "reachable": true
    }
  ]
}
```

---

## MCP Tools

### list_diagrams
List all currently open (or all) diagram files

**Input:**
```json
{
  "includeAll": false
}
```

**Maps to:** `GET /api/diagrams?includeAll=false`

---

### find_diagram_by_path
Find a diagram by relative path

**Input:**
```json
{
  "path": "docs/diagram.drawio.svg",
  "open": true
}
```

**Maps to:** `GET /api/diagrams/by-path?path=...&open=true`

---

### get_diagram_by_id
Get XML content by diagram ID

**Input:**
```json
{
  "id": "abc123"
}
```

**Maps to:** `GET /api/diagrams/abc123`

---

### get_diagram_by_path
Get XML content by relative path

**Input:**
```json
{
  "path": "docs/diagram.drawio.svg",
  "open": true
}
```

**Maps to:** `POST /api/diagrams/get-by-path`

---

### update_diagram_by_id
Update diagram content by ID

**Input:**
```json
{
  "id": "abc123",
  "xml": "<?xml version=\"1.0\"?>...",
  "autoSave": true
}
```

**Maps to:** `PUT /api/diagrams/abc123`

---

### update_diagram_by_path
Update diagram content by relative path

**Input:**
```json
{
  "path": "docs/diagram.drawio.svg",
  "xml": "<?xml version=\"1.0\"?>...",
  "autoSave": true,
  "openIfClosed": true
}
```

**Maps to:** `POST /api/diagrams/update-by-path`

---

### export_diagram_svg
Export diagram as SVG

**Input:**
```json
{
  "id": "abc123"
}
```

**Maps to:** `GET /api/diagrams/abc123/export/svg`

---

## Error Responses

### 404 Not Found
```json
{
  "error": "Diagram not found",
  "code": "DIAGRAM_NOT_FOUND",
  "message": "No diagram found at path: docs/missing.drawio.svg"
}
```

### 400 Bad Request
```json
{
  "error": "Invalid XML",
  "code": "INVALID_XML",
  "message": "The provided XML is not valid diagrams.net format"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal error",
  "code": "INTERNAL_ERROR",
  "message": "Failed to save diagram: [details]"
}
```

---

## File Formats

### Supported Extensions
- `.drawio` - Draw.io XML
- `.drawio.svg` - SVG with embedded XML
- `.drawio.png` - PNG with embedded XML
- `.drawio.xml` - XML diagram
- `.dio`, `.dio.svg`, `.dio.png`, `.dio.xml` - Alternative naming

### Auto-Detection
Files without explicit extensions are detected by:
- SVG: `<svg>` element with `content` attribute starting with `<mxfile`
- PNG: Metadata chunk with `mxfile` keyword
- XML: Root element `<mxfile><diagram/></mxfile>`

---

## Path Resolution

### Relative Paths
All paths are relative to project root:
- ✅ `docs/architecture/system.drawio.svg`
- ✅ `diagrams/flowchart.drawio.png`
- ✅ `README.drawio.svg`

### Not Supported
- ❌ Absolute paths: `/Users/john/projects/...`
- ❌ Parent directory: `../other-project/...`
- ❌ Home directory: `~/projects/...`

### Path Normalization
- Forward slashes `/` on all platforms
- No leading slash
- Case-sensitive on Linux/macOS, case-insensitive on Windows

---

## Testing with curl

### List diagrams
```bash
curl http://localhost:8765/api/diagrams
```

### Get diagram by path
```bash
curl "http://localhost:8765/api/diagrams/by-path?path=docs/test.drawio.svg"
```

### Get diagram content
```bash
curl http://localhost:8765/api/diagrams/abc123
```

### Update diagram
```bash
curl -X POST http://localhost:8765/api/diagrams/update-by-path \
  -H "Content-Type: application/json" \
  -d '{
    "path": "docs/test.drawio.svg",
    "xml": "<?xml version=\"1.0\"?><mxfile>...</mxfile>",
    "autoSave": true
  }'
```

### Export as SVG
```bash
curl http://localhost:8765/api/diagrams/abc123/export/svg -o diagram.svg
```

---

## Instance Discovery

### Discovery File Location
- **macOS/Linux**: `~/.diagrams-net-intellij-mcp/instances.json`
- **Windows**: `%USERPROFILE%\.diagrams-net-intellij-mcp\instances.json`

### Discovery File Format
```json
{
  "instances": [
    {
      "port": 8765,
      "projectName": "my-project",
      "projectRoot": "/Users/john/projects/my-project",
      "pid": 12345,
      "lastSeen": "2025-10-17T10:30:00Z"
    }
  ]
}
```

### Stale Instance Cleanup
Instances are considered stale if:
- `lastSeen` is older than 5 minutes
- Process with `pid` is no longer running
- Server is not reachable at specified port

---

## Performance Notes

- **List Diagrams**: O(n) where n = number of open editors
- **Get by Path**: O(1) with file system lookup
- **Update**: O(1) for XML update, plus render time
- **Export**: Depends on diagram complexity (typically < 500ms)

### Recommended Limits
- Max diagram size: 10MB
- Max number of open diagrams: 100
- Request timeout: 30 seconds
- WebView reload timeout: 10 seconds

---

## Security

### Localhost Only
Server binds to `127.0.0.1` only - no external access

### No Authentication
Not required for v1 (local-only access)

### Input Validation
- XML structure validation
- Path traversal prevention
- File extension validation
- Project boundary enforcement

### File Permissions
All writes go through IntelliJ's VFS, respecting:
- Read-only files
- File system permissions
- IDE's safe write mechanism

---

## Debugging

### Enable Debug Logging
Add to IntelliJ's Help → Diagnostic Tools → Debug Log Settings:
```
de.docs_as_co.intellij.plugin.drawio.mcp:TRACE
```

### Check Server Status
```bash
curl http://localhost:8765/api/status
```

### View Instance Registry
```bash
cat ~/.diagrams-net-intellij-mcp/instances.json | jq
```

### Test Port Availability
```bash
nc -zv localhost 8765
# or
lsof -i :8765
```

---

## Version Compatibility

- **Minimum Plugin Version**: 0.3.0 (when MCP is released)
- **IntelliJ Platform**: 2023.3+
- **HTTP Protocol**: HTTP/1.1
- **JSON Format**: RFC 8259
- **MCP Version**: 1.0 (when standardized)
