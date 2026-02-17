package org.metalib.papifly.fx.code.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.code.state.EditorStateCodec;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.docks.layout.ContentStateAdapter;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ContentStateAdapter implementation for code editor content state.
 */
public class CodeEditorStateAdapter implements ContentStateAdapter {

    private static final Logger LOG = Logger.getLogger(CodeEditorStateAdapter.class.getName());

    /**
     * Current state schema version.
     */
    public static final int VERSION = 1;

    @Override
    public String getTypeKey() {
        return CodeEditorFactory.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof CodeEditor editor)) {
            return Map.of();
        }
        EditorStateData state = editor.captureState();
        return EditorStateCodec.toMap(state);
    }

    @Override
    public Node restore(LeafContentData content) {
        CodeEditor editor = new CodeEditor();
        EditorStateData state = restoreState(content);
        rehydrateDocument(editor, state);
        editor.applyState(state);
        return editor;
    }

    private EditorStateData restoreState(LeafContentData content) {
        if (content == null || content.state() == null) {
            return EditorStateData.empty();
        }
        int version = content.version();
        if (version == VERSION) {
            return EditorStateCodec.fromMap(content.state());
        }
        if (version == 0) {
            // v0 used the same payload keys, but this branch is a dedicated migration hook.
            return EditorStateCodec.fromMap(content.state());
        }
        // Unknown future/legacy versions fall back to a safe minimal editor state.
        return EditorStateData.empty();
    }

    /**
     * Loads file content into the editor when filePath is set and readable.
     * Falls back to an empty document with metadata preserved when the file
     * is missing or unreadable (per spec §6 fallback behavior).
     */
    private void rehydrateDocument(CodeEditor editor, EditorStateData state) {
        String filePath = state.filePath();
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        Path path = Path.of(filePath);
        if (!Files.isReadable(path)) {
            LOG.log(Level.WARNING, "File not readable, creating empty document: {0}", filePath);
            return;
        }
        try {
            String text = Files.readString(path);
            editor.setText(text);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load file, creating empty document: " + filePath, e);
        }
    }
}
