package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.util.*

class DiagramsWebView(lifetime: Lifetime, uiTheme: String, uiMode: String) : BaseDiagramsWebView(lifetime, uiTheme, uiMode) {
    private val LOG = Logger.getInstance(DiagramsWebView::class.java)
    private var _initializedPromise = AsyncPromise<Unit>()
    private var _loadCompletePromise: AsyncPromise<Unit>? = null

    // hide the internal promise type from the outside
    fun initialized(): Promise<Unit> {
        return _initializedPromise;
    }

    private val _xmlContent = Property<String?>(null)
    val xmlContent: IPropertyView<String?> = _xmlContent

    override fun handleEvent(event: IncomingMessage.Event) {
        LOG.debug("handleEvent: received event ${event.javaClass.simpleName}, class=${event.javaClass.name}")

        when (event) {
            is IncomingMessage.Event.Initialized -> {
                LOG.debug("handleEvent: Initialized event")
                _initializedPromise.setResult(Unit)
            }
            is IncomingMessage.Event.Configure -> {
                LOG.debug("handleEvent: Configure event")
                send(OutgoingMessage.Event.Configure(DrawioConfig(false)))
            }
            is IncomingMessage.Event.AutoSave -> {
                LOG.debug("handleEvent: AutoSave event, xml length=${event.xml.length}")
                _xmlContent.set(event.xml)
            }
            is IncomingMessage.Event.Save -> {
                LOG.debug("handleEvent: Save event")
                // todo trigger save
            }
            is IncomingMessage.Event.Load -> {
                LOG.debug("handleEvent: Load event, promise=${_loadCompletePromise != null}")
                // Signal that load is complete
                _loadCompletePromise?.setResult(Unit)
                _loadCompletePromise = null
            }
            else -> {
                LOG.debug("handleEvent: Ignored event type: ${event.javaClass.name}, event=$event")
            }
        }
    }

    fun loadXmlLike(xmlLike: String): Promise<Unit> {
        LOG.debug("loadXmlLike: Starting load, xml length=${xmlLike.length}")
        _xmlContent.set(null) // xmlLike is not xml
        _loadCompletePromise = AsyncPromise()
        send(OutgoingMessage.Event.Load(xmlLike, 1))
        LOG.debug("loadXmlLike: Load event sent, promise created")
        return _loadCompletePromise!!
    }

    fun loadPng(payload: ByteArray): Promise<Unit> {
        _xmlContent.set(null) // xmlLike is not xml
        _loadCompletePromise = AsyncPromise()
        val xmlLike = "data:image/png;base64," + Base64.getEncoder().encodeToString(payload)
        send(OutgoingMessage.Event.Load(xmlLike, 1))
        return _loadCompletePromise!!
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
