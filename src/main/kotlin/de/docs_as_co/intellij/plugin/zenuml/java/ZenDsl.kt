package de.docs_as_co.intellij.plugin.zenuml.java

/**
 * Helper class to build ZenUML DSL string
 */
class ZenDsl {
    private val stringBuilder = StringBuilder()
    private var blockEntered = false

    fun append(str: String): ZenDsl {
        if (str.isNotBlank()) {
            stringBuilder.append(str)
        }
        return this
    }

    fun appendParams(params: String): ZenDsl {
        stringBuilder.append("(").append(params).append(")")
        stringBuilder.append(" {\n")
        blockEntered = true
        return this
    }

    fun appendParticipant(name: String): ZenDsl {
        stringBuilder.append(name).append(".")
        return this
    }

    fun appendAssignment(type: String, name: String): ZenDsl {
        stringBuilder.append(type).append(" ").append(name).append(" = ")
        return this
    }

    fun comment(text: String): ZenDsl {
        stringBuilder.append("// ").append(text).append("\n")
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
        if (blockEntered) {
            stringBuilder.append("\n")
        }
        return this
    }

    fun openBlock(): ZenDsl {
        stringBuilder.append(" {\n")
        blockEntered = true
        return this
    }

    fun closeBlock(): ZenDsl {
        stringBuilder.append("}\n")
        blockEntered = false
        return this
    }

    fun getDsl(): String {
        if (blockEntered) {
            closeBlock()
        }
        return stringBuilder.toString()
    }
} 