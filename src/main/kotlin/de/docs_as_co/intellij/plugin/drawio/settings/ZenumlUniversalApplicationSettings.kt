package de.docs_as_co.intellij.plugin.drawio.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Property

/**
 * Application level settings for the Diagrams.net plugin.
 */
@State(name = "DiagramsApplicationSettings", storages = [Storage("diagramsNet.xml")])
class ZenumlUniversalApplicationSettings : PersistentStateComponent<ZenumlUniversalApplicationSettings.State?>, DiagramsSettings.Holder {
    private val myState = State()
    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    override fun setDiagramsPreviewSettings(settings: DiagramsSettings) {
        myState.myPreviewSettings = settings

        ApplicationManager.getApplication().messageBus.syncPublisher(SettingsChangedListener.TOPIC)
            .onSettingsChange(this)
    }

    override fun getDiagramsSettings(): DiagramsSettings {
        return myState.myPreviewSettings
    }

    class State {
        @Property(surroundWithTag = false)
        var myPreviewSettings = DiagramsSettings.DEFAULT
    }

    companion object {
        val instance: ZenumlUniversalApplicationSettings
            get() = ServiceManager.getService(ZenumlUniversalApplicationSettings::class.java)
    }

    interface SettingsChangedListener {
        fun onSettingsChange(settings: ZenumlUniversalApplicationSettings)

        companion object {
            val TOPIC = Topic.create(
                "DiagramsApplicationSettingsChanged",
                SettingsChangedListener::class.java
            )
        }
    }
}
