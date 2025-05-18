package de.docs_as_co.intellij.plugin.zenuml.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import de.docs_as_co.intellij.plugin.zenuml.ZenUmlIcons

@Suppress("DialogTitleCapitalization")
class ZenUmlCreateFileAction : CreateFileFromTemplateAction("ZenUML Diagram", "Create a new ZenUML diagram", ZenUmlIcons.ZENUML_ICON), DumbAware {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New ZenUML Diagram")
            .addKind("ZenUML Diagram", ZenUmlIcons.ZENUML_ICON, "ZenUML Diagram")
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String {
        return "Create ZenUML Diagram"
    }
}
