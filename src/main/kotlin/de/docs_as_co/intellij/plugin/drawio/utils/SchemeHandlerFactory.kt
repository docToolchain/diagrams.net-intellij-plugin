package de.docs_as_co.intellij.plugin.drawio.utils

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.io.IOException
import java.io.InputStream
import java.net.URI

class SchemeHandlerFactory(val getStream: (uri: URI) -> InputStream?) : CefSchemeHandlerFactory {
    override fun create(browser: CefBrowser, frame: CefFrame, schemeName: String, request: CefRequest): CefResourceHandler {
        val uri = URI(request.url)

        val myStream = getStream(uri)

        return object : CefResourceHandler {
            override fun processRequest(req: CefRequest, callback: CefCallback): Boolean {
                callback.Continue()
                return true
            }

            override fun getResponseHeaders(response: CefResponse, response_length: IntRef, redirectUrl: StringRef?) {

                if (uri.path.endsWith(".html")) {
                    response.mimeType = "text/html"
                } else if (uri.path.endsWith(".js")) {
                    response.mimeType = "application/javascript"
                } else if (uri.path.endsWith(".css")) {
                    response.mimeType = "text/css"
                } else if (uri.path.endsWith(".svg")) {
                    response.mimeType = "image/svg+xml"
                }

                if (myStream === null) {
                    response.status = 404
                } else {
                    response.status = 200
                }
            }

            override fun readResponse(data_out: ByteArray, bytes_to_read: Int, bytes_read: IntRef, callback: CefCallback): Boolean {
                if (myStream === null) {
                    bytes_read.set(0)
                    return false
                }
                try {
                    val availableSize = myStream.available()
                    return if (availableSize > 0) {
                        bytes_read.set(myStream.read(data_out, 0, bytes_to_read.coerceAtMost(availableSize)))
                        true
                    } else {
                        bytes_read.set(0)
                        try {
                            myStream.close()
                        } catch (ex: IOException) {

                        }

                        false
                    }
                } catch (ex: IOException) {

                    return false
                }
            }

            override fun cancel() {
            }
        }
    }
}
