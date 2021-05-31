package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.util.xmlb.annotations.Attribute

class DiagramsSettings {
    @Attribute("uiTheme", converter = DiagramsUiThemeConverter::class)
    var uiTheme: DiagramsUiTheme = DiagramsUiTheme.DEFAULT

    constructor() {}
    constructor(uiTheme: DiagramsUiTheme) {
        this.uiTheme = uiTheme
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as DiagramsSettings
        return uiTheme == that.uiTheme
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + uiTheme.hashCode()
        return result
    }

    interface Holder {
        fun setDiagramsPreviewSettings(settings: DiagramsSettings)
        fun getDiagramsSettings(): DiagramsSettings
    }

    companion object {
        @JvmField
        val DEFAULT = DiagramsSettings()
    }
}
