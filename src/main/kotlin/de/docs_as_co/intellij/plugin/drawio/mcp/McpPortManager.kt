package de.docs_as_co.intellij.plugin.drawio.mcp

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger

/**
 * Manages MCP server port assignment based on IDE product type.
 *
 * Each JetBrains IDE type gets a unique port offset to allow running
 * multiple IDEs simultaneously with MCP servers.
 *
 * Port calculation:
 * 1. Check for per-IDE override: DIAGRAMS_NET_MCP_PORT_<CODE> (e.g., DIAGRAMS_NET_MCP_PORT_WS)
 * 2. Otherwise: base port + product offset
 *
 * After server starts, exports DIAGRAMS_NET_MCP_PORT_CURRENT for Claude Code
 * to discover the active IDE's port.
 */
object McpPortManager {
    private val LOG = Logger.getInstance(McpPortManager::class.java)

    const val DEFAULT_BASE_PORT = 8765
    const val ENV_BASE_PORT = "DIAGRAMS_NET_MCP_PORT"
    const val ENV_CURRENT_PORT = "DIAGRAMS_NET_MCP_PORT_CURRENT"

    /**
     * Port offsets for each IDE product code.
     * IntelliJ IDEA (IC/IU) is the base (offset 0).
     */
    private val PRODUCT_OFFSETS = mapOf(
        "IC" to 0,  // IntelliJ IDEA Community
        "IU" to 0,  // IntelliJ IDEA Ultimate
        "WS" to 1,  // WebStorm
        "PY" to 2,  // PyCharm (Professional)
        "PC" to 2,  // PyCharm Community
        "GO" to 3,  // GoLand
        "RD" to 4,  // Rider
        "RM" to 5,  // RubyMine
        "CL" to 6,  // CLion
        "PS" to 7,  // PhpStorm
        "DB" to 8,  // DataGrip
        "AI" to 9,  // Android Studio (based on IU)
        "DS" to 10  // DataSpell
    )

    /**
     * Get the current IDE's product code.
     */
    fun getProductCode(): String {
        return try {
            ApplicationInfo.getInstance().build.productCode
        } catch (e: Exception) {
            LOG.warn("Failed to get product code, defaulting to IU", e)
            "IU"
        }
    }

    /**
     * Get the port offset for the current IDE.
     */
    fun getProductOffset(): Int {
        val productCode = getProductCode()
        return PRODUCT_OFFSETS[productCode] ?: run {
            LOG.warn("Unknown product code: $productCode, using offset 0")
            0
        }
    }

    /**
     * Calculate the effective port for the MCP server.
     *
     * Priority:
     * 1. Per-IDE env var: DIAGRAMS_NET_MCP_PORT_<CODE>
     * 2. Base port from settings + product offset
     *
     * @param settingsPort The port configured in IDE settings
     * @return The effective port to use
     */
    fun calculatePort(settingsPort: Int): Int {
        val productCode = getProductCode()

        // Check for per-IDE override
        val perIdeEnvVar = "${ENV_BASE_PORT}_$productCode"
        val perIdePort = System.getenv(perIdeEnvVar)?.toIntOrNull()
        if (perIdePort != null) {
            LOG.info("Using per-IDE port from $perIdeEnvVar: $perIdePort")
            return perIdePort
        }

        // Calculate based on settings + offset
        val offset = getProductOffset()
        val calculatedPort = settingsPort + offset
        LOG.info("Calculated port for $productCode: $settingsPort + $offset = $calculatedPort")
        return calculatedPort
    }

    /**
     * Export the current port for Claude Code discovery.
     * Sets DIAGRAMS_NET_MCP_PORT_CURRENT as a JVM system property.
     *
     * Note: IntelliJ's terminal and other child processes inherit JVM
     * system properties, so this is sufficient for typical usage.
     */
    fun exportCurrentPort(port: Int) {
        try {
            System.setProperty(ENV_CURRENT_PORT, port.toString())
            LOG.info("Exported $ENV_CURRENT_PORT=$port as system property")
        } catch (e: Exception) {
            LOG.warn("Failed to export current port as system property", e)
        }
    }

    /**
     * Get a human-readable description of the port configuration.
     */
    fun getPortDescription(port: Int): String {
        val productCode = getProductCode()
        val offset = getProductOffset()
        val basePort = port - offset
        return "Port $port (${getProductName(productCode)}, base=$basePort, offset=$offset)"
    }

    /**
     * Get a human-readable product name.
     */
    private fun getProductName(code: String): String {
        return when (code) {
            "IC" -> "IntelliJ IDEA Community"
            "IU" -> "IntelliJ IDEA Ultimate"
            "WS" -> "WebStorm"
            "PY" -> "PyCharm Professional"
            "PC" -> "PyCharm Community"
            "GO" -> "GoLand"
            "RD" -> "Rider"
            "RM" -> "RubyMine"
            "CL" -> "CLion"
            "PS" -> "PhpStorm"
            "DB" -> "DataGrip"
            "AI" -> "Android Studio"
            "DS" -> "DataSpell"
            else -> "Unknown ($code)"
        }
    }
}
