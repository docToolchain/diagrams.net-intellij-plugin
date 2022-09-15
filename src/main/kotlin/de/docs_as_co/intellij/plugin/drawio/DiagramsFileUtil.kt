package de.docs_as_co.intellij.plugin.drawio

import com.intellij.openapi.vfs.VirtualFile

class DiagramsFileUtil {
    companion object {
        fun isDiagramsFile(file: VirtualFile?): Boolean {
            if (file == null) {
                return false
            }

            if (file.isDirectory || !file.exists()) {
                return false
            }
            //check for the right file extension
            val extensions = arrayOf(".zen", ".z", ".zenuml")
            // Short-circuit for well-known file names. Allows to start with an empty file and open it in the editor.
            if (extensions.any { ext -> file.name.endsWith(ext) }) {
                return true
            }

            return false
        }
    }
}
