package de.docs_as_co.intellij.plugin.zenuml.editor

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.util.*

/**
 * WebView for ZenUML diagrams.
 * This class handles the communication between the editor and the web view component.
 */
class ZenUmlWebView(lifetime: Lifetime, theme: String) : BaseZenUmlWebView(lifetime, theme) {
    private var _initializedPromise = AsyncPromise<Unit>()

    // hide the internal promise type from the outside
    fun initialized(): Promise<Unit> {
        return _initializedPromise
    }

    private val _codeContent = Property<String?>(null)
    val codeContent: IPropertyView<String?> = _codeContent

    override fun handleEvent(event: IncomingMessage.Event) {
        when (event) {
            is IncomingMessage.Event.Initialized -> {
                _initializedPromise.setResult(Unit)
            }
            is IncomingMessage.Event.ContentChanged -> {
                _codeContent.set(event.code)
            }
        }
    }

    /**
     * Load ZenUML code into the web view
     */
    fun loadCode(code: String) {
        _codeContent.set(code)
        send(OutgoingMessage.Event.Load(code))
    }

    /**
     * Update the ZenUML code in the web view
     */
    fun updateCode(code: String) {
        _codeContent.set(code)
        send(OutgoingMessage.Event.Update(code))
    }

    /**
     * Reload the web view with a new theme
     */
    override fun reload(theme: String, onThemeChanged: Runnable) {
        super.reload(theme) {
            // promise needs to be reset, so that it can be listened to again when the reload is complete
            _initializedPromise = AsyncPromise()
            onThemeChanged.run()
        }
    }

    /**
     * Export the diagram as SVG
     */
    fun exportSvg(): Promise<String> {
        val result = AsyncPromise<String>()
        send(OutgoingMessage.Request.Export(OutgoingMessage.Request.Export.SVG)).then { response ->
            val data = (response as IncomingMessage.Response.Export).data
            val payload = data.split(",")[1]
            val decodedBytes = Base64.getDecoder().decode(payload)
            result.setResult(String(decodedBytes))
        }
        return result
    }

    /**
     * Export the diagram as PNG
     */
    fun exportPng(): Promise<ByteArray> {
        val result = AsyncPromise<ByteArray>()
        send(OutgoingMessage.Request.Export(OutgoingMessage.Request.Export.PNG)).then { response ->
            val data = (response as IncomingMessage.Response.Export).data
            val payload = data.split(",")[1]
            val decodedBytes = Base64.getDecoder().decode(payload)
            result.setResult(decodedBytes)
        }
        return result
    }
}
