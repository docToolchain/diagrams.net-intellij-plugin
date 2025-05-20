package de.docs_as_co.intellij.plugin.zenuml.editor

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

sealed class OutgoingMessage {
    sealed class Request : OutgoingMessage() {
        /**
         * Is used to map responses to requests.
         * Must be unique.
         */
        var requestId: String? = null

        data class Export(val format: String) : Request() {
            companion object {
                val SVG = "svg"
                val PNG = "png"
            }

            val action = "export"
        }
    }

    sealed class Event : OutgoingMessage() {
        data class Load(val code: String) : Event() {
            val action = "load"
        }

        data class Update(val code: String) : Event() {
            val action = "update"
        }
    }
}

data class ZenUmlConfig(val theme: String)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "event"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = IncomingMessage.Response.Export::class, name = "export"),
    JsonSubTypes.Type(value = IncomingMessage.Response.Ack::class, name = "response"),
    JsonSubTypes.Type(value = IncomingMessage.Event.Initialized::class, name = "init"),
    JsonSubTypes.Type(value = IncomingMessage.Event.ContentChanged::class, name = "contentChanged"),
    JsonSubTypes.Type(value = IncomingMessage.Event.Ready::class, name = "ready")
)
sealed class IncomingMessage {
    sealed class Response : IncomingMessage() {
        /**
         * Matches the request id of the request.
         */
        abstract val requestId: String

        data class Export(val data: String, val format: String, val message: MessageWithRequestId) :
            Response() {
            override val requestId: String
                get() = message.requestId
        }

        data class Ack(override val requestId: String) : Response()
    }

    sealed class Event : IncomingMessage() {
        object Initialized : Event()
        data class ContentChanged(val code: String) : Event()
        data class Ready(val message: String? = null) : Event()
    }
}

data class MessageWithRequestId(val requestId: String)
