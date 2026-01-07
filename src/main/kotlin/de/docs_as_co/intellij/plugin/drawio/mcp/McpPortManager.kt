package de.docs_as_co.intellij.plugin.drawio.mcp

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger

/**
 * Manages MCP server port assignment based on IDE product type.
 *
 * Each JetBrains IDE type gets a unique port offset to allow running
 * multiple IDEs simultaneously with MCP servers.
 *
 * Port calculation priority:
 * 1. Per-IDE env var: DIAGRAMS_NET_MCP_PORT_<CODE> (e.g., DIAGRAMS_NET_MCP_PORT_WS)
 * 2. Base env var: DIAGRAMS_NET_MCP_PORT (applies to all IDEs, with offset)
 * 3. Settings port + product offset
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
     * 1. Per-IDE env var: DIAGRAMS_NET_MCP_PORT_<CODE> (e.g., DIAGRAMS_NET_MCP_PORT_WS) - exact port
     * 2. Base env var: DIAGRAMS_NET_MCP_PORT - applies offset for IDE-specific default
     * 3. Settings port:
     *    - If DEFAULT_BASE_PORT (8765): apply offset for IDE-specific default
     *    - If user changed it: use exactly what they set (no offset)
     *
     * @param settingsPort The port configured in IDE settings
     * @return The effective port to use
     */
    fun calculatePort(settingsPort: Int): Int {
        val productCode = getProductCode()
        val offset = getProductOffset()

        // Check for per-IDE override (exact port, no offset)
        val perIdeEnvVar = "${ENV_BASE_PORT}_$productCode"
        val perIdePort = System.getenv(perIdeEnvVar)?.toIntOrNull()
        if (perIdePort != null) {
            LOG.info("Using per-IDE port from $perIdeEnvVar: $perIdePort")
            return perIdePort
        }

        // Check for base env var (applies offset)
        val baseEnvPort = System.getenv(ENV_BASE_PORT)?.toIntOrNull()
        if (baseEnvPort != null) {
            val calculatedPort = baseEnvPort + offset
            LOG.info("Using base env port from $ENV_BASE_PORT: $baseEnvPort + $offset = $calculatedPort")
            return calculatedPort
        }

        // Only apply offset if using the default base port
        // If user explicitly set a different port, use it directly
        return if (settingsPort == DEFAULT_BASE_PORT) {
            val calculatedPort = settingsPort + offset
            LOG.info("Using default port for $productCode: $settingsPort + $offset = $calculatedPort")
            calculatedPort
        } else {
            LOG.info("Using user-configured port: $settingsPort (no offset)")
            settingsPort
        }
    }

    // Lazy-initialized mutable environment map obtained via reflection
    // This follows the same approach as the intellij-direnv plugin
    private val modifiableEnvironment: MutableMap<String, String> by lazy {
        getModifiableEnvironmentMap()
    }

    /**
     * Get the underlying mutable map from System.getenv() using reflection.
     * System.getenv() returns an UnmodifiableMap, but we can access the
     * underlying mutable map via reflection.
     */
    @Suppress("UNCHECKED_CAST")
    private fun getModifiableEnvironmentMap(): MutableMap<String, String> {
        val env = System.getenv()
        val envClass = env.javaClass

        // Find the internal 'm' field that holds the actual mutable map
        val field = envClass.declaredFields.firstOrNull { it.type == Map::class.java }
            ?: throw RuntimeException("Could not find Map field in ${envClass.name}")

        field.isAccessible = true
        return field.get(env) as MutableMap<String, String>
    }

    /**
     * Export the current port for Claude Code discovery.
     * Sets DIAGRAMS_NET_MCP_PORT_CURRENT as an environment variable
     * so that child processes (like terminals) can inherit it.
     *
     * Uses reflection to modify the JVM's environment map, following
     * the same approach as the intellij-direnv plugin.
     */
    fun exportCurrentPort(port: Int) {
        try {
            // Set as actual environment variable (inherited by child processes)
            modifiableEnvironment[ENV_CURRENT_PORT] = port.toString()
            LOG.info("Exported $ENV_CURRENT_PORT=$port as environment variable")

            // Also set as system property for in-process access
            System.setProperty(ENV_CURRENT_PORT, port.toString())
        } catch (e: Exception) {
            LOG.warn("Failed to export current port as environment variable", e)
            // Fallback to system property only
            try {
                System.setProperty(ENV_CURRENT_PORT, port.toString())
                LOG.info("Fallback: Exported $ENV_CURRENT_PORT=$port as system property only")
            } catch (e2: Exception) {
                LOG.error("Failed to export current port", e2)
            }
        }
    }

    /**
     * Clear the current port environment variable when server stops.
     * This ensures terminals opened after server stop don't see a stale port.
     */
    fun clearCurrentPort() {
        try {
            modifiableEnvironment.remove(ENV_CURRENT_PORT)
            System.clearProperty(ENV_CURRENT_PORT)
            LOG.info("Cleared $ENV_CURRENT_PORT environment variable")
        } catch (e: Exception) {
            LOG.warn("Failed to clear current port environment variable", e)
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
