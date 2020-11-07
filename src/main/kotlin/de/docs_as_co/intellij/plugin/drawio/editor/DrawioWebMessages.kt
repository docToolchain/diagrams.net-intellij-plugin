package de.docs_as_co.intellij.plugin.drawio.editor

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode

sealed class OutgoingMessage {
    sealed class Request : OutgoingMessage() {
        /**
         * Is used to map responses to requests.
         * Must be unique.
         */
        var requestId: String? = null

        data class Export(val format: String) : Request() {
            companion object {
                val HTML = "html"
                val XMLPNG = "xmlpng"
                val PNG = "png"
                val XML = "xml"
                val XMLSVG = "xmlsvg"
            }

            val action = "export"
        }
    }

    sealed class Event : OutgoingMessage() {
        data class Merge(val xml: String) : Event() {
            val action = "merge"
        }

        data class Configure(val config: DrawioConfig) : Event() {
            val action = "configure"
        }

        data class Load(val xml: String, val autosave: Int) : Event() {
            val action = "load"
        }
    }
}

data class DrawioConfig(val compressXml: Boolean?)

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "event"
)
@JsonSubTypes(
        JsonSubTypes.Type(value = IncomingMessage.Response.Export::class, name = "export"),
        JsonSubTypes.Type(value = IncomingMessage.Response.Ack::class, name = "response"),
        JsonSubTypes.Type(value = IncomingMessage.Event.Load::class, name = "load"),
        JsonSubTypes.Type(value = IncomingMessage.Event.Initialized::class, name = "init"),
        JsonSubTypes.Type(value = IncomingMessage.Event.Save::class, name = "save"),
        JsonSubTypes.Type(value = IncomingMessage.Event.AutoSave::class, name = "autosave"),
        JsonSubTypes.Type(value = IncomingMessage.Event.Configure::class, name = "configure")
)
sealed class IncomingMessage {
    sealed class Response : IncomingMessage() {
        /**
         * Matches the request id of the request.
         */
        abstract val requestId: String

        data class Export(val data: String, val format: String, val xml: String, val message: MessageWithRequestId) :
                Response() {
            override val requestId: String
                get() = message.requestId
        }

        data class Ack(override val requestId: String) : Response()
    }

    sealed class Event : IncomingMessage() {
        object Initialized : Event()
        data class AutoSave(val xml: String) : Event()
        data class Save(val xml: String) : Event()
        object Configure : Event()

        object Load : Event()
    }
}

data class MessageWithRequestId(val requestId: String)
