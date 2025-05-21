package de.docs_as_co.intellij.plugin.drawio

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import de.docs_as_co.intellij.plugin.drawio.utils.Analytics

/**
 * Startup activity that tests Mixpanel analytics on plugin initialization
 */
class DrawioAnalyticsStartup : StartupActivity.DumbAware {
    private val LOG = Logger.getInstance(DrawioAnalyticsStartup::class.java)

    override fun runActivity(project: Project) {
        LOG.info("Starting DrawioAnalyticsStartup activity to test Mixpanel connection")
        
        // Test Mixpanel connection
        Analytics.testMixpanelConnection()
        
        // Also track startup event
        Analytics.trackEvent("plugin_started", mapOf(
            "project_name" to project.name,
            "platform" to System.getProperty("os.name"),
            "java_version" to System.getProperty("java.version")
        ))
        
        LOG.info("Completed DrawioAnalyticsStartup activity")
    }
} 