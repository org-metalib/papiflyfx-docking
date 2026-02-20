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
 * Code editor sample with Markdown syntax highlighting.
 */
public class MarkdownEditorSample implements SampleScene {

    private static final String SAMPLE_TEXT =
        "# PapiflyFX Docking Framework\n" +
        "\n" +
        "A JavaFX docking library for building IDE-style layouts.\n" +
        "\n" +
        "## Features\n" +
        "\n" +
        "- Split layouts (horizontal and vertical)\n" +
        "- Tab groups with close, float, minimize, and maximize\n" +
        "- Floating windows with re-dock support\n" +
        "- Session persistence (JSON round-trip)\n" +
        "- Code editor integration with syntax highlighting\n" +
        "\n" +
        "## Quick Start\n" +
        "\n" +
        "```java\n" +
        "var dm = new DockManager();\n" +
        "var leaf = dm.createLeaf(\"Panel\", content);\n" +
        "var group = dm.createTabGroup();\n" +
        "group.addLeaf(leaf);\n" +
        "dm.setRoot(group);\n" +
        "```\n" +
        "\n" +
        "## Themes\n" +
        "\n" +
        "Supports `Theme.dark()` and `Theme.light()` with live switching.\n" +
        "Bind the shared `ObjectProperty<Theme>` to every `DockManager`.\n";

    @Override
    public String category() {
        return "Code";
    }

    @Override
    public String title() {
        return "Markdown Editor";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new CodeEditorStateAdapter());
        dm.setContentStateRegistry(registry);
        dm.setContentFactory(new CodeEditorFactory());

        CodeEditor editor = new CodeEditor();
        editor.setLanguageId("markdown");
        editor.setText(SAMPLE_TEXT);
        editor.bindThemeProperty(themeProperty);

        var leaf = dm.createLeaf("README.md", editor);
        leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);

        DockTabGroup tabGroup = dm.createTabGroup();
        tabGroup.addLeaf(leaf);
        dm.setRoot(tabGroup);

        return dm.getRootPane();
    }
}
