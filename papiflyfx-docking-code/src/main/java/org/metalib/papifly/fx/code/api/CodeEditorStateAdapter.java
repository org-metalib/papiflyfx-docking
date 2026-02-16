package org.metalib.papifly.fx.code.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.code.state.EditorStateCodec;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.docks.layout.ContentStateAdapter;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;

import java.util.Map;

/**
 * ContentStateAdapter implementation for code editor content state.
 */
public class CodeEditorStateAdapter implements ContentStateAdapter {

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
        if (content == null || content.state() == null) {
            return editor;
        }
        editor.applyState(EditorStateCodec.fromMap(content.state()));
        return editor;
    }
}
