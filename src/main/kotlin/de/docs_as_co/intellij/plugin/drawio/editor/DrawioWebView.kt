package de.docs_as_co.intellij.plugin.drawio.editor

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.util.*

class DrawioWebView(lifetime: Lifetime) : BaseDrawioWebView(lifetime) {
    private val _initializedPromise = AsyncPromise<Unit>()

    val initializedPromise: Promise<Unit> = _initializedPromise;

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
        }
    }

    fun loadXmlLike(xmlLike: String) {
        _xmlContent.set(null) // xmlLike is not xml
        send(OutgoingMessage.Event.Load(xmlLike, 1))
    }
    fun exportSvg() {
        send(OutgoingMessage.Request.Export(OutgoingMessage.Request.Export.XMLSVG)).then { response ->
            val data = (response as IncomingMessage.Response.Export).data
            val payload = data.split(",")[1]
            val decodedBytes = Base64.getDecoder().decode(payload)
            val svg = String(decodedBytes)
            _xmlContent.set(svg)
        }
    }
}