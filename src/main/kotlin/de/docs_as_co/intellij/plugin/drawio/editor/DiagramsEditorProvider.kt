package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.docs_as_co.intellij.plugin.drawio.DiagramsFileUtil
import java.io.IOException
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.util.regex.Pattern


class DiagramsEditorProvider : FileEditorProvider, DumbAware {

    val LOG: Logger = Logger.getInstance(DiagramsEditorProvider::class.java)

    /**
     * accept is called whenever IntelliJ opens an editor
     * if accept return true, IntelliJ will open an instance of this editor
     */
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return DiagramsFileUtil.isDiagramsFile(file)
    }

    init {
        // Workaround for https://youtrack.jetbrains.com/issue/IJPL-148653,
        // when a lock file remains which the name of the old host name
        val jcefCache = PathManager.getSystemDir().resolve("jcef_cache")
        if (jcefCache.toFile().exists()) {
            val singletonLock = jcefCache.resolve("SingletonLock")
            try {
                val hostName = InetAddress.getLocalHost().hostName
                val lockFile = Files.readSymbolicLink(singletonLock).toFile().name
                if (!lockFile.matches((Pattern.quote(hostName) + "-[0-9]*").toRegex())) {
                    // delete the stale lock to prevent the preview from appearing blank
                    Files.delete(singletonLock)
                }
            } catch (ignore: NoSuchFileException) {
                // nop
            } catch (e: IOException) {
                LOG.warn("Can't check lock", e)
            }
        }
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DiagramsEditor(project, file)

    override fun getEditorTypeId() = "diagrams.net JCEF editor"
    override fun getPolicy() = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}

