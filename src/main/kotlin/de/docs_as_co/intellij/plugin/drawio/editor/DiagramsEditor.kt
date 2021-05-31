package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import de.docs_as_co.intellij.plugin.drawio.settings.DiagramsApplicationSettings
import de.docs_as_co.intellij.plugin.drawio.settings.DiagramsUiTheme
import java.beans.PropertyChangeListener
import javax.swing.JComponent


class DiagramsEditor(private val project: Project, private val file: VirtualFile) : FileEditor, EditorColorsListener, DumbAware,
    DiagramsApplicationSettings.SettingsChangedListener {
    private val lifetimeDef = LifetimeDefinition()
    private val lifetime = lifetimeDef.lifetime
    private val userDataHolder = UserDataHolderBase()

    override fun getFile() = file

    private var view :DiagramsWebView

    init {

        //subscribe to changes of the theme
        val settingsConnection = ApplicationManager.getApplication().messageBus.connect(this)
        settingsConnection.subscribe(EditorColorsManager.TOPIC, this)
        settingsConnection.subscribe(DiagramsApplicationSettings.SettingsChangedListener.TOPIC, this)

        view = DiagramsWebView(lifetime, uiThemeFromConfig().key)
        initView()
    }

    private fun uiThemeFromConfig(): DiagramsUiTheme {
        var uiTheme = DiagramsApplicationSettings.instance.getDiagramsSettings().uiTheme

        if (uiTheme == DiagramsUiTheme.DEFAULT) {
            //set theme according to IntelliJ-theme
            if (UIUtil.isUnderDarcula()) {
                uiTheme = DiagramsUiTheme.DARK
            } else {
                uiTheme = DiagramsUiTheme.KENNEDY
            }
        }
        return uiTheme
    }

    private fun initView() {
        view.initialized().then {
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
                    view.exportSvg().then{ data: String ->
                        saveFile(data.toByteArray(charset("utf-8")))
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

    @Override
    override fun globalSchemeChange(scheme: EditorColorsScheme?) {
        view.reload(uiThemeFromConfig().key) {
            initView()
        }
    }

    override fun onSettingsChange(settings: DiagramsApplicationSettings) {
        view.reload(uiThemeFromConfig().key) {
            initView()
        }
    }

    private fun saveFile(data: ByteArray) {
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
