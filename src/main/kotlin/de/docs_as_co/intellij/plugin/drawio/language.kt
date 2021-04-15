package de.docs_as_co.intellij.plugin.drawio

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader

object DiagramsNet : Language("Diagrams.net")

object DiagramsNetFileType : LanguageFileType(DiagramsNet) {
    override fun getName() = "Diagrams.net Diagram"
    override fun getDescription() = "Diagrams.net Diagram File"
    override fun getDefaultExtension() = "drawio"
    override fun getIcon() = DiagramsNetIcon.FILE
}

object DiagramsNetIcon {
    val FILE = IconLoader.getIcon("/icons/diagrams.svg", this.javaClass)
}
