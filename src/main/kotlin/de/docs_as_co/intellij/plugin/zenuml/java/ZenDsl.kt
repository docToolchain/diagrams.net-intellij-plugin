package de.docs_as_co.intellij.plugin.zenuml.java

/**
 * Helper class to build ZenUML DSL string
 */
class ZenDsl {
    private val stringBuilder = StringBuilder()
    private var indentationLevel = 0
    private val tabSize = 2

    fun append(str: String): ZenDsl {
        ensureIndent()
        if (str.isNotBlank()) {
            stringBuilder.append(str)
        }
        return this
    }

    fun appendParams(params: String): ZenDsl {
        stringBuilder.append("(").append(params).append(")")
        return openBlock()
    }

    fun appendParticipant(name: String): ZenDsl {
        ensureIndent()
        stringBuilder.append(name).append(".")
        return this
    }

    fun appendAssignment(type: String, name: String): ZenDsl {
        ensureIndent()
        stringBuilder.append(type).append(" ").append(name).append(" = ")
        return this
    }
    
    fun appendMethodCall(methodName: String, args: String): ZenDsl {
        ensureIndent()
        stringBuilder.append(methodName).append("(").append(args).append(")")
        return this
    }

    fun comment(text: String): ZenDsl {
        ensureIndent()
        text.split("\n").forEach { line ->
            stringBuilder.append("// ").append(line)
            newLine()
        }
        return this
    }

    fun openParenthesis(): ZenDsl {
        stringBuilder.append("(")
        return this
    }

    fun closeParenthesis(): ZenDsl {
        stringBuilder.append(")")
        return this
    }

    fun closeExpressionAndNewLine(): ZenDsl {
        stringBuilder.append("")
        newLine()
        return this
    }

    fun openBlock(): ZenDsl {
        stringBuilder.append(" {")
        newLine()
        indentationLevel++
        return this
    }

    fun closeBlock(): ZenDsl {
        indentationLevel--
        ensureIndent()
        stringBuilder.append("}")
        newLine()
        return this
    }

    private fun newLine(): ZenDsl {
        stringBuilder.append("\n")
        return this
    }

    private fun ensureIndent(): ZenDsl {
        if (stringBuilder.isNotEmpty() && stringBuilder.last() == '\n') {
            val indentation = " ".repeat(indentationLevel * tabSize)
            stringBuilder.append(indentation)
        }
        return this
    }

    fun getDsl(): String {
        if (indentationLevel > 0) {
            while (indentationLevel > 0) {
                closeBlock()
            }
        }
        return stringBuilder.toString()
    }
} 