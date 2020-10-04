package de.docs_as_co.diagramsnet_intellij_plugin.editor;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class DiagramHtmlPanel extends JCEFHtmlPanel {
    private JBCefJSQuery myloadIntelliJ;
    public DiagramHtmlPanel(VirtualFile file) {
        super("about:blank@" + new Random().nextInt(Integer.MAX_VALUE));
        String fileContent;
        try (InputStream stream = file.getInputStream()) {
            fileContent = IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        myloadIntelliJ = JBCefJSQuery.create(this);
        myloadIntelliJ.addHandler(s -> {
            return new JBCefJSQuery.Response(fileContent);
        });

        try (InputStream stream = DiagramHtmlPanel.class.getResourceAsStream("/base.html")) {
            @NotNull String html = IOUtils.toString(stream, StandardCharsets.UTF_8);
            ;
            String jsSnippet = "function loadIntelliJ() { return '" + StringEscapeUtils.escapeEcmaScript(fileContent) + "' } ";
            html = html.replace("/* CUSTOMCODE */", jsSnippet);
            html = html.replace("%%DATA%%", fileContent);
            this.setHtml(html);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
