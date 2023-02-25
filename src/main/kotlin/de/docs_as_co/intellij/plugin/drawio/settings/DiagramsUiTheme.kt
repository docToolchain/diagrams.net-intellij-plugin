package de.docs_as_co.intellij.plugin.drawio.settings

import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle

enum class DiagramsUiTheme(val key: String) {
    DEFAULT ("kennedy"),
    ATLAS ("atlas"),
    MINIMAL ("min"),
    SKETCH ("sketch"),
    SIMPLE ("simple");

    override fun toString(): String {
        return DiagramsNetBundle.message("diagrams.theme.$name")
    }
}
