package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.util.xmlb.Converter

/**
 * This object converts values to/from the application's XML configuration.
 * As the standard "toString()" method is overridden for the GUI/Swing text to be displayed,
 * this uses the Enum name to serialize the contents. Therefore the presentation and the theme key for Diagrams.net
 * can change independently of the name of the Enum.
 */
object DiagramsUiModeConverter : Converter<DiagramsUiMode>() {
    override fun toString(value: DiagramsUiMode): String {
        return value.name
    }

    override fun fromString(value: String): DiagramsUiMode {
        return try {
            DiagramsUiMode.valueOf(value)
        } catch (ex: IllegalArgumentException) {
            DiagramsUiMode.AUTO
        }
    }

}
