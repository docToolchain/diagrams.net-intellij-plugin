package de.docs_as_co.intellij.plugin.zenuml.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditor
import de.docs_as_co.intellij.plugin.zenuml.editor.ZenUmlEditor

/**
 * Action for opening developer tools in the ZenUML editor.
 * This allows debugging the WebView component of the ZenUML editor.
 */
class OpenZenUmlDevtoolsAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val fileEditor: FileEditor? = event.getData(PlatformDataKeys.FILE_EDITOR)
        if (fileEditor is ZenUmlEditor) {
            fileEditor.openDevTools()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(event: AnActionEvent) {
        var visible = false
        val fileEditor: FileEditor? = event.getData(PlatformDataKeys.FILE_EDITOR)
        if (fileEditor is ZenUmlEditor) {
            visible = true
        }
        event.presentation.isVisible = visible
    }
}
