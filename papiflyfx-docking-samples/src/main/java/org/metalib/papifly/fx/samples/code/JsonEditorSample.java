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
 * Code editor sample with JSON syntax highlighting.
 */
public class JsonEditorSample implements SampleScene {

    private static final String SAMPLE_TEXT =
        "{\n" +
        "  \"name\": \"papiflyfx-docking\",\n" +
        "  \"version\": \"0.0.6-SNAPSHOT\",\n" +
        "  \"description\": \"JavaFX docking and layout components\",\n" +
        "  \"groupId\": \"org.metalib.papifly.docking\",\n" +
        "  \"modules\": [\n" +
        "    \"papiflyfx-docking-api\",\n" +
        "    \"papiflyfx-docking-docks\",\n" +
        "    \"papiflyfx-docking-code\",\n" +
        "    \"papiflyfx-docking-samples\"\n" +
        "  ],\n" +
        "  \"build\": {\n" +
        "    \"java\": \"25\",\n" +
        "    \"javafx\": \"23.0.1\",\n" +
        "    \"maven\": \"3.9+\"\n" +
        "  },\n" +
        "  \"themes\": [\n" +
        "    { \"id\": \"dark\",  \"background\": \"#1e1e1e\", \"foreground\": \"#d4d4d4\" },\n" +
        "    { \"id\": \"light\", \"background\": \"#ffffff\", \"foreground\": \"#1e1e1e\" }\n" +
        "  ],\n" +
        "  \"license\": \"Apache-2.0\",\n" +
        "  \"published\": false\n" +
        "}\n";

    /**
     * Creates the JSON editor sample.
     */
    public JsonEditorSample() {
        // Default constructor.
    }

    @Override
    public String category() {
        return "Code";
    }

    @Override
    public String title() {
        return "JSON Editor";
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
        editor.setLanguageId("json");
        editor.setText(SAMPLE_TEXT);
        editor.bindThemeProperty(themeProperty);

        var leaf = dm.createLeaf("package.json", editor);
        leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);

        DockTabGroup tabGroup = dm.createTabGroup();
        tabGroup.addLeaf(leaf);
        dm.setRoot(tabGroup);

        return dm.getRootPane();
    }
}
