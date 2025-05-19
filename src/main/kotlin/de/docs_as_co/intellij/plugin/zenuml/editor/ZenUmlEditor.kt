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
    private val statusLabel = JLabel("Initializing ZenUML Editor...")
    private var documentListenerAdded = false

    init {
        LOG.info("Initializing ZenUML Editor for ${file.path}")

        // Add a status label at the top
        panel.add(statusLabel, BorderLayout.NORTH)

        try {
            // Set up document listener to sync changes from text editor
            setupDocumentListener()
            
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
                statusLabel.text = "ZenUML Editor loaded successfully"

                // Load the initial content
                loadContent()

                // Listen for changes in the WebView content
                view!!.codeContent.advise(lifetime) { newContent ->
                    if (newContent != null) {
                        updateFileContent(newContent)
                    }
                }

                // Listen for initialization
                view!!.initialized().onSuccess {
                    LOG.info("ZenUML WebView initialized successfully")
                    statusLabel.text = "ZenUML Editor ready"
                }.onError { error ->
                    LOG.error("Failed to initialize ZenUML WebView", error)
                    statusLabel.text = "Error: Failed to initialize ZenUML Editor"

                    // Remove the WebView component and show an error message
                    panel.remove(view!!.component)
                    panel.add(JLabel("Error: ${error.message}"), BorderLayout.CENTER)
                    panel.revalidate()
                    panel.repaint()
                }
            } catch (e: Exception) {
                LOG.error("Error creating ZenUmlWebView", e)
                statusLabel.text = "Error: Failed to create ZenUML Editor"
                panel.add(JLabel("Error: ${e.message}"), BorderLayout.CENTER)
            }
        } catch (e: Exception) {
            LOG.error("Error initializing ZenUML Editor", e)
            statusLabel.text = "Error: Failed to initialize ZenUML Editor"
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
                            view?.loadCode(content)
                        } catch (e: Exception) {
                            LOG.error("Error reading file content", e)
                            statusLabel.text = "Error: Failed to read file content"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error loading content", e)
            statusLabel.text = "Error: Failed to load content"
        }
    }

    /**
     * Update the file content when changes are made in the WebView
     * Uses proper threading model to avoid read access exceptions
     */
    private fun updateFileContent(newContent: String) {
        try {
            // Make sure we're on the EDT thread with read access first
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().runReadAction {
                    val document = FileDocumentManager.getInstance().getDocument(file)
                    if (document != null && document.text != newContent) {
                        WriteCommandAction.runWriteCommandAction(project) {
                            document.setText(newContent)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error updating file content", e)
            statusLabel.text = "Error: Failed to update file content"
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
            statusLabel.text = "Error: Failed to update theme"
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

    /**
     * Sets up a document listener to detect changes in the text editor
     * and sync them to the WebView
     */
    private fun setupDocumentListener() {
        try {
            // Get the document associated with our file
            val document = FileDocumentManager.getInstance().getDocument(file)
            if (document != null && !documentListenerAdded) {
                document.addDocumentListener(object : DocumentListener {
                    override fun documentChanged(event: DocumentEvent) {
                        // When document changes in text editor, update the WebView
                        ApplicationManager.getApplication().invokeLater {
                            ApplicationManager.getApplication().runReadAction {
                                val content = document.text
                                view?.updateCode(content)
                            }
                        }
                    }
                })
                documentListenerAdded = true
                LOG.info("Document listener added for ${file.path}")
            }
        } catch (e: Exception) {
            LOG.error("Error setting up document listener", e)
        }
    }
    
    private fun saveFile(content: String) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                try {
                    file.getOutputStream(this).apply {
                        writer().apply {
                            write(content)
                            flush()
                        }
                        flush()
                        close()
                    }
                } catch (e: Exception) {
                    LOG.error("Error saving file content", e)
                    statusLabel.text = "Error: Failed to save file content"
                }
            }
        }
    }
}
