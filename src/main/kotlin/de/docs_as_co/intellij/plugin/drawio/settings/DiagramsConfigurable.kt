package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.util.Alarm
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle.message
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class DiagramsConfigurable : SearchableConfigurable {
    private var myForm: DiagramsSettingsForm? = null
    private var statusUpdateAlarm: Alarm? = null
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

        // Calculate effective port (includes IDE-specific offset)
        val effectivePort = de.docs_as_co.intellij.plugin.drawio.mcp.McpPortManager.calculatePort(newSettings.mcpServerPort)

        // Restart server if:
        // - enabled/disabled state changed, OR
        // - configured port changed, OR
        // - actual running port differs from effective port (e.g., due to port fallback)
        val needsRestart = newSettings.mcpServerEnabled != oldSettings.mcpServerEnabled ||
            newSettings.mcpServerPort != oldSettings.mcpServerPort ||
            (mcpService.isServerRunning() && mcpService.getActualPort() != effectivePort)

        if (needsRestart) {
            // Stop server if it was running
            if (mcpService.isServerRunning()) {
                mcpService.stopServer()
            }

            // Start server if enabled, with callback to refresh status
            if (newSettings.mcpServerEnabled) {
                // Capture reference to form before async operation
                val currentForm = myForm
                // Show "Starting..." status immediately
                currentForm?.setServerStatusStarting(effectivePort)

                // Track if callback already handled notification
                var notificationShown = false

                mcpService.startServer(effectivePort) { actualPort ->
                    // Callback invoked on EDT when server is started
                    currentForm?.updateServerStatus()

                    // Also show notification balloon for fallback (visible even after dialog closes)
                    if (actualPort != effectivePort && !notificationShown) {
                        notificationShown = true
                        com.intellij.notification.NotificationGroupManager.getInstance()
                            .getNotificationGroup("DiagramsNet")
                            .createNotification(
                                "MCP Server Port",
                                "Port $effectivePort was busy. Server started on port $actualPort instead.",
                                com.intellij.notification.NotificationType.WARNING
                            )
                            .notify(null)
                    }
                }

                // Fallback: Update status after a short delay in case callback doesn't fire
                // Cancel any previous pending requests and reuse/create the alarm
                statusUpdateAlarm?.cancelAllRequests()
                if (statusUpdateAlarm == null) {
                    statusUpdateAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)
                }
                statusUpdateAlarm!!.addRequest({
                    currentForm?.updateServerStatus()
                    // Also show notification balloon for fallback
                    val actualPort = mcpService.getActualPort()
                    if (actualPort != 0 && actualPort != effectivePort && !notificationShown) {
                        notificationShown = true
                        com.intellij.notification.NotificationGroupManager.getInstance()
                            .getNotificationGroup("DiagramsNet")
                            .createNotification(
                                "MCP Server Port",
                                "Port $effectivePort was busy. Server started on port $actualPort instead.",
                                com.intellij.notification.NotificationType.WARNING
                            )
                            .notify(null)
                    }
                }, 500)
            } else {
                // Server disabled - update status immediately
                form.updateServerStatus()
            }
        } else {
            form.updateServerStatus()
        }
    }

    override fun reset() {
        val settings = DiagramsApplicationSettings.instance
        form.setDiagramsPreviewSettings(settings.getDiagramsSettings())
    }

    override fun disposeUIResources() {
        statusUpdateAlarm?.cancelAllRequests()
        statusUpdateAlarm = null
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
