package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.options.SearchableConfigurable
import de.docs_as_co.intellij.plugin.drawio.MyBundle.message
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class DiagramsConfigurable : SearchableConfigurable {
    private var myForm: DiagramsSettingsForm? = null
    override fun getId(): String {
        return "Settings.DiagramsNet.Preview"
    }

    override fun enableSearch(option: String): Runnable? {
        return null
    }

    @Suppress("DialogTitleCapitalization")
    override fun getDisplayName(): @Nls String {
        return message("settings.diagrams.name")
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent? {
        return form.component
    }

    override fun isModified(): Boolean {
        val settings = ZenumlUniversalApplicationSettings.instance
        return !form.getDiagramsSettings().equals(settings.getDiagramsSettings())
    }

    override fun apply() {
        val settings = ZenumlUniversalApplicationSettings.instance
        settings.setDiagramsPreviewSettings(form.getDiagramsSettings())
    }

    override fun reset() {
        val settings = ZenumlUniversalApplicationSettings.instance
        form.setDiagramsPreviewSettings(settings.getDiagramsSettings())
    }

    override fun disposeUIResources() {
        myForm = null
    }

    val form: DiagramsSettingsForm
        get() {
            if (myForm == null) {
                myForm = DiagramsSettingsForm()
            }
            return myForm!!
        }
}
