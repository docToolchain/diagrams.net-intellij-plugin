package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import java.beans.PropertyChangeListener
import javax.swing.JComponent


class DiagramsEditor(private val project: Project, private val file: VirtualFile) : FileEditor, DumbAware {
    private val lifetimeDef = LifetimeDefinition()
    private val lifetime = lifetimeDef.lifetime
    private val userDataHolder = UserDataHolderBase()

    override fun getFile() = file

    private val view = DrawioWebView(lifetime)

    init {

        view.initializedPromise.then {
            if (file.name.endsWith(".png")) {
                val payload = file.inputStream.readBytes()
                view.loadPng(payload)
            } else {
                val payload = file.inputStream.reader().readText()
                view.loadXmlLike(payload)
            }
        }

        view.xmlContent.advise(lifetime) { xml ->
            if (xml !== null) {
                val isSVGFile = file.name.endsWith(".svg")
                val isPNGFile = file.name.endsWith(".png")
                if ( isSVGFile ) {
                    //ignore the xml payload and ask for an exported svg
                    view.exportSvg().then{ data : String ->
                        saveFile (data.toByteArray(charset("utf-8")))
                    }
                } else if ( isPNGFile ) {
                    //ignore the xml payload and ask for an exported svg
                    view.exportPng().then { data: ByteArray ->
                        saveFile(data)
                    }
                } else {
                    saveFile(xml.toByteArray(charset("utf-8")))
                }
            }
        }
    }

    private fun saveFile(data : ByteArray) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                file.getOutputStream(this).apply {
                    writer().apply {
                        //svg and png are returned base64 encoded
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

    override fun getPreferredFocusedComponent(): JComponent {
        return view.component
    }

    override fun getName() = "diagrams.net editor"

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
        return userDataHolder.getUserData(key)
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        userDataHolder.putUserData(key, value)
    }
}
