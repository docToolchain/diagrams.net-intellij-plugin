package de.docs_as_co.intellij.plugin.zenuml.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import de.docs_as_co.intellij.plugin.zenuml.ZenUmlIcons
import de.docs_as_co.intellij.plugin.zenuml.editor.ZenUmlFileCreator
import de.docs_as_co.intellij.plugin.zenuml.java.PsiToDslConverter
import de.docs_as_co.intellij.plugin.zenuml.java.ZenUmlJavaService

class ShowZenUmlFromJavaAction : AnAction() {

    init {
        templatePresentation.icon = ZenUmlIcons.SEQUENCE_ICON
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        val psiMethod = ZenUmlJavaService.getInstance(project).findEnclosingMethod(editor) ?: return
        
        val dsl = generateZenUML(psiMethod)
        val content = """
            // Generated from Java method: ${psiMethod.containingClass?.qualifiedName}.${psiMethod.name}
            // This is an experimental feature. Please report issues to https://github.com/ZenUml/jetbrains-zenuml/discussions
            
            ${dsl}
        """.trimIndent()
        
        // Create and open a new ZenUML file
        val fileName = "${psiMethod.containingClass?.name ?: "Unknown"}_${psiMethod.name}.zenuml"
        val file = ZenUmlFileCreator.createZenUmlFile(project, fileName, content)
        
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true)
        } else {
            Messages.showErrorDialog(
                project,
                "Failed to create ZenUML file",
                "ZenUML Generation Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        
        if (project == null || editor == null) {
            e.presentation.isEnabled = false
            return
        }
        
        // Only enable if we're in a Java method
        e.presentation.isEnabled = ZenUmlJavaService.getInstance(project).isInsideMethod(editor)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    private fun generateZenUML(psiMethod: PsiMethod): String {
        val converter = PsiToDslConverter()
        converter.visitMethod(psiMethod)
        return converter.getDsl()
    }
} 