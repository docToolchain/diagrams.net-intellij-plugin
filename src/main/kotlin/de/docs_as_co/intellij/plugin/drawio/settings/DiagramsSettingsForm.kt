package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle
import de.docs_as_co.intellij.plugin.drawio.mcp.DiagramMcpService
import de.docs_as_co.intellij.plugin.drawio.mcp.McpPortManager
import javax.swing.JComponent
import javax.swing.JPanel

class DiagramsSettingsForm : DiagramsSettings.Holder {
    private var myMainPanel: JPanel? = null
    private lateinit var myUiTheme: ComboBox<DiagramsUiTheme>
    private lateinit var myUiMode: ComboBox<DiagramsUiMode>
    private lateinit var myThemeLabel: JBLabel
    private lateinit var myModeLabel: JBLabel
    private lateinit var myMcpServerEnabled: JBCheckBox
    private lateinit var myMcpServerPort: JBTextField
    private lateinit var myMcpServerStatusLabel: JBLabel
    private var myUiThemeModel: EnumComboBoxModel<DiagramsUiTheme> = EnumComboBoxModel(DiagramsUiTheme::class.java)
    private var myUiModeModel: EnumComboBoxModel<DiagramsUiMode> = EnumComboBoxModel(DiagramsUiMode::class.java)

    val component: JComponent?
        get() = myMainPanel

    private fun updateServerStatus() {
        val service = DiagramMcpService.instance
        if (service.isServerRunning()) {
            myMcpServerStatusLabel.text = "Server Status: Running on port ${service.getActualPort()}"
        } else {
            myMcpServerStatusLabel.text = "Server Status: Stopped"
        }
    }

    override fun setDiagramsPreviewSettings(settings: DiagramsSettings) {
        myThemeLabel.text = DiagramsNetBundle.message("diagrams.settings.theme")
        myUiTheme.model = myUiThemeModel
        myUiThemeModel.setSelectedItem(settings.uiTheme)

        myModeLabel.text = DiagramsNetBundle.message("diagrams.settings.mode")
        myUiMode.model = myUiModeModel
        myUiModeModel.setSelectedItem(settings.uiMode)

        myMcpServerEnabled.isSelected = settings.mcpServerEnabled
        myMcpServerPort.text = settings.mcpServerPort.toString()

        updateServerStatus()
    }

    override fun getDiagramsSettings(): DiagramsSettings {
        val port = try {
            myMcpServerPort.text.toInt()
        } catch (e: NumberFormatException) {
            McpPortManager.DEFAULT_BASE_PORT
        }

        return DiagramsSettings(
            myUiThemeModel.selectedItem,
            myUiModeModel.selectedItem,
            myMcpServerEnabled.isSelected,
            port
        )
    }
}
