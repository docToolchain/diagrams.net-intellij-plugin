package de.docs_as_co.intellij.plugin.drawio.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditor
import de.docs_as_co.intellij.plugin.drawio.editor.DiagramsEditor

class OpenDevtoolsAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val fileEditor: FileEditor? = event.getData(PlatformDataKeys.FILE_EDITOR)
        if (fileEditor is DiagramsEditor) {
            fileEditor.openDevTools();
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(event: AnActionEvent) {
        var visible = false
        val fileEditor: FileEditor? = event.getData(PlatformDataKeys.FILE_EDITOR)
        if (fileEditor is DiagramsEditor) {
            visible = true
        }
        event.presentation.isVisible = visible
    }
}
