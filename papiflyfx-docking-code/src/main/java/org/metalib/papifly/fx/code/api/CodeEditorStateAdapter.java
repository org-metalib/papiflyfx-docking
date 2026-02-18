package org.metalib.papifly.fx.code.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.code.state.EditorStateCodec;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ContentStateAdapter implementation for code editor content state.
 *
 * <p>Persistence contract (v1):</p>
 * <ol>
 *   <li>Decode state from map via version-gated helpers.</li>
 *   <li>Rehydrate document text from {@code filePath} when readable.</li>
 *   <li>Apply editor metadata (caret, scroll, language).</li>
 * </ol>
 *
 * <p>Version migration is structured additively: each version gets a dedicated
 * decode method ({@code decodeV1}, {@code migrateV0ToV1}) so future v2+
 * introduction does not require branching chaos.</p>
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

    // --- Version-gated decode helpers ---

    private EditorStateData restoreState(LeafContentData content) {
        if (content == null || content.state() == null) {
            return EditorStateData.empty();
        }
        int version = content.version();
        if (version == VERSION) {
            return decodeV1(content.state());
        }
        if (version == 0) {
            return migrateV0ToV1(content.state());
        }
        return fallbackEmptyState(version);
    }

    /**
     * Decodes a v1 state map.
     */
    private EditorStateData decodeV1(Map<String, Object> state) {
        return EditorStateCodec.fromMap(state);
    }

    /**
     * Migrates a v0 state map to v1 format.
     * V0 used the same payload keys, so this delegates to the v1 codec.
     * Future schema changes would add field transformations here.
     */
    private EditorStateData migrateV0ToV1(Map<String, Object> state) {
        return EditorStateCodec.fromMap(state);
    }

    /**
     * Returns empty state for unknown/future versions.
     */
    private EditorStateData fallbackEmptyState(int version) {
        LOG.log(Level.WARNING, "Unknown state version {0}, falling back to empty state", version);
        return EditorStateData.empty();
    }

    /**
     * Loads file content into the editor when filePath is set and readable.
     * Falls back to an empty document with metadata preserved when the file
     * is missing, unreadable, or the path syntax is invalid
     * (per spec Phase 6 fallback behavior).
     */
    private void rehydrateDocument(CodeEditor editor, EditorStateData state) {
        String filePath = state.filePath();
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        Path path;
        try {
            path = Path.of(filePath);
        } catch (InvalidPathException e) {
            LOG.log(Level.WARNING, "Invalid path syntax, creating empty document: " + filePath, e);
            return;
        }
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
