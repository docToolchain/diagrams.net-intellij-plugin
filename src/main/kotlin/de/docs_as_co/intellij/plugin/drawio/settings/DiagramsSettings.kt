package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.util.xmlb.annotations.Attribute

class DiagramsSettings {
    @Attribute("uiTheme", converter = DiagramsUiThemeConverter::class)
    var uiTheme: DiagramsUiTheme = DiagramsUiTheme.DEFAULT

    @Attribute("uiMode", converter = DiagramsUiModeConverter::class)
    var uiMode: DiagramsUiMode = DiagramsUiMode.AUTO

    @Attribute("mcpServerEnabled")
    var mcpServerEnabled: Boolean = false

    @Attribute("mcpServerPort")
    var mcpServerPort: Int = 8765

    constructor() {}
    constructor(uiTheme: DiagramsUiTheme, uiMode: DiagramsUiMode) {
        this.uiTheme = uiTheme
        this.uiMode = uiMode
    }
    constructor(uiTheme: DiagramsUiTheme, uiMode: DiagramsUiMode, mcpServerEnabled: Boolean, mcpServerPort: Int) {
        this.uiTheme = uiTheme
        this.uiMode = uiMode
        this.mcpServerEnabled = mcpServerEnabled
        this.mcpServerPort = mcpServerPort
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as DiagramsSettings
        return uiTheme == that.uiTheme &&
               uiMode == that.uiMode &&
               mcpServerEnabled == that.mcpServerEnabled &&
               mcpServerPort == that.mcpServerPort
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + uiTheme.hashCode() + uiMode.hashCode()
        result = 31 * result + mcpServerEnabled.hashCode()
        result = 31 * result + mcpServerPort.hashCode()
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
