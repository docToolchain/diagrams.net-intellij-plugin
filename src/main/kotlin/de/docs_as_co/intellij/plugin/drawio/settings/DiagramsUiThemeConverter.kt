package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.util.xmlb.Converter

/**
 * This object converts values to/from the application's XML configuration.
 * As the standard "toString()" method is overridden for the GUI/Swing text to be displayed,
 * this uses the Enum name to serialize the contents. Therefore the presentation and the theme key for Diagrams.net
 * can change independently of the name of the Enum.
 */
object DiagramsUiThemeConverter : Converter<DiagramsUiTheme>() {
    override fun toString(value: DiagramsUiTheme): String {
        return value.name
    }

    override fun fromString(value: String): DiagramsUiTheme {
        try {
            return DiagramsUiTheme.valueOf(value)
        } catch (ex: IllegalArgumentException) {
            return DiagramsUiTheme.DEFAULT
        }
    }

}
