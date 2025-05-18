package de.docs_as_co.intellij.plugin.zenuml.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.BorderLayout

/**
 * Editor for ZenUML files.
 * This is a placeholder implementation that will be expanded in later steps.
 */
class ZenUmlEditor(private val project: Project, private val file: VirtualFile) : FileEditor, DumbAware {
    private val userDataHolder = UserDataHolderBase()
    private val panel = JPanel(BorderLayout())

    init {
        // For now, just display a placeholder message
        panel.add(JLabel("ZenUML Editor - Coming Soon"), BorderLayout.CENTER)
    }

    override fun getFile(): VirtualFile = file

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent? = panel

    override fun getName(): String = "ZenUML"

    override fun setState(state: FileEditorState) {
        // Will be implemented later
    }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        // Will be implemented later
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        // Will be implemented later
    }

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        // Will be implemented later
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? = userDataHolder.getUserData(key)

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = userDataHolder.putUserData(key, value)
}
