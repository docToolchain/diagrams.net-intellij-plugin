package de.docs_as_co.intellij.plugin.zenuml.java

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import java.util.*

/**
 * Helper class to maintain a stack of visited methods to detect recursion
 */
class MethodStack {
    private val methods = Stack<PsiMethod>()

    fun push(method: PsiMethod) {
        methods.push(method)
    }

    fun pop(): PsiMethod? {
        return if (methods.isEmpty()) null else methods.pop()
    }

    fun contains(method: PsiMethod): Boolean {
        return methods.contains(method)
    }

    fun peekContainingClass(): Optional<PsiClass> {
        return if (methods.isEmpty()) {
            Optional.empty()
        } else {
            Optional.ofNullable(methods.peek().containingClass)
        }
    }
} 