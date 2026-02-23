package org.metalib.papifly.fx.samples.code;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.code.api.CodeEditor;
import org.metalib.papifly.fx.code.api.CodeEditorFactory;
import org.metalib.papifly.fx.code.api.CodeEditorStateAdapter;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;

/**
 * Code editor sample with JavaScript syntax highlighting.
 */
public class JavaScriptEditorSample implements SampleScene {

    private static final String SAMPLE_TEXT =
        "// ES Module: greeting utilities\n" +
        "\n" +
        "const DEFAULT_GREETING = 'Hello';\n" +
        "\n" +
        "/**\n" +
        " * Creates a greeting message.\n" +
        " * @param {string} name - The recipient's name.\n" +
        " * @returns {string} The greeting message.\n" +
        " */\n" +
        "export const greet = (name) => {\n" +
        "    return `${DEFAULT_GREETING}, ${name}!`;\n" +
        "};\n" +
        "\n" +
        "/**\n" +
        " * Greets multiple people.\n" +
        " * @param {string[]} names - Array of names.\n" +
        " */\n" +
        "export function greetAll(names) {\n" +
        "    names.forEach(name => {\n" +
        "        const message = greet(name);\n" +
        "        console.log(message);\n" +
        "    });\n" +
        "}\n" +
        "\n" +
        "// Default export\n" +
        "export default {\n" +
        "    greet,\n" +
        "    greetAll,\n" +
        "    version: '1.0.0',\n" +
        "};\n" +
        "\n" +
        "// Usage\n" +
        "greetAll(['Alice', 'Bob', 'PapiflyFX']);\n";

    /**
     * Creates the JavaScript editor sample.
     */
    public JavaScriptEditorSample() {
        // Default constructor.
    }

    @Override
    public String category() {
        return "Code";
    }

    @Override
    public String title() {
        return "JavaScript Editor";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new CodeEditorStateAdapter());
        dm.setContentStateRegistry(registry);
        dm.setContentFactory(new CodeEditorFactory());

        CodeEditor editor = new CodeEditor();
        editor.setLanguageId("javascript");
        editor.setText(SAMPLE_TEXT);
        editor.bindThemeProperty(themeProperty);

        var leaf = dm.createLeaf("greeting.js", editor);
        leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);

        DockTabGroup tabGroup = dm.createTabGroup();
        tabGroup.addLeaf(leaf);
        dm.setRoot(tabGroup);

        return dm.getRootPane();
    }
}
