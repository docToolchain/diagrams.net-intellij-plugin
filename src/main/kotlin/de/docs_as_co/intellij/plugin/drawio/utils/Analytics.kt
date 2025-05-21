package de.docs_as_co.intellij.plugin.drawio.utils

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import de.docs_as_co.intellij.plugin.drawio.settings.DiagramsApplicationSettings
import org.json.JSONObject
import com.mixpanel.mixpanelapi.MessageBuilder
import com.mixpanel.mixpanelapi.MixpanelAPI
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL

object Analytics {
    private const val PROJECT_TOKEN = ""
    private const val USER_ID_KEY = "diagramly.analytics.user.id"
    private val messageBuilder = MessageBuilder(PROJECT_TOKEN)
    private val mixpanel = MixpanelAPI()
    private val LOG = Logger.getInstance(Analytics::class.java)

    // Analytics log file
    private val LOG_FILE = File(System.getProperty("java.io.tmpdir"), "drawio_analytics.log")

    init {
        // Initialize log file with header
        if (!LOG_FILE.exists()) {
            LOG_FILE.createNewFile()
            logToFile("=== DrawIO Analytics Log Started at ${Date()} ===")
            logToFile("Project Token: $PROJECT_TOKEN")
            logToFile("Log File Location: ${LOG_FILE.absolutePath}")
            logToFile("Java Version: ${System.getProperty("java.version")}")
            logToFile("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
        }
    }

    private fun logToFile(message: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
            LOG_FILE.appendText("[$timestamp] $message\n")
        } catch (e: Exception) {
            LOG.warn("Failed to write to analytics log file", e)
        }
    }

    private fun getOrCreateUserId(): String {
        val props = PropertiesComponent.getInstance()
        var id = props.getValue(USER_ID_KEY)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            props.setValue(USER_ID_KEY, id)
            LOG.info("Generated new analytics user ID: $id")
            logToFile("Generated new analytics user ID: $id")
        } else {
            LOG.debug("Using existing analytics user ID: $id")
        }
        return id
    }

    fun trackEvent(eventName: String, properties: Map<String, Any?> = emptyMap()) {
        val settings = DiagramsApplicationSettings.instance.state
        if (settings?.analyticsEnabled != true) {
            LOG.info("Analytics disabled, not sending event: $eventName")
            logToFile("Analytics disabled, not sending event: $eventName")
            return
        }

        try {
            val userId = getOrCreateUserId()
            val trackingMessage = "Tracking event: $eventName, userId: $userId, properties: $properties"
            LOG.info(trackingMessage)
            logToFile(trackingMessage)

            val jsonProps = JSONObject(properties)
            LOG.debug("JSON properties: ${jsonProps}")

            val event = messageBuilder.event(userId, eventName, jsonProps)
            LOG.debug("Sending event to Mixpanel: $event")
            logToFile("Sending event JSON: $event")

            // Try direct HTTP POST to Mixpanel for debug purposes
            val directPostSuccess = sendDirectMixpanelEvent(userId, eventName, jsonProps)
            logToFile("Direct HTTP POST result: $directPostSuccess")

            // Also send via the Mixpanel API
            val response = mixpanel.sendMessage(event)
            LOG.info("Mixpanel API response: $response")
            logToFile("Mixpanel API response: $response")
        } catch (e: Exception) {
            val errorMsg = "Error sending analytics event: $eventName - ${e.message}"
            LOG.error(errorMsg, e)
            logToFile("ERROR: $errorMsg")
            logToFile("Stack trace: ${e.stackTraceToString()}")
        }
    }

    private fun sendDirectMixpanelEvent(distinctId: String, eventName: String, properties: JSONObject): Boolean {
        try {
            val event = JSONObject()
            event.put("event", eventName)

            // Make a copy of properties to avoid modifying the original
            val props = JSONObject()
            // Copy all properties from the original
            for (key in properties.keys()) {
                props.put(key, properties.get(key))
            }

            // Add required Mixpanel properties
            props.put("distinct_id", distinctId)
            props.put("token", PROJECT_TOKEN)
            event.put("properties", props)

            val data = "data=${java.net.URLEncoder.encode(event.toString(), "UTF-8")}"
            logToFile("Direct POST data: $data")

            val url = URL("https://api.mixpanel.com/track")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            connection.connectTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
            connection.readTimeout = TimeUnit.SECONDS.toMillis(10).toInt()

            logToFile("Connecting to: ${url.toString()}")

            connection.outputStream.use { os ->
                os.write(data.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage
            logToFile("HTTP Response: $responseCode - $responseMessage")

            connection.inputStream.use { inputStream ->
                val response = inputStream.bufferedReader().use { it.readText() }
                logToFile("Response body: $response")
            }

            return responseCode == 200
        } catch (e: Exception) {
            logToFile("ERROR in direct HTTP POST: ${e.message}")
            logToFile("Stack trace: ${e.stackTraceToString()}")
            return false
        }
    }

    /**
     * Test function to manually verify Mixpanel connectivity.
     * Call this from a place where you can observe logs to verify events are being sent.
     */
    fun testMixpanelConnection() {
        LOG.info("Testing Mixpanel connection...")
        logToFile("=== TESTING MIXPANEL CONNECTION ===")
        logToFile("Log file location for support: ${LOG_FILE.absolutePath}")

        trackEvent("test_event", mapOf(
            "test_property" to "test_value",
            "timestamp" to System.currentTimeMillis()
        ))

        LOG.info("Test event sent. Check logs at: ${LOG_FILE.absolutePath}")
        logToFile("=== TEST COMPLETE ===")
    }
}
