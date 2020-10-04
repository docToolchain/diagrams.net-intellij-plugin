package de.docs_as_co.diagramsnet_intellij_plugin.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

public class DiagramsEditor implements FileEditor {
    private Project project;
    private VirtualFile file;
    private JPanel myHtmlPanelWrapper;
    private JBCefBrowser myCefBrowser;

    public DiagramsEditor(Project project, VirtualFile file) {
        this.project = project;
        this.file = file;
        myHtmlPanelWrapper = new JPanel(new BorderLayout());
        final DiagramHtmlPanel newPanel = new DiagramHtmlPanel(file);
        myHtmlPanelWrapper.add(newPanel.getComponent(), BorderLayout.CENTER);
    }

    /**
     * @return component which represents editor in the UI.
     * The method should never return {@code null}.
     */
    @Override
    public @NotNull JComponent getComponent() {
        return myHtmlPanelWrapper;
    }

    /**
     * Returns component to be focused when editor is opened.
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return myHtmlPanelWrapper;
    }

    /**
     * @return editor's name, a string that identifies editor among
     * other editors. For example, UI form might have two editor: "GUI Designer"
     * and "Text". So "GUI Designer" can be a name of one editor and "Text"
     * can be a name of other editor. The method should never return {@code null}.
     */
    @Override
    public @NotNull String getName() {
        return "diagrams.net editor";
    }

    /**
     * Applies given state to the editor.
     *
     * @param state cannot be null
     */
    @Override
    public void setState(@NotNull FileEditorState state) {

    }

    /**
     * @return whether the editor's content is modified in comparison with its file.
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * @return whether the editor is valid or not. An editor is valid if the contents displayed in it still exists. For example, an editor
     * displaying the contents of a file stops being valid if the file is deleted. Editor can also become invalid when it's disposed.
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Adds specified listener.
     *
     * @param listener to be added
     */
    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    /**
     * Removes specified listener.
     *
     * @param listener to be removed
     */
    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    /**
     * The method is optional. Currently is used only by find usages subsystem
     *
     * @return the location of user focus. Typically it's a caret or any other form of selection start.
     */
    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    /**
     * Usually not invoked directly, see class javadoc.
     */
    @Override
    public void dispose() {

    }

    /**
     * @param key
     * @return a user data value associated with this object. Doesn't require read action.
     */
    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    /**
     * Add a new user data value to this object. Doesn't require write action.
     *
     * @param key
     * @param value
     */
    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

    }
}
