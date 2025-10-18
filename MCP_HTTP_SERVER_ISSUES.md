# MCP HTTP Server Implementation Issues

## Problem Summary

The initial implementation using Ktor/Netty for the MCP HTTP server fails to send responses when running inside the IntelliJ plugin.

## Root Cause

`call.respondText()` and `call.respond()` hang indefinitely in Ktor handlers.
This occurs even for simple endpoints that don't access IntelliJ APIs.

## Investigation Details

### What Works
* MCP server starts successfully on port 8765
* Port is listening and accepts connections
* Ktor routing matches requests and enters handler functions
* Root endpoint `/` works (uses plain `respondText()`)

### What Fails
* ALL `/api/*` endpoints return HTTP 500 with empty response body
* `call.respond()` with Gson content negotiation hangs
* `call.respondText()` with JSON content type also hangs
* No exceptions are thrown or logged

### Attempted Fixes
.
Added comprehensive debug logging
.
Added StatusPages exception handler
.
Wrapped IntelliJ API calls in `runReadAction`
.
Started server in background thread
.
Used `respondText()` instead of Gson serialization

None of these resolved the issue.

## Technical Analysis

The problem appears to be a fundamental incompatibility between:

.
**Ktor's coroutine-based request handling**
.
**Netty's event loop**
.
**IntelliJ's plugin classloader/threading model**

The Netty event loop appears to be blocked or waiting for a resource that's never released due to classloader isolation or thread context issues.

## Dependencies Used

[source,kotlin]
----
implementation("io.ktor:ktor-server-core:2.3.7")
implementation("io.ktor:ktor-server-netty:2.3.7")
implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
implementation("io.ktor:ktor-serialization-gson:2.3.7")
implementation("io.ktor:ktor-server-cors:2.3.7")
implementation("io.ktor:ktor-server-status-pages:2.3.7")
----

## Resolution

After research, **NanoHTTPD** was chosen as the replacement solution:

.
**Proven**: Successfully used by IntelliJ-Automation-Plugin and other plugins
.
**Lightweight**: Single JAR dependency, ~60KB
.
**Simple API**: Straightforward request/response handling
.
**No Threading Issues**: Works well in plugin classloader environment
.
**Independent Port**: Can use our own port (8765) without conflicts

Alternative considered: IntelliJ's built-in httpRequestHandler extension point (port 63342) was considered but would have required using IntelliJ's shared server.
