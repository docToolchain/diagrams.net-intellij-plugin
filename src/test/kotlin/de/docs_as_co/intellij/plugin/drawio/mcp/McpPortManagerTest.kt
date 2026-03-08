package de.docs_as_co.intellij.plugin.drawio.mcp

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for McpPortManager port calculation logic.
 *
 * Tests the priority order:
 * 1. Per-IDE env var: DIAGRAMS_NET_MCP_PORT_<CODE>
 * 2. Base env var: DIAGRAMS_NET_MCP_PORT
 * 3. Settings port + product offset
 */
class McpPortManagerTest {

    private val modifiableEnv: MutableMap<String, String> by lazy {
        getModifiableEnvironmentMap()
    }

    private val envVarsToCleanup = mutableListOf<String>()

    @Before
    fun setUp() {
        // Clear any existing test env vars
        clearTestEnvVars()
    }

    @After
    fun tearDown() {
        // Clean up any env vars set during tests
        clearTestEnvVars()
    }

    private fun clearTestEnvVars() {
        envVarsToCleanup.forEach { modifiableEnv.remove(it) }
        envVarsToCleanup.clear()
        // Also clear the standard env vars we test with
        modifiableEnv.remove(McpPortManager.ENV_BASE_PORT)
        modifiableEnv.remove("${McpPortManager.ENV_BASE_PORT}_IC")
        modifiableEnv.remove("${McpPortManager.ENV_BASE_PORT}_WS")
    }

    private fun setEnvVar(name: String, value: String) {
        modifiableEnv[name] = value
        envVarsToCleanup.add(name)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getModifiableEnvironmentMap(): MutableMap<String, String> {
        val env = System.getenv()
        val envClass = env.javaClass
        val field = envClass.declaredFields.firstOrNull { it.type == Map::class.java }
            ?: throw RuntimeException("Could not find Map field in ${envClass.name}")
        field.isAccessible = true
        return field.get(env) as MutableMap<String, String>
    }

    // ========== Tests for settings-based port calculation ==========

    @Test
    fun `calculatePort returns settings port when no env vars set`() {
        // Given: no env vars set, settings port is 8765
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should return settings port (with offset, but IC offset is 0)
        assertEquals(settingsPort, result)
    }

    @Test
    fun `calculatePort returns settings port plus offset for non-IC IDEs`() {
        // Note: This test is limited because we can't easily mock ApplicationInfo
        // The actual offset depends on the running IDE
        val settingsPort = 8765
        val result = McpPortManager.calculatePort(settingsPort)

        // Result should be >= settings port (offset is always >= 0)
        assertTrue("Port should be >= settings port", result >= settingsPort)
    }

    // ========== Tests for base env var (DIAGRAMS_NET_MCP_PORT) ==========

    @Test
    fun `calculatePort uses base env var when set`() {
        // Given: base env var is set
        setEnvVar(McpPortManager.ENV_BASE_PORT, "9000")
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should use env var port (with offset applied)
        // For IC, offset is 0, so result should be 9000
        assertEquals(9000, result)
    }

    @Test
    fun `calculatePort ignores invalid base env var`() {
        // Given: base env var is set to invalid value
        setEnvVar(McpPortManager.ENV_BASE_PORT, "not-a-number")
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should fall back to settings port
        assertEquals(settingsPort, result)
    }

    @Test
    fun `calculatePort ignores empty base env var`() {
        // Given: base env var is set to empty string
        setEnvVar(McpPortManager.ENV_BASE_PORT, "")
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should fall back to settings port
        assertEquals(settingsPort, result)
    }

    // ========== Tests for per-IDE env var (DIAGRAMS_NET_MCP_PORT_<CODE>) ==========

    @Test
    fun `calculatePort uses per-IDE env var when set`() {
        // Given: per-IDE env var is set (assuming test runs in IC)
        val productCode = McpPortManager.getProductCode()
        setEnvVar("${McpPortManager.ENV_BASE_PORT}_$productCode", "9500")
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should use per-IDE env var port exactly (no offset)
        assertEquals(9500, result)
    }

    @Test
    fun `calculatePort prefers per-IDE env var over base env var`() {
        // Given: both env vars are set
        val productCode = McpPortManager.getProductCode()
        setEnvVar(McpPortManager.ENV_BASE_PORT, "9000")
        setEnvVar("${McpPortManager.ENV_BASE_PORT}_$productCode", "9500")
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should use per-IDE env var (higher priority)
        assertEquals(9500, result)
    }

    @Test
    fun `calculatePort falls back to base env var when per-IDE not set`() {
        // Given: only base env var is set
        setEnvVar(McpPortManager.ENV_BASE_PORT, "9000")
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should use base env var
        assertEquals(9000, result)
    }

    @Test
    fun `calculatePort ignores invalid per-IDE env var and uses base`() {
        // Given: per-IDE env var is invalid, base is valid
        val productCode = McpPortManager.getProductCode()
        setEnvVar("${McpPortManager.ENV_BASE_PORT}_$productCode", "invalid")
        setEnvVar(McpPortManager.ENV_BASE_PORT, "9000")
        val settingsPort = 8765

        // When
        val result = McpPortManager.calculatePort(settingsPort)

        // Then: should use base env var
        assertEquals(9000, result)
    }

    // ========== Tests for product offset logic ==========

    @Test
    fun `getProductOffset returns valid offset`() {
        // When
        val offset = McpPortManager.getProductOffset()

        // Then: offset should be non-negative
        assertTrue("Offset should be >= 0", offset >= 0)
        assertTrue("Offset should be < 20", offset < 20)
    }

    @Test
    fun `getProductCode returns non-empty string`() {
        // When
        val productCode = McpPortManager.getProductCode()

        // Then
        assertTrue("Product code should not be empty", productCode.isNotEmpty())
    }

    // ========== Tests for port description ==========

    @Test
    fun `getPortDescription returns formatted string`() {
        // When
        val description = McpPortManager.getPortDescription(8765)

        // Then
        assertTrue("Description should contain port", description.contains("8765"))
        assertTrue("Description should contain 'Port'", description.contains("Port"))
    }

    // ========== Tests for DEFAULT_BASE_PORT constant ==========

    @Test
    fun `DEFAULT_BASE_PORT is 8765`() {
        assertEquals(8765, McpPortManager.DEFAULT_BASE_PORT)
    }

    @Test
    fun `ENV_BASE_PORT is correct`() {
        assertEquals("DIAGRAMS_NET_MCP_PORT", McpPortManager.ENV_BASE_PORT)
    }

    @Test
    fun `ENV_CURRENT_PORT is correct`() {
        assertEquals("DIAGRAMS_NET_MCP_PORT_CURRENT", McpPortManager.ENV_CURRENT_PORT)
    }
}
