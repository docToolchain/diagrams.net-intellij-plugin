package de.docs_as_co.intellij.plugin.drawio.editor

import com.intellij.mock.MockVirtualFileSystem
import com.intellij.openapi.command.impl.DummyProject
import spock.lang.Ignore
import spock.lang.Specification

import javax.swing.JEditorPane

class DiagramsEditorTest extends Specification {
    @Ignore("needs proper implementation")
    def "test saveFile()"() {
        given:
            def project = DummyProject.getInstance()
            def vFS = new MockVirtualFileSystem()
            vFS.file("test.dio.svg","")
            def file = vFS.findFileByPath("test.dio.svg")
            def editorProvider = new DiagramsEditorProvider()
            def editor = new DiagramsEditor(project, file)
        when:
            editor.saveFile("<test />")
        then:
            file.content == "<test />"
    }
}
