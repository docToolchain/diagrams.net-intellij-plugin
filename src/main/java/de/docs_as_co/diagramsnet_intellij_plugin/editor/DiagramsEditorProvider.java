package de.docs_as_co.diagramsnet_intellij_plugin.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class DiagramsEditorProvider implements FileEditorProvider, DumbAware {
    /**
     * Method is expected to run fast.
     *
     * @param project
     * @param file    file to be tested for acceptance.
     * @return {@code true} if provider can create valid editor for the specified {@code file}.
     */
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        if (file.isDirectory() || !file.exists()) {
            return false;
        }
        return file.getName().endsWith(".drawio");
    }

    /**
     * Creates editor for the specified file.
     * <p>
     * This method is called only if the provider has accepted this file (i.e. method {@link #accept(Project, VirtualFile)} returned
     * {@code true}).
     * The provider should return only valid editor.
     *
     * @param project
     * @param file
     * @return created editor for specified file.
     */
    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {

        return new DiagramsEditor(project, file);
    }

    /**
     * @return id of type of the editors created with this FileEditorProvider. Each FileEditorProvider should have
     * unique nonnull id. The id is used for saving/loading of EditorStates.
     */
    @Override
    public @NotNull String getEditorTypeId() {
        return "diagrams.net JCEF editor";
    }

    /**
     * @return policy that specifies how editor created via this provider should be opened.
     * @see FileEditorPolicy#NONE
     * @see FileEditorPolicy#HIDE_DEFAULT_EDITOR
     * @see FileEditorPolicy#PLACE_BEFORE_DEFAULT_EDITOR
     */
    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
