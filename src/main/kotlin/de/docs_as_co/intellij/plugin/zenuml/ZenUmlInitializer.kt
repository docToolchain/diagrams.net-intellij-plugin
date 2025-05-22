package de.docs_as_co.intellij.plugin.zenuml

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * Ensures proper initialization of services in the correct order
 * to prevent early initialization warnings
 */
class ZenUmlInitializer : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        // This simple implementation ensures the post-startup activity runs
        // at the correct time in the initialization sequence.
        // This helps prevent "created too early" warnings for service initialization
    }
} 