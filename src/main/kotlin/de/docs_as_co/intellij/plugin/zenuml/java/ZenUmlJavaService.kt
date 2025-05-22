package de.docs_as_co.intellij.plugin.zenuml.java

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

@Service(Service.Level.PROJECT)
class ZenUmlJavaService(private val project: Project) {

    /**
     * Checks if the cursor is inside a Java method
     */
    fun isInsideMethod(editor: Editor): Boolean {
        val method = findEnclosingMethod(editor)
        return method != null
    }

    /**
     * Finds the method at the current cursor position
     */
    fun findEnclosingMethod(editor: Editor): PsiMethod? {
        val offset = editor.caretModel.offset
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
        
        val element = psiFile.findElementAt(offset) ?: return null
        return PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ZenUmlJavaService = project.service()
    }
} 