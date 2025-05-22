package de.docs_as_co.intellij.plugin.zenuml.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JLabel
import java.awt.BorderLayout
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

/**
 * Editor for ZenUML files.
 * This editor integrates with the ZenUmlWebView to provide a split view with code editing and diagram preview.
 */
class ZenUmlEditor(private val project: Project, private val file: VirtualFile) : FileEditor, EditorColorsListener, DumbAware {
    private val LOG: Logger = Logger.getInstance(ZenUmlEditor::class.java)
    private val lifetimeDef = LifetimeDefinition()
    private val lifetime = lifetimeDef.lifetime
    private val userDataHolder = UserDataHolderBase()
    private val panel = JPanel(BorderLayout())

    private var view: ZenUmlWebView? = null
    
    init {
        LOG.info("Initializing ZenUML Editor for ${file.path}")

        try {
            // Subscribe to changes of the theme
            val settingsConnection = ApplicationManager.getApplication().messageBus.connect(this)
            settingsConnection.subscribe(EditorColorsManager.TOPIC, this)

            // Initialize the WebView with the current theme
            val isDarkTheme = JBColor.isBright()
            val theme = if (isDarkTheme) "dark" else "light"
            LOG.info("Creating ZenUmlWebView with theme: $theme")

            try {
                view = ZenUmlWebView(lifetime, theme)

                // Add the WebView component to the panel
                panel.add(view!!.component, BorderLayout.CENTER)

                // Load the initial content
                loadContent()

                // Listen for changes in the WebView content
                view!!.codeContent.advise(lifetime) { newContent ->
                    if (newContent != null) {
                        LOG.info("Content changed in webview, updating file. Content length: ${newContent.length}")
                        updateFileContent(newContent)
                    } else {
                        LOG.info("Received null content from webview")
                    }
                }

                // Listen for initialization
                view!!.initialized().onSuccess {
                    LOG.info("ZenUML WebView initialized successfully")
                }.onError { error ->
                    LOG.error("Failed to initialize ZenUML WebView", error)

                    // Remove the WebView component and show an error message
                    panel.remove(view!!.component)
                    panel.add(JLabel("Error: ${error.message}"), BorderLayout.CENTER)
                    panel.revalidate()
                    panel.repaint()
                }
            } catch (e: Exception) {
                LOG.error("Error creating ZenUmlWebView", e)
                panel.add(JLabel("Error: ${e.message}"), BorderLayout.CENTER)
            }
        } catch (e: Exception) {
            LOG.error("Error initializing ZenUML Editor", e)
            panel.add(JLabel("Error: ${e.message}"), BorderLayout.CENTER)
        }
    }

    /**
     * Load the content from the file into the WebView
     * Uses proper threading model to avoid read access exceptions
     */
    private fun loadContent() {
        try {
            view?.initialized()?.then {
                // Make sure we're on the EDT thread with read access
                ApplicationManager.getApplication().invokeLater {
                    ApplicationManager.getApplication().runReadAction {
                        try {
                            val content = file.inputStream.reader().readText()
                            LOG.info("Loading initial content from file: ${file.path}, length: ${content.length}")
                            view?.loadCode(content)
                        } catch (e: Exception) {
                            LOG.error("Error reading file content", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error loading content", e)
        }
    }

    /**
     * Update the file content when changes are made in the WebView
     * Uses direct file writing to ensure changes are saved to disk
     */
    private fun updateFileContent(newContent: String) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                file.getOutputStream(this).apply {
                    writer().apply {
                        write(newContent)
                        flush()
                    }
                    flush()
                    close()
                }
            }
        }
    }

    /**
     * Handle theme changes
     */
    override fun globalSchemeChange(scheme: EditorColorsScheme?) {
        try {
            val isDarkTheme = UIUtil.isUnderDarcula()
            val theme = if (isDarkTheme) "dark" else "light"
            LOG.info("Theme changed to: $theme")
            view?.reload(theme) {
                // Reload the content after theme change
                loadContent()
            }
        } catch (e: Exception) {
            LOG.error("Error handling theme change", e)
        }
    }

    override fun getFile(): VirtualFile = file

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent? = view?.component ?: panel

    override fun getName(): String = "ZenUML"

    override fun setState(state: FileEditorState) {
        // Not implemented for now
    }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        // Not implemented for now
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        // Not implemented for now
    }

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        LOG.info("Disposing ZenUML Editor")
        lifetimeDef.terminate()
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? = userDataHolder.getUserData(key)

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = userDataHolder.putUserData(key, value)

    /**
     * Opens the developer tools window for the WebView
     */
    fun openDevTools() {
        view?.openDevTools()
    }
}
