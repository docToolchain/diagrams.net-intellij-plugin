package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBLabel
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle
import javax.swing.JComponent
import javax.swing.JPanel

class DiagramsSettingsForm : DiagramsSettings.Holder {
    private var myMainPanel: JPanel? = null
    private lateinit var myUiTheme: ComboBox<DiagramsUiTheme>
    private lateinit var myUiMode: ComboBox<DiagramsUiMode>
    private lateinit var myThemeLabel: JBLabel
    private lateinit var myModeLabel: JBLabel
    private var myUiThemeModel: EnumComboBoxModel<DiagramsUiTheme> = EnumComboBoxModel(DiagramsUiTheme::class.java)
    private var myUiModeModel: EnumComboBoxModel<DiagramsUiMode> = EnumComboBoxModel(DiagramsUiMode::class.java)
    val component: JComponent?
        get() = myMainPanel

    private fun createUIComponents() {
    }

    override fun setDiagramsPreviewSettings(settings: DiagramsSettings) {
        myThemeLabel.text = DiagramsNetBundle.message("diagrams.settings.theme")
        myUiTheme.model = myUiThemeModel
        myUiThemeModel.setSelectedItem(settings.uiTheme)

        myModeLabel.text = DiagramsNetBundle.message("diagrams.settings.mode")
        myUiMode.model = myUiModeModel
        myUiModeModel.setSelectedItem(settings.uiMode)
    }

    override fun getDiagramsSettings(): DiagramsSettings {
        return DiagramsSettings(myUiThemeModel.selectedItem, myUiModeModel.selectedItem)
    }
}
