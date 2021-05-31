package de.docs_as_co.intellij.plugin.drawio.settings

enum class DiagramsUiTheme(val key: String, private val presentation: String) {
    DEFAULT ("", "automatic (like IntelliJ)"),
    DARK ("dark", "Dark"),
    KENNEDY ("kennedy","Light (Kennedy)"),
    ATLAS ("atlas","Atlas"),
    MINIMAL ("min","Minimal"),
    SKETCH ("sketch","Sketch");

    override fun toString(): String {
        return this.presentation;
    }
}
