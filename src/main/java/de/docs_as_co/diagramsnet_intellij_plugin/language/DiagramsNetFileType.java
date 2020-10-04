package de.docs_as_co.diagramsnet_intellij_plugin.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DiagramsNetFileType extends LanguageFileType {
    private DiagramsNetFileType() {
        super(DiagramsNet.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Diagrams.net Diagram";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Simple language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "drawio";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return DiagramsNetIcon.FILE;
    }

    public static final DiagramsNetFileType INSTANCE = new DiagramsNetFileType();
}
