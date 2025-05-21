package de.docs_as_co.intellij.plugin.zenuml.java

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Converts Java PSI elements to ZenUML DSL
 */
class PsiToDslConverter : JavaRecursiveElementVisitor() {
    private val LOG = Logger.getInstance(PsiToDslConverter::class.java)
    private val methodStack = MethodStack()
    private val zenDsl = ZenDsl()
    
    companion object {
        private const val TYPE_PARAMETER_PATTERN = "<[^<>]*>"
        private const val ARRAY_PATTERN = "\\[\\d*\\]"
    }

    override fun visitMethod(method: PsiMethod) {
        LOG.debug("Enter: visitMethod: ${method}")
        appendMethod(method)
        processChildren(method)
        LOG.debug("Exit: visitMethod: ${method}")
    }

    private fun appendMethod(method: PsiMethod) {
        method.containingClass?.let { appendParticipant(it) }
        zenDsl.append(method.name)
        appendParameters(method.parameterList.parameters)
    }

    private fun appendParameters(parameters: Array<PsiParameter>) {
        val parameterNames = Stream.of(*parameters)
            .map { it.name }
            .collect(Collectors.joining(", "))
        
        zenDsl.appendParams(parameterNames)
    }

    private fun appendParticipant(containingClass: PsiClass) {
        val headClass = methodStack.peekContainingClass()
        
        if (headClass.isPresent && containingClass == headClass.get()) {
            return
        }
        
        zenDsl.appendParticipant(containingClass.name ?: "Anonymous")
    }

    private fun processChildren(method: PsiMethod) {
        if (isExternalMethod(method)) {
            zenDsl.closeExpressionAndNewLine()
            return
        }

        if (detectReEntry(method)) return
        
        methodStack.push(method)
        super.visitMethod(method)
        methodStack.pop()
    }

    private fun isExternalMethod(method: PsiMethod): Boolean {
        return false // We can implement this later if needed
    }

    private fun detectReEntry(method: PsiMethod): Boolean {
        if (methodStack.contains(method)) {
            LOG.debug("Exit (loop detected): visitMethod: ${method}")
            zenDsl.comment("Method re-entered")
            return true
        }
        return false
    }

    override fun visitLocalVariable(variable: PsiLocalVariable) {
        LOG.debug("Enter: visitLocalVariable: ${variable}")
        if (isWithinForStatement(variable)) return

        if (variable.hasInitializer() && variable.initializer !is PsiArrayInitializerExpression) {
            zenDsl.appendAssignment(
                replaceArray(withoutTypeParameter(variable.typeElement?.text ?: "")), 
                variable.name ?: ""
            )
        } else {
            zenDsl.comment(replaceArray(variable.text ?: ""))
        }
        
        super.visitLocalVariable(variable)
        LOG.debug("Exit: visitLocalVariable: ${variable}")
    }

    private fun withoutTypeParameter(text: String): String {
        return text.replace(Regex(TYPE_PARAMETER_PATTERN), "")
    }

    private fun replaceArray(text: String): String {
        return text.replace(Regex(ARRAY_PATTERN), "_array")
    }

    private fun isWithinForStatement(element: PsiElement?): Boolean {
        if (element == null) return false
        return element.parent is PsiForStatement || isWithinForStatement(element.parent)
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        LOG.debug("Enter: visitMethodCallExpression: ${expression}")
        zenDsl.append(expression.methodExpression.text)
            .openParenthesis()
            .append(getArgs(expression.argumentList))
            .closeParenthesis()
        
        super.visitMethodCallExpression(expression)
    }

    override fun visitNewExpression(expression: PsiNewExpression) {
        LOG.debug("Enter: visitNewExpression: ${expression}")
        zenDsl
            .append("new ")
            .append(expression.classReference?.referenceName ?: "")
            .openParenthesis()
            .append(getArgs(expression.argumentList))
            .closeParenthesis()
        
        super.visitNewExpression(expression)
    }

    override fun visitCallExpression(callExpression: PsiCallExpression) {
        val method = callExpression.resolveMethod()
        if (method != null) {
            LOG.debug("Method resolved from expression: ${method}")
            processChildren(method)
        } else {
            LOG.debug("Method not resolved from expression, appending the expression directly")
            zenDsl.closeExpressionAndNewLine()
        }
    }

    private fun getArgs(argumentList: PsiExpressionList?): String {
        if (argumentList == null) return ""

        return Arrays.stream(argumentList.expressions)
            .map { e ->
                when (e) {
                    is PsiLambdaExpression -> "lambda"
                    is PsiNewExpression -> {
                        "new ${withoutTypeParameter(e.type?.presentableText ?: "")}()"
                    }
                    else -> withoutTypeParameter(e.text ?: "")
                }
            }
            .collect(Collectors.joining(", "))
    }

    override fun visitWhileStatement(statement: PsiWhileStatement) {
        LOG.debug("Enter: visitWhileStatement: ${statement}")
        zenDsl.append("while")
            .openParenthesis()
            .append(statement.condition?.text ?: "")
            .closeParenthesis()
            .openBlock()
        
        statement.body?.accept(this)
        
        zenDsl.closeBlock()
    }

    override fun visitForStatement(statement: PsiForStatement) {
        zenDsl.append("for")
            .openParenthesis()
            .append(statement.condition?.text ?: "")
            .closeParenthesis()
            .openBlock()
        
        statement.body?.accept(this)
        
        zenDsl.closeBlock()
    }

    override fun visitForeachStatement(statement: PsiForeachStatement) {
        zenDsl.append("for")
            .openParenthesis()
            .append("${statement.iterationParameter.name} : ${statement.iteratedValue?.text}")
            .closeParenthesis()
            .openBlock()
        
        statement.body?.accept(this)
        
        zenDsl.closeBlock()
    }

    override fun visitIfStatement(statement: PsiIfStatement) {
        LOG.debug("Enter: visitIfStatement: ${statement}")
        zenDsl.append("if")
            .openParenthesis()
            .append(statement.condition?.text ?: "")
            .closeParenthesis()
            .openBlock()
        
        statement.thenBranch?.accept(this)
        
        zenDsl.closeBlock()
        
        statement.elseBranch?.let {
            zenDsl.append("else")
                .openBlock()
            
            it.accept(this)
            
            zenDsl.closeBlock()
        }
    }

    override fun visitReturnStatement(statement: PsiReturnStatement) {
        zenDsl.append("return")
        statement.returnValue?.let {
            zenDsl.append(" ${it.text}")
        }
        zenDsl.closeExpressionAndNewLine()
        
        super.visitReturnStatement(statement)
    }

    override fun visitTryStatement(statement: PsiTryStatement) {
        zenDsl.append("try")
            .openBlock()
        
        statement.tryBlock?.accept(this)
        
        zenDsl.closeBlock()
        
        statement.catchSections.forEach { it.accept(this) }
        
        statement.finallyBlock?.let {
            zenDsl.append("finally")
                .openBlock()
            
            it.accept(this)
            
            zenDsl.closeBlock()
        }
    }

    override fun visitCatchSection(section: PsiCatchSection) {
        zenDsl.append("catch")
            .openParenthesis()
            .append(section.parameter?.text ?: "")
            .closeParenthesis()
            .openBlock()
        
        section.catchBlock?.accept(this)
        
        zenDsl.closeBlock()
    }

    fun getDsl(): String {
        return zenDsl.getDsl()
    }
} 