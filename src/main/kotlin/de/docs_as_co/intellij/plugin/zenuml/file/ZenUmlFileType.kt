package de.docs_as_co.intellij.plugin.zenuml.file

import com.intellij.openapi.fileTypes.LanguageFileType
import de.docs_as_co.intellij.plugin.zenuml.ZenUmlIcons
import javax.swing.Icon

object ZenUmlFileType : LanguageFileType(ZenUmlLanguage) {
    override fun getName(): String = "ZenUML"
    override fun getDescription(): String = "ZenUML sequence diagram file"
    override fun getDefaultExtension(): String = "zenuml"
    override fun getIcon(): Icon = ZenUmlIcons.ZENUML_ICON

    // Additional extensions
    val EXTENSIONS = arrayOf("zenuml", "zen", "z")
}
