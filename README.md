# ZenUML Integration for IntelliJ

[![Build Status (GitHub Workflow Build)](https://github.com/docToolchain/diragrams.net-intellij-plugin/workflows/Build/badge.svg?branch=main)](https://github.com/docToolchain/diragrams.net-intellij-plugin/actions?query=workflow%3ABuild+branch%3Amain)
[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/15635-diagrams-net-integration.svg)](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15635-diagrams-net-integration.svg)](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration)

<!-- Plugin description -->
This official extension integrates [ZenUML](https://ZenUML.com/) directly into all JetBrains IDEs.
It supports ZenUML diagram files with the extensions `.z`, `.zen` and `.zenuml`.

The editor uses an offline version of ZenUML renderer, therefore it works without an internet connection and content stays local in your IDE.

# Free
* Full support of [ZenUML DSL](https://zenuml.atlassian.net/wiki/spaces/Doc/pages/518848513/Sequence+diagram+syntax)
  * Sync and Async messages
  * Creation, Return/Reply messages
  * Alt, Loop, Par, try/catch/finally fragments
* Live preview of your diagrams
* Cloud and other common icons (EC2, Lambda, Database, Boundary, etc.)

# Paid

Generate ZenUML diagram from Java code only on Intellij IDEA.

# Hacking / reverse engineering
To enable DevTools,
1. Open menu item **Help > Find action...** and search for "Registry..."
2. Enable "ide.browser.jcef.contextMenu.devTools.enabled"
<!-- Plugin description end -->

## About

This plugin is still an early version and experimental.
If you like, you can help to evolve it.

![screenshot](images/drawioscreenshot.jpg)

## Installation

Releases are available on the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration). 
Use [Install plugin from repository](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_repo) to install them.

For pre-releases, either 
- download them from the [GitHub releases](https://github.com/docToolchain/diagrams.net-intellij-plugin/releases) and use [Install plugin from disk](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk) or 
- add the URL `https://plugins.jetbrains.com/plugins/eap/list?pluginId=15635` as a [custom plugin repository to your IDE](https://www.jetbrains.com/help/idea/managing-plugins.html#repos).

## References

* https://desk.draw.io/support/solutions/articles/16000042544-embed-mode
* https://github.com/jgraph/drawio-integration
* https://github.com/hediet/vscode-drawio

## Authors

[![](https://img.shields.io/twitter/follow/RalfDMueller.svg?style=social)](https://twitter.com/intent/follow?screen_name=RalfDMueller)

[![](https://img.shields.io/twitter/follow/hediet_dev.svg?style=social)](https://twitter.com/intent/follow?screen_name=hediet_dev)

[![](https://img.shields.io/twitter/follow/ahus1de.svg?style=social)](https://twitter.com/intent/follow?screen_name=ahus1de)

## Docs

An architecture overview can be found at https://drawio-intellij-plugin.netlify.app/ .

## FAQ

### How do I build and run this project?

For development purpose, clone the project locally and start it with the command

`./gradlew runIde`

This will build the plugin and start an Instance of IntelliJ with the plugin already installed.
You can even start this in debug mode.

## Environment Variables

This project requires the `MIXPANEL_PROJECT_TOKEN` to be set as a system environment variable for analytics to work.

### Setting the environment variable
- On macOS/Linux (zsh/bash):
  ```sh
  export MIXPANEL_PROJECT_TOKEN=your_token_here
  ```
- On Windows (cmd):
  ```cmd
  set MIXPANEL_PROJECT_TOKEN=your_token_here
  ```
- In IntelliJ IDEA, you can set environment variables in your Run/Debug Configuration under the "Environment variables" field.

### Note
- Do not commit secrets to version control.
- For production or CI/CD, ensure the environment variable is set in your deployment environment.


