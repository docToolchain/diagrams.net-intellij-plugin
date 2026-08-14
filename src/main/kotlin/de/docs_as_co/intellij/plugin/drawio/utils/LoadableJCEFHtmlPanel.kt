package de.docs_as_co.intellij.plugin.drawio.utils

import com.intellij.CommonBundle
import com.intellij.ide.plugins.MultiPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JCEFHtmlPanel
import de.docs_as_co.intellij.plugin.drawio.DiagramsNetBundle
import kotlinx.coroutines.*
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import javax.swing.JComponent

class LoadableJCEFHtmlPanel(
    url: String? = null, html: String? = null,
    var timeoutCallback: String? = DiagramsNetBundle.message("diagrams.editor.timeout")
) : Disposable {
    private val htmlPanelComponent = JCEFHtmlPanel(
        JBCefApp.isOffScreenRenderingModeEnabled(),
        null,
        null)

    // Create a local Disposable parent for loadingPanel to avoid registering with ROOT_DISPOSABLE
    private val loadingPanelDisposable = Disposer.newDisposable()
    private val loadingPanel = JBLoadingPanel(BorderLayout(), loadingPanelDisposable).apply { setLoadingText(CommonBundle.getLoadingTreeNodeText()) }
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)
    private var timeoutJob: Job? = null

    val browser: JBCefBrowserBase get() = htmlPanelComponent

    companion object {
        private const val LOADING_KEY = 1
        private const val CONTENT_KEY = 0
    }

    private val multiPanel: MultiPanel = object : MultiPanel() {
        override fun create(key: Int) = when (key) {
            LOADING_KEY -> loadingPanel
            CONTENT_KEY -> htmlPanelComponent.component
            else -> throw UnsupportedOperationException("Unknown key")
        }
    }

    init {
        if (url != null) {
            htmlPanelComponent.loadURL(url)
        }
        if (html != null) {
            htmlPanelComponent.loadHTML(html)
        }
        multiPanel.select(CONTENT_KEY, true)
    }

    init {
        htmlPanelComponent.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadStart(browser: CefBrowser?, frame: CefFrame?, transitionType: CefRequest.TransitionType?) {
                timeoutJob?.cancel()
                timeoutJob = coroutineScope.launch {
                    delay(Registry.intValue("html.editor.timeout", 10000).toLong())
                    htmlPanelComponent.setHtml(timeoutCallback!!)
                }
            }

            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                timeoutJob?.cancel()
            }

            override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                if (isLoading) {
                    invokeLater {
                        loadingPanel.startLoading()
                        multiPanel.select(LOADING_KEY, true)
                    }
                } else {
                    invokeLater {
                        loadingPanel.stopLoading()
                        multiPanel.select(CONTENT_KEY, true)
                    }
                }
            }
        }, htmlPanelComponent.cefBrowser)
    }

    override fun dispose() {
        coroutineScope.cancel()
        loadingPanel.stopLoading()
        Disposer.dispose(loadingPanelDisposable)  // Dispose the loading panel and its children
        htmlPanelComponent.dispose()
    }

    val component: JComponent get() = this.multiPanel

    fun loadUrl(url: String) {
        htmlPanelComponent.loadURL(url)
    }

}
