package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.options.SearchableConfigurable
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle.message
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
        val settings = DiagramsApplicationSettings.instance
        return !form.getDiagramsSettings().equals(settings.getDiagramsSettings())
    }

    override fun apply() {
        val oldSettings = DiagramsApplicationSettings.instance.getDiagramsSettings()
        val newSettings = form.getDiagramsSettings()

        DiagramsApplicationSettings.instance.setDiagramsPreviewSettings(newSettings)

        // Handle MCP server start/stop based on settings changes
        val mcpService = de.docs_as_co.intellij.plugin.drawio.mcp.DiagramMcpService.instance

        if (newSettings.mcpServerEnabled != oldSettings.mcpServerEnabled ||
            newSettings.mcpServerPort != oldSettings.mcpServerPort) {

            // Stop server if it was running
            if (mcpService.isServerRunning()) {
                mcpService.stopServer()
            }

            // Start server if enabled
            if (newSettings.mcpServerEnabled) {
                mcpService.startServer(newSettings.mcpServerPort)
            }
        }
    }

    override fun reset() {
        val settings = DiagramsApplicationSettings.instance
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
