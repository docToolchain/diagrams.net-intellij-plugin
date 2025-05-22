package de.docs_as_co.intellij.plugin.zenuml.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File

/**
 * Utility class for creating ZenUML files
 */
object ZenUmlFileCreator {
    
    /**
     * Creates a new ZenUML file in the project's zenuml directory.
     * If the directory doesn't exist, it creates it.
     *
     * @param project The current project
     * @param fileName The name of the file to create
     * @param content The ZenUML content to write to the file
     * @return The VirtualFile if created successfully, null otherwise
     */
    fun createZenUmlFile(project: Project, fileName: String, content: String): VirtualFile? {
        return ApplicationManager.getApplication().runWriteAction(Computable {
            try {
                // Get project's base directory
                val projectBasePath = project.basePath
                if (projectBasePath == null) {
                    return@Computable null
                }
                
                // Create a zenuml directory in the project root if it doesn't exist
                val zenumlDirFile = File(projectBasePath, "zenuml")
                if (!zenumlDirFile.exists()) {
                    zenumlDirFile.mkdirs()
                }
                
                // Create the zenuml directory VirtualFile
                val zenumlDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(zenumlDirFile)
                    ?: return@Computable null
                
                // Check if file already exists, if so, add a number suffix
                var newFileName = fileName
                var fileCounter = 1
                while (zenumlDir.findChild(newFileName) != null) {
                    val baseName = fileName.substringBeforeLast(".")
                    val extension = if (fileName.contains(".")) "." + fileName.substringAfterLast(".") else ""
                    newFileName = "${baseName}_${fileCounter}${extension}"
                    fileCounter++
                }
                
                // Create the file and write content
                val file = zenumlDir.createChildData(this, newFileName)
                VfsUtil.saveText(file, content)
                
                return@Computable file
            } catch (e: Exception) {
                e.printStackTrace()
                return@Computable null
            }
        })
    }
} 