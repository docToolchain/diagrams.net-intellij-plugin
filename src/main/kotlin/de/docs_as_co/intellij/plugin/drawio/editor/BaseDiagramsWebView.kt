package de.docs_as_co.intellij.plugin.drawio.editor

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.assertAlive
import com.jetbrains.rd.util.lifetime.onTermination
import de.docs_as_co.intellij.plugin.drawio.settings.DiagramsUiTheme
import de.docs_as_co.intellij.plugin.drawio.utils.LoadableJCEFHtmlPanel
import de.docs_as_co.intellij.plugin.drawio.utils.SchemeHandlerFactory
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.net.URI

abstract class BaseDiagramsWebView(val lifetime: Lifetime, var uiTheme: String) {
    companion object {
        val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        private var didRegisterSchemeHandler = false
        private var myUiTheme = DiagramsUiTheme.KENNEDY.key;
        fun initializeSchemeHandler(uiTheme: String) {
            // set new theme to private variable. Will be used when rendering the preview the next time
            myUiTheme = uiTheme

            if (!didRegisterSchemeHandler) {
                didRegisterSchemeHandler = true
                CefApp.getInstance().registerSchemeHandlerFactory(
                    // needed to use "https" as scheme here as "drawio-plugin" scheme didn't allow for CORS requests that were needed
                    // to start the diagrams.net application in the JCEF/Chromium preview browser.
                    // Worked in previous versions, but not from IntelliJ 2021.1 onwards; maybe due to tightened security in Chromium.
                    // Error message was: "CORS policy: Cross origin requests are only supported for protocol schemes..."
                    "https", "drawio-plugin",
                    SchemeHandlerFactory { uri: URI ->
                        if (uri.path == "/index.html") {
                            data class InitialData(
                                val baseUrl: String,
                                val localStorage: String?,
                                val theme: String,
                                val lang: String,
                                val showChrome: String
                            )

                            val text =
                                BaseDiagramsWebView::class.java.getResourceAsStream("/assets/index.html").reader()
                                    .readText()
                            val updatedText = text.replace(
                                "\$\$initialData\$\$",
                                mapper.writeValueAsString(
                                    InitialData(
                                        "https://drawio-plugin",
                                        null,
                                        myUiTheme,
                                        "en",
                                        "1"
                                    )
                                )
                            )

                            updatedText.byteInputStream()
                        } else {
                            BaseDiagramsWebView::class.java.getResourceAsStream("/assets" + uri.path)
                        }
                    }
                ).also { successful -> assert(successful) }
            }
        }
    }

    private val panel = LoadableJCEFHtmlPanel("https://drawio-plugin/index.html", null, null)
    val component = panel.component

    private val responseMap = HashMap<String, AsyncPromise<IncomingMessage.Response>>()

    init {
        initializeSchemeHandler(uiTheme)
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

    open fun reload(uiTheme: String, onThemeChanged: Runnable) {
        if (this.uiTheme != uiTheme) {
            this.uiTheme = uiTheme
            initializeSchemeHandler(uiTheme)
            this.panel.browser.cefBrowser.reloadIgnoreCache()
            onThemeChanged.run();
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

