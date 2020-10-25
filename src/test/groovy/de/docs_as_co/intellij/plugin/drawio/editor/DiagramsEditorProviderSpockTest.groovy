package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.mock.MockVirtualFileSystem
import com.intellij.openapi.command.impl.DummyProject
import spock.lang.Specification


class DiagramsEditorProviderSpockTest extends Specification {
    def "test accept() method"() {
        given: "an instance of DiagramsEditorProvider"
            def editorProvider = new DiagramsEditorProvider()
            def project = DummyProject.getInstance()
            def vFS = new MockVirtualFileSystem()
            vFS.file("test.dio.svg","")
            def file = vFS.findFileByPath(filename)
        when: ""
            println filename
        then: """
            the accept() method is called with a filename,
            it only returns `true` for the correct names
            """
            editorProvider.accept(project, file) == result
        where:
            filename              || result
            "test.drawio.svg"     || true
            "test.drawio"         || true
            "test.svg"            || false
            "test.xml"            || false
            "test.dio"            || true
            "test.dio.svg"        || true
            "dio.svg"             || false
            "drawio.svg"          || false
            "test.dio.svg.xml"    || false
    }
}
