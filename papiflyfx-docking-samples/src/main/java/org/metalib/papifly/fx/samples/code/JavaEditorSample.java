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
 * Code editor sample with Java syntax highlighting.
 */
public class JavaEditorSample implements SampleScene {

    private static final String SAMPLE_TEXT =
        "package org.example;\n" +
        "\n" +
        "/**\n" +
        " * A simple Hello World program demonstrating Java syntax highlighting.\n" +
        " */\n" +
        "public class HelloWorld {\n" +
        "\n" +
        "    // Greeting constant\n" +
        "    private static final String GREETING = \"Hello, World!\";\n" +
        "\n" +
        "    public static void main(String[] args) {\n" +
        "        System.out.println(GREETING);\n" +
        "        greet(\"PapiflyFX\");\n" +
        "    }\n" +
        "\n" +
        "    /**\n" +
        "     * Prints a personalised greeting.\n" +
        "     *\n" +
        "     * @param name the recipient's name\n" +
        "     */\n" +
        "    public static void greet(String name) {\n" +
        "        String message = \"Hello, \" + name + \"!\";\n" +
        "        System.out.println(message);\n" +
        "    }\n" +
        "\n" +
        "    /** Returns the default greeting string. */\n" +
        "    public static String getGreeting() {\n" +
        "        return GREETING;\n" +
        "    }\n" +
        "}\n";

    /**
     * Creates the Java editor sample.
     */
    public JavaEditorSample() {
        // Default constructor.
    }

    @Override
    public String category() {
        return "Code";
    }

    @Override
    public String title() {
        return "Java Editor";
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
        editor.setLanguageId("java");
        editor.setText(SAMPLE_TEXT);
        editor.bindThemeProperty(themeProperty);

        var leaf = dm.createLeaf("HelloWorld.java", editor);
        leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);

        DockTabGroup tabGroup = dm.createTabGroup();
        tabGroup.addLeaf(leaf);
        dm.setRoot(tabGroup);

        return dm.getRootPane();
    }
}
