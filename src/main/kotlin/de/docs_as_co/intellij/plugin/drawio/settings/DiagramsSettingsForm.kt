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

    /**
     * Update the server status label. Called when settings are loaded
     * and after the server is restarted via Apply.
     * Automatically detects port fallback and environment variable overrides.
     */
    fun updateServerStatus() {
        val service = DiagramMcpService.instance
        if (service.isServerRunning()) {
            val actualPort = service.getActualPort()
            // Get the configured port from settings and calculate effective port
            val settings = DiagramsApplicationSettings.instance.getDiagramsSettings()
            val configuredPort = McpPortManager.calculatePort(settings.mcpServerPort)

            // Check if env var is overriding the port
            val productCode = McpPortManager.getProductCode()
            val perIdeEnvVar = "${McpPortManager.ENV_BASE_PORT}_$productCode"
            val perIdePort = System.getenv(perIdeEnvVar)?.toIntOrNull()
            val baseEnvPort = System.getenv(McpPortManager.ENV_BASE_PORT)?.toIntOrNull()

            // Determine the requested port (from env var or settings)
            val envVarName = when {
                perIdePort != null -> perIdeEnvVar
                baseEnvPort != null -> McpPortManager.ENV_BASE_PORT
                else -> null
            }
            val requestedPort = perIdePort ?: baseEnvPort ?: configuredPort

            val statusText = when {
                envVarName != null && actualPort != requestedPort ->
                    "Running on port $actualPort (from $envVarName=$requestedPort, was busy)"
                envVarName != null ->
                    "Running on port $actualPort (from $envVarName)"
                actualPort != configuredPort ->
                    "Running on port $actualPort (port $configuredPort was busy)"
                else ->
                    "Running on port $actualPort"
            }
            myMcpServerStatusLabel.text = "Server Status: $statusText"
        } else {
            myMcpServerStatusLabel.text = "Server Status: Stopped"
        }
    }

    /**
     * Show "Starting..." status immediately when server is being started.
     * This provides immediate feedback before the async server start completes.
     */
    fun setServerStatusStarting(port: Int) {
        myMcpServerStatusLabel.text = "Server Status: Starting on port $port..."
    }

    override fun setDiagramsPreviewSettings(settings: DiagramsSettings) {
        myThemeLabel.text = DiagramsNetBundle.message("diagrams.settings.theme")
        myUiTheme.model = myUiThemeModel
        myUiThemeModel.setSelectedItem(settings.uiTheme)

        myModeLabel.text = DiagramsNetBundle.message("diagrams.settings.mode")
        myUiMode.model = myUiModeModel
        myUiModeModel.setSelectedItem(settings.uiMode)

        myMcpServerEnabled.isSelected = settings.mcpServerEnabled

        // Check if port is overridden by environment variable
        val productCode = McpPortManager.getProductCode()
        val perIdeEnvVar = "${McpPortManager.ENV_BASE_PORT}_$productCode"
        val perIdePort = System.getenv(perIdeEnvVar)?.toIntOrNull()
        val baseEnvPort = System.getenv(McpPortManager.ENV_BASE_PORT)?.toIntOrNull()

        // Calculate the effective port (what will actually be used)
        val effectivePort = McpPortManager.calculatePort(settings.mcpServerPort)

        when {
            perIdePort != null -> {
                myMcpServerPort.text = perIdePort.toString()
                myMcpServerPort.isEnabled = false
                myMcpServerPort.toolTipText = "Port is set by environment variable $perIdeEnvVar"
            }
            baseEnvPort != null -> {
                myMcpServerPort.text = effectivePort.toString()
                myMcpServerPort.isEnabled = false
                myMcpServerPort.toolTipText = "Port is set by environment variable ${McpPortManager.ENV_BASE_PORT}"
            }
            else -> {
                myMcpServerPort.text = effectivePort.toString()
                myMcpServerPort.isEnabled = true
                myMcpServerPort.toolTipText = if (settings.mcpServerPort == McpPortManager.DEFAULT_BASE_PORT) {
                    "Default port for this IDE (change to override)"
                } else null
            }
        }

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
