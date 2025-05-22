package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.BoxLayout
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.Dimension

class DiagramsSettingsForm : DiagramsSettings.Holder {
    private var myMainPanel: JPanel? = null
    private val myUiTheme: ComboBox<DiagramsUiTheme> = ComboBox(EnumComboBoxModel(DiagramsUiTheme::class.java))
    private val myUiMode: ComboBox<DiagramsUiMode> = ComboBox(EnumComboBoxModel(DiagramsUiMode::class.java))
    private val myThemeLabel: JBLabel = JBLabel()
    private val myModeLabel: JBLabel = JBLabel()
    val component: JComponent?
        get() = myMainPanel

    init {
        // Create panel with GridBagLayout for better control
        myMainPanel = JPanel(GridBagLayout())
        
        // Set preferred size to comboboxes to prevent them from being too wide
        myUiTheme.preferredSize = Dimension(200, 30)
        myUiMode.preferredSize = Dimension(200, 30)
        
        // Create constraints
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.WEST
        constraints.insets = Insets(5, 5, 5, 5)
        
        // Add theme label and combobox
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.gridwidth = 1
        constraints.weightx = 0.0
        myMainPanel!!.add(myThemeLabel, constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        myMainPanel!!.add(myUiTheme, constraints)
        
        // Add mode label and combobox
        constraints.gridx = 0
        constraints.gridy = 1
        constraints.weightx = 0.0
        myMainPanel!!.add(myModeLabel, constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        myMainPanel!!.add(myUiMode, constraints)
        
        // Add filler at the bottom to push everything to the top
        constraints.gridx = 0
        constraints.gridy = 2
        constraints.gridwidth = 2
        constraints.weighty = 1.0
        constraints.fill = GridBagConstraints.BOTH
        myMainPanel!!.add(JPanel(), constraints)
    }

    override fun setDiagramsPreviewSettings(settings: DiagramsSettings) {
        myThemeLabel.text = DiagramsNetBundle.message("diagrams.settings.theme")
        myUiTheme.selectedItem = settings.uiTheme
        myModeLabel.text = DiagramsNetBundle.message("diagrams.settings.mode")
        myUiMode.selectedItem = settings.uiMode
    }

    override fun getDiagramsSettings(): DiagramsSettings {
        return DiagramsSettings(myUiTheme.selectedItem as DiagramsUiTheme, myUiMode.selectedItem as DiagramsUiMode)
    }
}
