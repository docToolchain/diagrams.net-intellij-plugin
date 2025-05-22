package de.docs_as_co.intellij.plugin.zenuml.editor

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.assertAlive
import de.docs_as_co.intellij.plugin.drawio.utils.LoadableJCEFHtmlPanel
import de.docs_as_co.intellij.plugin.drawio.utils.SchemeHandlerFactory
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.io.InputStream
import java.net.URI

abstract class BaseZenUmlWebView(val lifetime: Lifetime, var theme: String) {
    companion object {
        private val LOG: Logger = Logger.getInstance(BaseZenUmlWebView::class.java)

        val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        private var didRegisterSchemeHandler = false
        private var myTheme = "light"

        fun initializeSchemeHandler(theme: String) {
            // set new theme to private variable. Will be used when rendering the preview the next time
            myTheme = theme
            LOG.info("Initializing scheme handler with theme: $theme")

            if (!didRegisterSchemeHandler) {
                try {
                    didRegisterSchemeHandler = true
                    LOG.info("Registering scheme handler for zenuml-plugin")

                    CefApp.getInstance().registerSchemeHandlerFactory(
                        "https", "zenuml-plugin",
                        SchemeHandlerFactory { uri: URI ->
                            LOG.info("SchemeHandlerFactory handling URI: $uri")

                            if (uri.path == "/index.html") {
                                data class InitialData(
                                    val baseUrl: String,
                                    val theme: String
                                )

                                try {
                                    val resourcePath = "/assets/zenuml/index.html"
                                    LOG.info("Loading resource: $resourcePath")

                                    val inputStream: InputStream? = BaseZenUmlWebView::class.java.getResourceAsStream(resourcePath)

                                    if (inputStream == null) {
                                        LOG.error("Resource not found: $resourcePath")
                                        throw RuntimeException("Resource not found: $resourcePath")
                                    }

                                    val text = inputStream.reader().readText()
                                    LOG.info("Resource loaded, length: ${text.length}")

                                    val initialData = InitialData(
                                        "https://zenuml-plugin",
                                        myTheme
                                    )

                                    val initialDataJson = mapper.writeValueAsString(initialData)
                                    LOG.info("Initial data: $initialDataJson")

                                    val updatedText = text.replace(
                                        "\$\$initialData\$\$",
                                        initialDataJson
                                    )

                                    updatedText.byteInputStream()
                                } catch (e: Exception) {
                                    LOG.error("Error loading index.html", e)
                                    throw e
                                }
                            } else {
                                try {
                                    val resourcePath = "/assets/zenuml${uri.path}"
                                    LOG.info("Loading resource: $resourcePath")

                                    val inputStream = BaseZenUmlWebView::class.java.getResourceAsStream(resourcePath)

                                    if (inputStream == null) {
                                        LOG.error("Resource not found: $resourcePath")
                                        throw RuntimeException("Resource not found: $resourcePath")
                                    }

                                    inputStream
                                } catch (e: Exception) {
                                    LOG.error("Error loading resource: ${uri.path}", e)
                                    throw e
                                }
                            }
                        }
                    ).also { successful ->
                        LOG.info("Scheme handler registration result: $successful")
                        assert(successful)
                    }
                } catch (e: Exception) {
                    LOG.error("Error registering scheme handler", e)
                    throw e
                }
            }
        }
    }

    private val LOG: Logger = Logger.getInstance(javaClass)
    private val panel = LoadableJCEFHtmlPanel("https://zenuml-plugin/index.html", null, null)
    val component = panel.component

    fun openDevTools() {
        panel.browser.openDevtools()
    }

    private val responseMap = HashMap<String, AsyncPromise<IncomingMessage.Response>>()

    init {
        LOG.info("Initializing BaseZenUmlWebView with theme: $theme")

        try {
            object : CefLifeSpanHandlerAdapter() {
                override fun onAfterCreated(browser: CefBrowser?) {
                    super.onAfterCreated(browser)
                    LOG.info("Browser created, initializing scheme handler")
                    initializeSchemeHandler(theme)
                }
            }.also { handler ->
                panel.browser.jbCefClient.addLifeSpanHandler(handler, panel.browser.cefBrowser)
            }

            val jsRequestHandler = JBCefJSQuery.create(panel.browser).also { handler ->
                handler.addHandler { request: String ->
                    LOG.info("Received JS request: ${request.take(100)}...")

                    try {
                        val message = mapper.readValue(request, IncomingMessage::class.java)

                        if (message is IncomingMessage.Response) {
                            val promise = responseMap[message.requestId]
                            if (promise != null) {
                                responseMap.remove(message.requestId)
                                promise.setResult(message)
                            } else {
                                LOG.warn("No promise found for requestId: ${message.requestId}")
                            }
                        }

                        if (message is IncomingMessage.Event) {
                            this.handleEvent(message)
                        }
                    } catch (e: Exception) {
                        LOG.error("Error processing JS request", e)
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
                    LOG.info("Page loaded with status: $httpStatusCode")

                    frame?.executeJavaScript(
                            "window.sendMessageToHost = function(message) {" +
                                    jsRequestHandler.inject("message") +
                                    "};",
                            frame.url, 0
                    )
                }
// TODO: review this
//                override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: Int, errorText: String?, failedUrl: String?) {
//                    LOG.error("Load error: $errorCode - $errorText for URL: $failedUrl")
//                    super.onLoadError(browser, frame, errorCode, errorText, failedUrl)
//                }
            }.also { handler ->
                panel.browser.jbCefClient.addLoadHandler(handler, panel.browser.cefBrowser)
                lifetime.onTermination {
                    panel.browser.jbCefClient.removeLoadHandler(handler, panel.browser.cefBrowser)
                }
            }
        } catch (e: Exception) {
            LOG.error("Error initializing BaseZenUmlWebView", e)
            throw e
        }
    }

    private var requestId = 0

    open fun reload(theme: String, onThemeChanged: Runnable) {
        LOG.info("Reloading with theme: $theme, current theme: ${this.theme}")

        if (this.theme != theme) {
            this.theme = theme
            initializeSchemeHandler(theme)
            this.panel.browser.cefBrowser.reloadIgnoreCache()
            onThemeChanged.run()
        }
    }

    private fun sendMessage(message: OutgoingMessage) {
        lifetime.assertAlive()

        try {
            val json = ObjectMapper().writeValueAsString(message)
            // The webview expects a json string, not an object, so that sending and receiving messages align.
            // This is why we need to encode it again.
            val jsonStr = ObjectMapper().writeValueAsString(json)
            val js = "window.processMessageFromHost($jsonStr)"

            LOG.info("Sending message to browser: ${message.javaClass.simpleName}")

            panel.browser.cefBrowser.executeJavaScript(
                    js,
                    panel.browser.cefBrowser.url, 0
            )
        } catch (e: Exception) {
            LOG.error("Error sending message to browser", e)
        }
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
