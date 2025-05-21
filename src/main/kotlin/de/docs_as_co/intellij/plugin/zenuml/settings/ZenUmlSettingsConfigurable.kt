package de.docs_as_co.intellij.plugin.zenuml.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Configurable for ZenUML settings
 */
class ZenUmlSettingsConfigurable : SearchableConfigurable {
    private var settingsForm: ZenUmlSettingsForm? = null

    override fun getId(): String = "Settings.ZenUml"

    override fun getDisplayName(): String = "ZenUML Sequence Diagram"

    override fun createComponent(): JComponent {
        settingsForm = ZenUmlSettingsForm()
        return settingsForm?.component ?: JPanel()
    }

    override fun isModified(): Boolean {
        val settings = service<ZenUmlApplicationSettings>()
        val form = settingsForm ?: return false
        
        return form.isModified(settings)
    }

    override fun apply() {
        val settings = service<ZenUmlApplicationSettings>()
        val form = settingsForm ?: return
        
        form.apply(settings)
    }

    override fun reset() {
        val settings = service<ZenUmlApplicationSettings>()
        val form = settingsForm ?: return
        
        form.reset(settings)
    }

    override fun disposeUIResources() {
        settingsForm?.let {
            Disposer.dispose(it)
        }
        settingsForm = null
    }
} 