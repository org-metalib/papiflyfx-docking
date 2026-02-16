package org.metalib.papifly.fx.code.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docks.layout.ContentFactory;

/**
 * ContentFactory implementation for creating code editor content nodes.
 */
public class CodeEditorFactory implements ContentFactory {

    /**
     * Stable factory identifier for code editor content.
     */
    public static final String FACTORY_ID = "code-editor";

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) {
            return null;
        }
        return new CodeEditor();
    }
}
