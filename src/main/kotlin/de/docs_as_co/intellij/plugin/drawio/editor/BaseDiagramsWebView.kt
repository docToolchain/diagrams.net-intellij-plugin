package de.docs_as_co.intellij.plugin.drawio.editor

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.assertAlive
import de.docs_as_co.intellij.plugin.drawio.settings.DiagramsUiMode
import de.docs_as_co.intellij.plugin.drawio.settings.DiagramsUiTheme
import de.docs_as_co.intellij.plugin.drawio.utils.LoadableJCEFHtmlPanel
import de.docs_as_co.intellij.plugin.drawio.utils.SchemeHandlerFactory
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.net.URI

abstract class BaseDiagramsWebView(val lifetime: Lifetime, var uiTheme: String, var uiMode: String) {
    companion object {
        val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        private var didRegisterSchemeHandler = false
        private var myUiTheme = DiagramsUiTheme.DEFAULT.key
        private var myUiMode = DiagramsUiMode.AUTO.key
        fun initializeSchemeHandler(uiTheme: String, uiMode: String) {
            // set new theme to private variable. Will be used when rendering the preview the next time
            myUiTheme = uiTheme
            myUiMode = uiMode

            if (!didRegisterSchemeHandler) {
                didRegisterSchemeHandler = true
                CefApp.getInstance().registerSchemeHandlerFactory(
                    // needed to use "https" as scheme here as "drawio-plugin" scheme didn't allow for CORS requests that were needed
                    // to start the diagrams.net application in the JCEF/Chromium preview browser.
                    // Worked in previous versions, but not from IntelliJ 2021.1 onwards; maybe due to tightened security in Chromium.
                    // Error message was: "CORS policy: Cross origin requests are only supported for protocol schemes..."
                    "https", "drawio-plugin",
                    SchemeHandlerFactory { uri: URI ->
                        println("SCHEME HANDLER: Requested URI: ${uri.path}")
                        if (uri.path == "/index.html") {
                            println("SCHEME HANDLER: Serving index.html with initialData")
                            // Build initial data JSON manually to avoid Jackson reflection issues with local classes
                            val initialDataJson = """{"baseUrl":"https://drawio-plugin","localStorage":null,"theme":"$myUiTheme","mode":"$myUiMode","lang":"en","showChrome":"1"}"""

                            val text =
                                BaseDiagramsWebView::class.java.getResourceAsStream("/assets/index.html")?.reader()
                                    ?.readText()
                            if (text == null) {
                                println("SCHEME HANDLER ERROR: Failed to load /assets/index.html")
                                null
                            } else {
                                val updatedText = text.replace(
                                    "\$\$initialData\$\$",
                                    initialDataJson
                                )
                                println("SCHEME HANDLER: index.html loaded, size: ${updatedText.length} bytes")
                                updatedText.byteInputStream()
                            }
                        } else {
                            println("SCHEME HANDLER: Serving asset: /assets${uri.path}")
                            val stream = BaseDiagramsWebView::class.java.getResourceAsStream("/assets" + uri.path)
                            if (stream == null) {
                                println("SCHEME HANDLER ERROR: Asset not found: /assets${uri.path}")
                            } else {
                                println("SCHEME HANDLER: Asset found: /assets${uri.path}")
                            }
                            stream
                        }
                    }
                ).also { successful -> assert(successful) }
            }
        }
    }

    private val panel = LoadableJCEFHtmlPanel("https://drawio-plugin/index.html", null, null)
    val component = panel.component

    fun openDevTools() {
        panel.browser.openDevtools()
    }

    private val responseMap = HashMap<String, AsyncPromise<IncomingMessage.Response>>()

    init {
        object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser?) {
                super.onAfterCreated(browser)
                initializeSchemeHandler(uiTheme, uiMode)
            }
        }.also { handler ->
            panel.browser.jbCefClient.addLifeSpanHandler(handler, panel.browser.cefBrowser)
        }
        val jsRequestHandler = JBCefJSQuery.create(panel.browser).also { handler ->
            handler.addHandler { request: String ->
                val message = mapper.readValue(request, IncomingMessage::class.java)

                if (message is IncomingMessage.Response) {
                    val promise = responseMap[message.requestId]!!
                    responseMap.remove(message.requestId)
                    promise.setResult(message)
                }

                if (message is IncomingMessage.Event) {
                    this.handleEvent(message)
                }

                null
            }
            lifetime.onTermination {
                handler.dispose()
                panel.dispose()
            }
        }
        object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                frame?.executeJavaScript(
                        "window.sendMessageToHost = function(message) {" +
                                jsRequestHandler.inject("message") +
                                "};",
                        frame.url, 0
                )
            }
        }.also { handler ->
            panel.browser.jbCefClient.addLoadHandler(handler, panel.browser.cefBrowser)
            lifetime.onTermination {
                panel.browser.jbCefClient.removeLoadHandler(handler, panel.browser.cefBrowser)
            }
        }
    }

    private var requestId = 0

    open fun reload(uiTheme: String, uiMode: String, onThemeChanged: Runnable) {
        if (this.uiTheme != uiTheme || this.uiMode != uiMode) {
            this.uiTheme = uiTheme
            this.uiMode = uiMode
            initializeSchemeHandler(uiTheme, uiMode)
            this.panel.browser.cefBrowser.reloadIgnoreCache()
            onThemeChanged.run()
        }

    }
    private fun sendMessage(message: OutgoingMessage) {
        lifetime.assertAlive()

        val json = ObjectMapper().writeValueAsString(message)
        // The webview expects a json string, not an object, so that sending and receiving messages align.
        // This is why we need to encode it again.
        val jsonStr = ObjectMapper().writeValueAsString(json)
        val js = "window.processMessageFromHost($jsonStr)"
        panel.browser.cefBrowser.executeJavaScript(
                js,
                panel.browser.cefBrowser.url, 0
        )
    }

    protected fun send(message: OutgoingMessage.Request): Promise<IncomingMessage.Response> {
        message.requestId = "req-${requestId++}"
        val result = AsyncPromise<IncomingMessage.Response>()
        responseMap[message.requestId!!] = result
        sendMessage(message)
        return result
    }

    protected fun send(message: OutgoingMessage.Event) {
        sendMessage(message)
    }

    protected abstract fun handleEvent(event: IncomingMessage.Event)
}

