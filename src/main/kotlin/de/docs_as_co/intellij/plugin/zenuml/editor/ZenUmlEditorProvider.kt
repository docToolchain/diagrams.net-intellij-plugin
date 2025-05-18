package de.docs_as_co.intellij.plugin.zenuml.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.docs_as_co.intellij.plugin.zenuml.file.ZenUmlFileType

class ZenUmlEditorProvider : FileEditorProvider, DumbAware {

    private val LOG: Logger = Logger.getInstance(ZenUmlEditorProvider::class.java)

    /**
     * accept is called whenever IntelliJ opens an editor
     * if accept return true, IntelliJ will open an instance of this editor
     */
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return isZenUmlFile(file)
    }

    /**
     * Check if the file is a ZenUML file based on its extension
     */
    private fun isZenUmlFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase() ?: return false
        return ZenUmlFileType.EXTENSIONS.contains(extension)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        // We'll implement the ZenUmlEditor in the next step
        // For now, we'll return a placeholder that will be replaced later
        return ZenUmlEditor(project, file)
    }

    override fun getEditorTypeId() = "ZenUML Editor"

    override fun getPolicy() = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
