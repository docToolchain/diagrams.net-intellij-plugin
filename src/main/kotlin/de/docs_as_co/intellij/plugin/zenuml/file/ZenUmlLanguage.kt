package de.docs_as_co.intellij.plugin.zenuml.file

import com.intellij.lang.Language

object ZenUmlLanguage : Language("ZenUML") {
    override fun getDisplayName(): String = "ZenUML"
    override fun isCaseSensitive(): Boolean = true
}
