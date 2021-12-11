# Changelog

## [Unreleased]
### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [0.1.12] - 2021-12-11
### Changed
- update minimum required version to 2021.1
- Update to Diagrams.net 15.5.4 (#108)

### Fixed
- add XML header for SVG files (#113)

## [0.1.11] - 2021-07-20
### Fixed
- fix empty preview in Markdown or AsciiDoc editor being empty after editing a diagrams.net diagram (#87)

## [0.1.10] - 2021-07-20
### Fixed
- allow DTDs for draw.io content, but don't download load them if they are remote (#83)

## [0.1.9] - 2021-07-19
### Fixed
- don't resolve external DTDs and other references when checking an SVG for draw.io content (#83)

## [0.1.8] - 2021-06-06
### Added
- Theme of editor can be configured via plugin's settings (#65)

### Changed
- Update to Diagrams.net 14.7.4

## [0.1.7] - 2021-04-27
### Changed
- Update to Diagrams.net 14.6.6

### Fixed
- fix menu ID to avoid clash with AsciiDoc plugin (#48)

## [0.1.6] - 2021-04-24
### Added
- Autodetect editable diagrams.net files (#48)
- Entries in create new file context menu for diagrams.net including an empty template for editable SVG, editable PNG and XML (#48)

### Changed
- Update to Diagrams.net 14.6.0
- Migration to JDK 11 / Project requires minimum 2020.3

## [0.1.5] - 2021-03-01
### Changed
- PR#59 - Restored compatibility with latest version of IntelliJ (2021.1 EAP)

## [0.1.4] - 2021-01-01
### Changed
- PR#41 - Allow the plugin to be installed on more recent IDE builds 
- Vendor-URL updated

## [0.1.3] - 2020-12-23
### fixed
- keyboard shortcuts (#40)

## [0.1.2]
### Added
- draw.io theme adopts to IntelliJ darcula

## [0.1.1]
### fixed
- SVG handling

## [0.1.0]
### Added
-   MVP
-   load *.(drawio|dio)(.svg|.png|) in editor
-   support dark theme for darcula
-   autosave