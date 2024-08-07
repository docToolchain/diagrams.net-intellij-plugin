package de.docs_as_co.intellij.plugin.drawio

import com.intellij.openapi.vfs.VirtualFile
import org.w3c.dom.NodeList
import org.xml.sax.SAXParseException
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadataFormatImpl
import javax.imageio.metadata.IIOMetadataNode
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class DiagramsFileUtil {
    companion object {
        fun isDiagramsFile(file: VirtualFile?): Boolean {
            if (file == null) {
                return false
            }

            if (file.isDirectory || !file.exists()) {
                return false
            }
            //check for the right file extension
            val extensions = arrayOf(".drawio", ".drawio.svg", ".drawio.png", ".dio", ".dio.svg", ".dio.png")
            // Short-circuit for well-known file names. Allows to start with an empty file and open it in the editor.
            if (extensions.any { ext -> file.name.endsWith(ext) }) {
                return true
            }

            // Detect editable SVG. Editable SVG will have an embedded diagrams.net diagram.
            if (file.name.lowercase().endsWith(".svg") && file.exists()) {
                // prevent external content in SVGs. Even when working in a trusted project, resolving external context might slow down the UI
                // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxp-documentbuilderfactory-saxparserfactory-and-dom4j
                val factory = DocumentBuilderFactory.newInstance()
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                factory.setXIncludeAware(false)
                factory.setExpandEntityReferences(false)

                val builder = factory.newDocumentBuilder()
                // if the attribute "content" of element "svg" starts with "<mxfile ", this is a diagrams.net file
                file.inputStream.use {
                    try {
                        val doc = builder.parse(it)
                        val xPathfactory = XPathFactory.newInstance()
                        val xpath = xPathfactory.newXPath()
                        val expr = xpath.compile("/svg/@content")
                        val content = expr.evaluate(doc, XPathConstants.STRING)
                        if (content.toString().startsWith("<mxfile ")) {
                            return true
                        }
                    } catch (ignored: SAXParseException) {
                        // might happen if:
                        // * XML is invalid
                        return false
                    }
                }
            }

            // Detect editable PNG. Editable SVG will have an embedded diagrams.net diagram.
            if (file.name.lowercase().endsWith(".png") && file.exists()) {
                file.inputStream.use {
                    ImageIO.createImageInputStream(it).use { input ->
                        val readers: Iterator<ImageReader> = ImageIO.getImageReaders(input)
                        if (readers.hasNext()) {
                            val reader = readers.next()
                            reader.input = input
                            try {
                                val entries: NodeList = reader.getImageMetadata(0)
                                    .getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName).childNodes

                                // if we find a text node with an attribute mxfile, this is a image contains diagrams.net information
                                for (i in 0 until entries.length) {
                                    val node = entries.item(i) as IIOMetadataNode
                                    if (node.nodeName.equals("Text")) {
                                        for (j in 0 until node.childNodes.length) {
                                            if (node.childNodes.item(j).attributes.getNamedItem("keyword").nodeValue.equals(
                                                    "mxfile"
                                                )
                                            ) {
                                                return true
                                            }
                                        }
                                    }
                                }
                            } catch (iio: IIOException) {
                                // will happen on broken PNG images
                                return false
                            }
                        }
                    }
                }
            }

            // Detect editable SVG. Editable SVG will have an embedded diagrams.net diagram.
            // Ignore large XML files as they are are probably not a diagram.
            if (file.name.lowercase().endsWith(".xml") && file.exists() && file.length < 10_000_000) {
                // prevent external content in SVGs. Even when working in a trusted project, resolving external context might slow down the UI
                // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxp-documentbuilderfactory-saxparserfactory-and-dom4j
                val factory = DocumentBuilderFactory.newInstance()
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                factory.setXIncludeAware(false)
                factory.setExpandEntityReferences(false)

                val builder = factory.newDocumentBuilder()
                // if the XML contains "<mxfile><diagram/></mxfile>", this is a diagrams.net file
                file.inputStream.use {
                    try {
                        val doc = builder.parse(it)
                        val xPathfactory = XPathFactory.newInstance()
                        val xpath = xPathfactory.newXPath()
                        val expr = xpath.compile("/mxfile/diagram")
                        val content = expr.evaluate(doc, XPathConstants.NODESET)
                        if (content is NodeList) {
                            return content.length > 0
                        }
                    } catch (ignored: SAXParseException) {
                        // might happen if:
                        // * XML is invalid
                        return false
                    }
                }
            }

            return false
        }
    }
}
