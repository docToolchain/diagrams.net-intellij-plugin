package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.runNonUndoableWriteAction
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class DiagramsEditorProvider : FileEditorProvider, DumbAware {
    /**
     * accept is called whenever IntelliJ opens an editor
     * if accept return true, IntelliJ will open an instance of this editor
     */
    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (file.isDirectory || !file.exists()) {
            return false;
        }
        //check for the right file extension
        val extensions = arrayOf(".drawio", ".drawio.svg", ".dio", ".dio.svg")
        if (extensions.any { ext -> file.name.endsWith(ext)}) {
            return true
        }
        return false
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DiagramsEditor(project, file)

    override fun getEditorTypeId() = "diagrams.net JCEF editor"
    override fun getPolicy() = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}

