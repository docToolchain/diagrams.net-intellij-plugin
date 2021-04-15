package de.docs_as_co.intellij.plugin.drawio.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetIcon

@Suppress("DialogTitleCapitalization")
class DiagramCreateFileAction : CreateFileFromTemplateAction("diagrams.net File", "Create new diagrams.net file", DiagramsNetIcon.FILE), DumbAware {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
                .setTitle("New diagrams.net File") // add templates to filer src/main/resources/fileTemplates.internal
                .addKind("Editable SVG file", DiagramsNetIcon.FILE, "editable-svg")
                .addKind("Editable PNG file", DiagramsNetIcon.FILE, "editable-png")
                .addKind("XML file", DiagramsNetIcon.FILE, "xml")
                .setValidator(NonEmptyInputValidator())
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "diagrams.net File"
    }

    override fun createFile(name: String, templateName: String, directory: PsiDirectory): PsiFile? {
        if (templateName == "editable-png") {
            // PNG is a binary file, use extra logic to create binary file from template
            // inspired by: DefaultCreateFromTemplateHandler#createFromTemplate
            val fileName = checkAppendExtension(name, "png")

            if (FileTypeManager.getInstance().isFileIgnored(fileName)) {
                throw IncorrectOperationException("This filename is ignored (Settings | Editor | File Types | Ignore files and folders)")
            }

            val file = directory.createFile(fileName)
            file.virtualFile.getOutputStream(null).use { os ->
                val templateFile = "/fileTemplates/internal/$templateName.png.bin"
                this.javaClass.getResourceAsStream(templateFile).use {
                    if (it == null) {
                        throw IllegalArgumentException("file not found: $templateFile")
                    }
                    it.transferTo(os)
                }
            }
            return file
        } else {
            return super.createFile(name, templateName, directory)
        }
    }

    protected fun checkAppendExtension(fileName: String, extension: String): String {
        var result = fileName
        val suggestedFileNameEnd = ".$extension"
        if (!result.endsWith(suggestedFileNameEnd)) {
            result += suggestedFileNameEnd
        }
        return result
    }

}
