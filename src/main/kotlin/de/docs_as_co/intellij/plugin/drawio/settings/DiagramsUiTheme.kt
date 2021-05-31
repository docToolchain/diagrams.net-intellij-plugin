package de.docs_as_co.intellij.plugin.drawio.settings

import de.docs_as_co.intellij.plugin.drawio.MyBundle

enum class DiagramsUiTheme(val key: String) {
    DEFAULT (""),
    DARK ("dark"),
    KENNEDY ("kennedy"),
    ATLAS ("atlas"),
    MINIMAL ("min"),
    SKETCH ("sketch");

    override fun toString(): String {
        return MyBundle.message("diagrams.theme.$name")
    }
}
