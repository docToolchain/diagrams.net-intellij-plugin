package de.docs_as_co.intellij.plugin.drawio

import com.intellij.openapi.vfs.VirtualFile
import org.w3c.dom.NodeList
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
            if (file.name.endsWith(".svg")) {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                // if if the attribute "content" of element "svg" starts with "<mxfile ", this is a diagrams.net file
                file.inputStream.use {
                    val doc = builder.parse(it)
                    val xPathfactory = XPathFactory.newInstance()
                    val xpath = xPathfactory.newXPath()
                    val expr = xpath.compile("/svg/@content")
                    val content = expr.evaluate(doc, XPathConstants.STRING)
                    if (content.toString().startsWith("<mxfile ")) {
                        return true
                    }
                }
            }

            // Detect editable PNG. Editable SVG will have an embedded diagrams.net diagram.
            if (file.name.endsWith(".png")) {
                file.inputStream.use {
                    ImageIO.createImageInputStream(it).use { input ->
                        val readers: Iterator<ImageReader> = ImageIO.getImageReaders(input)
                        if (readers.hasNext()) {
                            val reader = readers.next()
                            reader.input = input
                            val entries: NodeList = reader.getImageMetadata(0).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName).childNodes

                            // if we find a text node with an attribute mxfile, this is a image contains diagrams.net information
                            for (i in 0 until entries.length) {
                                val node = entries.item(i) as IIOMetadataNode
                                if (node.nodeName.equals("Text")) {
                                    for (j in 0 until node.childNodes.length) {
                                        if (node.childNodes.item(j).attributes.getNamedItem("keyword").nodeValue.equals("mxfile")) {
                                            return true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return false
        }
    }
}
