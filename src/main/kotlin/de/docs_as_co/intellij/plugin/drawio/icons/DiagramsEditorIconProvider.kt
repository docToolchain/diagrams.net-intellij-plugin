package de.docs_as_co.intellij.plugin.drawio.icons

import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.docs_as_co.intellij.plugin.drawio.DiagramsFileUtil
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetIcon
import javax.swing.Icon

class DiagramsEditorIconProvider : DumbAware, IconProvider() {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element is PsiFile) {
            if (DiagramsFileUtil.isDiagramsFile(element.virtualFile)) {
                return DiagramsNetIcon.FILE
            }
        }
        return null
    }
}
