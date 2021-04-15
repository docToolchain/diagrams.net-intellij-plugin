package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.docs_as_co.intellij.plugin.drawio.DiagramsFileUtil


class DiagramsEditorProvider : FileEditorProvider, DumbAware {
    /**
     * accept is called whenever IntelliJ opens an editor
     * if accept return true, IntelliJ will open an instance of this editor
     */
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return DiagramsFileUtil.isDiagramsFile(file)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DiagramsEditor(project, file)

    override fun getEditorTypeId() = "diagrams.net JCEF editor"
    override fun getPolicy() = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}

