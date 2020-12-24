# Diagrams.net Integration For IntelliJ

<!-- Plugin description -->
This unofficial extension integrates [diagrams.net](https://app.diagrams.net/)  (formerly known as draw.io) directly into IntelliJ.
It supports diagram files with the extensions `.drawio.(svg|png)`.
<!-- Plugin description end -->

## About

This plugin is far from being production ready.
In fact, it is currently only a proof of concept.
If you like, you can help to evolve it.

![screenshot](images/drawioscreenshot.jpg)

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


