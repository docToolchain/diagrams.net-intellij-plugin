package de.docs_as_co.intellij.plugin.drawio.actions

import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Regression tests for issue #365: creating a diagram from a bundled
 * internal file template must not fail with
 * "Unknown bundled file template ... register it in plugin.xml via <internalFileTemplate/>".
 */
class DiagramCreateFileActionTest : BasePlatformTestCase() {

    private fun create(name: String, templateName: String): PsiFile? {
        val directory = myFixture.psiManager.findDirectory(myFixture.tempDirFixture.findOrCreateDir("diagrams"))!!
        return WriteAction.computeAndWait<PsiFile?, RuntimeException> {
            DiagramCreateFileAction().createFile(name, templateName, directory)
        }
    }

    fun testCreateXmlDiagram() {
        val file = create("architecture", "xml")
        assertNotNull(file)
        assertEquals("architecture.drawio", file!!.name)
        assertTrue(file.text.contains("<mxfile"))
    }

    fun testCreateEditableSvgDiagram() {
        val file = create("architecture", "editable-svg")
        assertNotNull(file)
        assertEquals("architecture.svg", file!!.name)
        assertTrue(file.text.contains("<svg"))
    }

    fun testCreateEditablePngDiagram() {
        val file = create("architecture", "editable-png")
        assertNotNull(file)
        assertEquals("architecture.png", file!!.name)
        assertTrue(file.virtualFile.length > 0)
    }
}
