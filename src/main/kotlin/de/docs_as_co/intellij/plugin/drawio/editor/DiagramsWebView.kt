package de.docs_as_co.intellij.plugin.drawio.editor

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.util.*

class DiagramsWebView(lifetime: Lifetime, uiTheme: String, uiMode: String) : BaseDiagramsWebView(lifetime, uiTheme, uiMode) {
    private var _initializedPromise = AsyncPromise<Unit>()

    // hide the internal promise type from the outside
    fun initialized(): Promise<Unit> {
        return _initializedPromise;
    }

    private val _xmlContent = Property<String?>(null)
    val xmlContent: IPropertyView<String?> = _xmlContent

    override fun handleEvent(event: IncomingMessage.Event) {

        when (event) {
            is IncomingMessage.Event.Initialized -> {
                _initializedPromise.setResult(Unit)
            }
            is IncomingMessage.Event.Configure -> {
                send(OutgoingMessage.Event.Configure(DrawioConfig(false)))
            }
            is IncomingMessage.Event.AutoSave -> {
                _xmlContent.set(event.xml)
            }
            is IncomingMessage.Event.Save -> {
                // todo trigger save
            }
            IncomingMessage.Event.Load -> {
                // Ignore
            }
            else -> {
                // Handle any other event types that might be coming from the JavaScript side
                // This prevents NoWhenBranchMatchedException
            }
        }
    }

    fun loadXmlLike(xmlLike: String) {
        _xmlContent.set(null) // xmlLike is not xml
        send(OutgoingMessage.Event.Load(xmlLike, 1))
    }

    fun loadPng(payload: ByteArray) {
        _xmlContent.set(null) // xmlLike is not xml
        val xmlLike = "data:image/png;base64," + Base64.getEncoder().encodeToString(payload)
        send(OutgoingMessage.Event.Load(xmlLike, 1))
    }

    override fun reload(uiTheme: String, uiMode: String, onThemeChanged: Runnable) {
        super.reload(uiTheme, uiMode) {
            // promise needs to be reset, to that it can be listened to again when the reload is complete
            _initializedPromise = AsyncPromise()
            onThemeChanged.run()
        }
    }

    fun exportSvg() : Promise<String> {
        val result = AsyncPromise<String>()
        send(OutgoingMessage.Request.Export(OutgoingMessage.Request.Export.XMLSVG)).then  { response ->
            val data = (response as IncomingMessage.Response.Export).data
            val payload = data.split(",")[1]
            val decodedBytes = Base64.getDecoder().decode(payload)
            result.setResult(String(decodedBytes))
        }
        return result
    }
    fun exportPng() : Promise<ByteArray> {
        val result = AsyncPromise<ByteArray>()
        send(OutgoingMessage.Request.Export(OutgoingMessage.Request.Export.XMLPNG)).then  { response ->
            val data = (response as IncomingMessage.Response.Export).data
            val payload = data.split(",")[1]
            val decodedBytes = Base64.getDecoder().decode(payload)
            result.setResult(decodedBytes)
        }
        return result
    }

}
