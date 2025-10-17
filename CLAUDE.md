# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ Platform plugin that integrates diagrams.net (formerly draw.io) directly into JetBrains IDEs. It provides a JCEF (Java Chromium Embedded Framework) based editor for diagram files with extensions `.drawio.(svg|png|xml)` and `.dio.(svg|png|xml)`, and auto-detects editable PNGs and SVGs created with diagrams.net.

The plugin uses an offline version of diagrams.net by default, so it works without an internet connection and keeps content local.

## Development Setup

### Prerequisites

This project uses SDKMAN for Java version management. **The required JDK is JDK 21.0.8-tem** as specified in `.sdkmanrc`:

```bash
# Install and use the correct JDK (21.0.8-tem)
sdk env install
```

**IMPORTANT**: All gradle commands MUST be run with the correct JDK activated:
```bash
source ~/.sdkman/bin/sdkman-init.sh && sdk env && ./gradlew <task>
```

The project requires exactly **JDK 21.0.8-tem (Temurin)**. Using a different JDK version will cause build failures.

### Submodule Initialization

The project includes diagrams.net as a git submodule at `src/webview/drawio`. **This must be initialized before building**:

```bash
git submodule update --init
```

The build will fail with an error if the submodule is not initialized.

## Common Development Commands

### Running and Testing

```bash
# Run IDE with plugin installed (for development/debugging)
./gradlew runIde

# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Run all checks (verification)
./gradlew check
```

### Plugin-Specific Tasks

```bash
# Build plugin ZIP for distribution
./gradlew buildPlugin

# Verify plugin compatibility with target IDE versions
./gradlew verifyPlugin

# Patch plugin.xml with version info
./gradlew patchPluginXml

# Prepare sandbox environment
./gradlew prepareSandbox

# Sign plugin (requires credentials)
./gradlew signPlugin

# Publish to JetBrains Marketplace (requires PUBLISH_TOKEN env var)
./gradlew publishPlugin
```

## Architecture

### Core Components

The plugin architecture consists of these key layers:

1. **Editor Provider Layer** (`DiagramsEditorProvider.kt`)
   - Entry point for IntelliJ's editor system
   - Determines if a file should be opened with the diagrams.net editor via `DiagramsFileUtil.isDiagramsFile()`
   - Creates `DiagramsEditor` instances
   - Contains workaround for JCEF cache lock issues (IJPL-148653)

2. **Editor Layer** (`DiagramsEditor.kt`)
   - Main `FileEditor` implementation
   - Manages the lifecycle of the `DiagramsWebView`
   - Handles file I/O operations (loading and saving)
   - Supports different file formats:
     - XML files (`.drawio`, `.drawio.xml`, `.dio`, `.dio.xml`) - saves as XML
     - SVG files (`.drawio.svg`, `.dio.svg`) - exports as SVG with XML header
     - PNG files (`.drawio.png`, `.dio.png`) - exports as PNG with embedded diagram data
   - Listens to theme changes and settings changes to reload the webview

3. **WebView Layer** (`BaseDiagramsWebView.kt`, `DiagramsWebView.kt`)
   - `BaseDiagramsWebView` manages JCEF browser instance and message passing
   - Registers custom `https://drawio-plugin` scheme handler to load offline diagrams.net assets
   - Implements bidirectional communication between Kotlin and JavaScript via `JBCefJSQuery`
   - `DiagramsWebView` handles diagram-specific operations (load, save, export)
   - Uses Jackson for JSON serialization/deserialization

4. **Message Protocol** (`DrawioWebMessages.kt`)
   - Defines sealed classes for bidirectional communication:
     - `OutgoingMessage.Request` (Export with response)
     - `OutgoingMessage.Event` (Load, Configure, Merge)
     - `IncomingMessage.Response` (Export data, Ack)
     - `IncomingMessage.Event` (Initialized, AutoSave, Save, Configure)
   - Request/response pattern uses unique requestIds for correlation

### Key Technical Details

- **JCEF Integration**: Uses custom scheme handler (`https://drawio-plugin`) instead of `drawio-plugin://` to avoid CORS restrictions in newer Chromium versions
- **Asset Loading**: Diagram.net assets are bundled from `src/webview/drawio/src/main/webapp` into the plugin JAR under `/assets`
- **Theme Support**: Respects IntelliJ's theme (light/dark) and can sync with editor color scheme changes
- **Offline Operation**: All diagrams.net code runs locally via bundled assets
- **File Format Handling**: Supports embedded XML in PNG/SVG for editable diagrams

### Package Structure

- `de.docs_as_co.intellij.plugin.drawio.editor` - Core editor implementation
- `de.docs_as_co.intellij.plugin.drawio.settings` - Plugin settings and configuration UI
- `de.docs_as_co.intellij.plugin.drawio.actions` - Actions (create file, open devtools)
- `de.docs_as_co.intellij.plugin.drawio.icons` - Icon provider for file types
- `de.docs_as_co.intellij.plugin.drawio.utils` - Utility classes (scheme handler, JCEF panel)

## Testing Notes

- Plugin testing uses IntelliJ Platform test framework
- Test sandbox environments are prepared via gradle tasks
- Performance testing is available via `testIdePerformance` task

## Updating diagrams.net Version

To update the bundled diagrams.net version:

```bash
cd src/webview/drawio
git fetch origin
git checkout v<version>  # e.g., v22.1.22
cd ../../..
git add src/webview/drawio
git commit -m "Update diagrams.net to v<version>"
```

Other developers need to run `git submodule update --init` after pulling.

## Configuration

- Plugin metadata is in `gradle.properties` (version, platform version, etc.)
- Plugin descriptor is at `src/main/resources/META-INF/plugin.xml`
- Build configuration is in `build.gradle.kts`
- Supported IDE versions are defined in `pluginVerifierIdeVersions` property

## Publishing

The plugin publishes to different channels based on the `PRE_RELEASE` environment variable:
- `PRE_RELEASE=true` → EAP channel
- `PRE_RELEASE=false` (or unset) → default/stable channel

Requires `PUBLISH_TOKEN` environment variable for authentication.

## MCP Integration (In Design Phase)

An MCP (Model Context Protocol) server integration is being designed to enable LLM interaction with diagrams. Key capabilities:

- **File-Based Access**: Access diagrams by relative path (e.g., `docs/architecture/system.drawio.svg`)
- **Real-Time Updates**: LLM modifications appear immediately in the IntelliJ editor
- **Multi-Instance Support**: Multiple IntelliJ instances can run on different ports
- **Auto-Open**: Diagrams automatically open when accessed via API

### Documentation

- **`MCP_INTEGRATION_DESIGN.md`**: Comprehensive technical design with architecture, API specs, and implementation plan
- **`MCP_QUICK_START.md`**: User-facing guide for setting up and using MCP integration
- **`MCP_IMPLEMENTATION_SUMMARY.md`**: Developer-focused implementation overview with checklists

### Key Concepts

When implementing MCP features, be aware of:
- REST API runs on configurable port (default: 8765)
- Instance discovery file: `~/.diagrams-net-intellij-mcp/instances.json`
- Supports all diagram formats (SVG, PNG, XML) with XML extraction
- Path resolution is relative to project root
- Auto-opening uses IntelliJ's `FileEditorManager`
