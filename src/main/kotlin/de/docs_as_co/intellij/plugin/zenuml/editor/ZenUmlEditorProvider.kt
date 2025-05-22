package de.docs_as_co.intellij.plugin.zenuml.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.docs_as_co.intellij.plugin.zenuml.file.ZenUmlFileType

/**
 * Provider for ZenUML Editor.
 * This class is responsible for creating ZenUML editors for ZenUML files.
 */
class ZenUmlEditorProvider : FileEditorProvider, DumbAware {

    private val LOG: Logger = Logger.getInstance(ZenUmlEditorProvider::class.java)

    /**
     * accept is called whenever IntelliJ opens an editor
     * if accept return true, IntelliJ will open an instance of this editor
     */
    override fun accept(project: Project, file: VirtualFile): Boolean {
        LOG.info("ZenUmlEditorProvider.accept: ${file.path}, extension: ${file.extension}")
        return isZenUmlFile(file)
    }

    /**
     * Check if the file is a ZenUML file based on its extension
     */
    private fun isZenUmlFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase() ?: return false
        val result = extension in ZenUmlFileType.EXTENSIONS
        LOG.info("isZenUmlFile: $result for extension: $extension")
        return result
    }

    /**
     * Create a new ZenUML editor for the given file
     */
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        LOG.info("Creating ZenUML editor for ${file.path}")
        return ZenUmlEditor(project, file)
    }

    /**
     * Get the ID of this editor type
     */
    override fun getEditorTypeId(): String = "ZenUML Editor"
    
    /**
     * Define the policy for this editor
     * PLACE_BEFORE_DEFAULT_EDITOR means our editor will be shown before the default text editor
     */
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
