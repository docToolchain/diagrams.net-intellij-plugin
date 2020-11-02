package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.runNonUndoableWriteAction
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import java.beans.PropertyChangeListener
import java.util.*
import javax.swing.JComponent


class DiagramsEditor(private val project: Project, private val file: VirtualFile) : FileEditor {
    private val lifetimeDef = LifetimeDefinition()
    private val lifetime = lifetimeDef.lifetime

    override fun getFile() = file

    private val view = DrawioWebView(lifetime)

    init {

        view.initializedPromise.then {
            var payload = ""
            if (file.name.endsWith(".png")) {
                val binaryContent = file.inputStream.readBytes()
                payload = Base64.getEncoder().encodeToString(binaryContent)
            } else {
                payload = file.inputStream.reader().readText()
            }
            System.out.println("=====================")
            System.out.println(payload)
            view.loadXmlLike(payload)
        }

        view.xmlContent.advise(lifetime) { xml ->
            if (xml !== null) {
                val isSVGFile = file.name.endsWith(".svg")
                if ( isSVGFile ) {
                    //ignore the xml payload and ask for an exported svg
                    view.exportSvg().then{ svg : String ->
                        saveFile (svg)
                    }
                } else {
                    saveFile(xml)
                }
            }
        }
    }

    private fun saveFile(data : String) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                file.getOutputStream(this).apply {
                    writer().apply {
                        write(data)
                        flush()
                    }
                    flush()
                    close()
                }
            }
        }

    }
    override fun getComponent(): JComponent {
        return view.component
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return view.component
    }

    override fun getName() = "diagrams.net editor";

    override fun setState(state: FileEditorState) {

    }

    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {

    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun dispose() {
        lifetimeDef.terminate(true)
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        return null
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
    }
}
