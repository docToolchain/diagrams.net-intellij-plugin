package de.docs_as_co.intellij.plugin.drawio.settings

import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle

enum class DiagramsUiMode(val key: String) {
    AUTO ("auto"),
    LIGHT ("0"),
    DARK ("1");

    override fun toString(): String {
        return DiagramsNetBundle.message("diagrams.mode.$name")
    }
}
