package de.docs_as_co.intellij.plugin.drawio

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.DiagramsNetBundle"

object DiagramsNetBundle : AbstractBundle(BUNDLE) {

    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

    @Suppress("SpreadOperator")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = run {
        message(key, *params)
    }
}
