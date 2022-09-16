package de.docs_as_co.intellij.plugin.drawio

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader

object DiagramsNet : Language("ZenUML")

object DiagramsNetFileType : LanguageFileType(DiagramsNet) {
    override fun getName() = "ZenUML Diagram"
    override fun getDescription() = "ZenUML diagram file"
    override fun getDefaultExtension() = "zen"
    override fun getIcon() = DiagramsNetIcon.FILE
}

object DiagramsNetIcon {
    val FILE = IconLoader.getIcon("/icons/zenuml-file.svg", this.javaClass)
}
