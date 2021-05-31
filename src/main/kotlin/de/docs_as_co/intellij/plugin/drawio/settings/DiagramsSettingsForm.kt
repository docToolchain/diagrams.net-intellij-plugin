package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

class DiagramsSettingsForm : DiagramsSettings.Holder {
    private var myMainPanel: JPanel? = null
    private lateinit var myUiTheme: ComboBox<DiagramsUiTheme>
    private var myUiThemeModel: EnumComboBoxModel<DiagramsUiTheme> = EnumComboBoxModel(DiagramsUiTheme::class.java)
    val component: JComponent?
        get() = myMainPanel

    private fun createUIComponents() {
    }

    override fun setDiagramsPreviewSettings(settings: DiagramsSettings) {
        myUiTheme.model = myUiThemeModel
        myUiThemeModel.setSelectedItem(settings.uiTheme)

    }

    override fun getDiagramsSettings(): DiagramsSettings {
        return DiagramsSettings(myUiThemeModel.selectedItem)
    }
}
