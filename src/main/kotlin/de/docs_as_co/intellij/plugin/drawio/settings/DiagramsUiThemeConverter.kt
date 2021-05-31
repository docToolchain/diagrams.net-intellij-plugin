package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.util.xmlb.Converter

object DiagramsUiThemeConverter : Converter<DiagramsUiTheme>() {
    override fun toString(value: DiagramsUiTheme): String {
        return value.key
    }

    override fun fromString(value: String): DiagramsUiTheme {
        return DiagramsUiTheme.valueOf(value)
    }

}
