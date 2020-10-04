package de.docs_as_co.diagramsnet_intellij_plugin.language;

import com.intellij.lang.Language;

public class DiagramsNet extends Language {
    private DiagramsNet() {
        super("Diagrams.net");
    }

    public static final DiagramsNet INSTANCE = new DiagramsNet();
}
