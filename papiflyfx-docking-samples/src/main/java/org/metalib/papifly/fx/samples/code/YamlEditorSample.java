package org.metalib.papifly.fx.samples.code;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.code.api.CodeEditor;
import org.metalib.papifly.fx.code.api.CodeEditorFactory;
import org.metalib.papifly.fx.code.api.CodeEditorStateAdapter;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.samples.SampleScene;

/**
 * Code editor sample with YAML syntax highlighting.
 */
public class YamlEditorSample implements SampleScene {

    private static final String SAMPLE_TEXT =
        "---\n" +
        "apiVersion: apps/v1\n" +
        "kind: Deployment\n" +
        "metadata:\n" +
        "  name: papiflyfx-samples\n" +
        "  labels:\n" +
        "    app: papiflyfx\n" +
        "    tier: demo\n" +
        "spec:\n" +
        "  replicas: 2\n" +
        "  selector:\n" +
        "    matchLabels:\n" +
        "      app: papiflyfx\n" +
        "  template:\n" +
        "    metadata:\n" +
        "      labels:\n" +
        "        app: papiflyfx\n" +
        "    spec:\n" +
        "      containers:\n" +
        "        - name: samples-app\n" +
        "          image: ghcr.io/org-metalib/papiflyfx-samples:latest\n" +
        "          imagePullPolicy: IfNotPresent\n" +
        "          ports:\n" +
        "            - containerPort: 8080\n" +
        "          env:\n" +
        "            - name: PAPIFLYFX_THEME\n" +
        "              value: \"dark\"\n" +
        "            - name: PAPIFLYFX_DEMO_MODE\n" +
        "              value: \"true\"\n" +
        "          readinessProbe:\n" +
        "            httpGet:\n" +
        "              path: /health\n" +
        "              port: 8080\n" +
        "            initialDelaySeconds: 5\n" +
        "            periodSeconds: 10\n" +
        "          resources:\n" +
        "            requests:\n" +
        "              cpu: 100m\n" +
        "              memory: 128Mi\n" +
        "            limits:\n" +
        "              cpu: 500m\n" +
        "              memory: 512Mi\n";

    /**
     * Creates the YAML editor sample.
     */
    public YamlEditorSample() {
        // Default constructor.
    }

    @Override
    public String category() {
        return "Code";
    }

    @Override
    public String title() {
        return "YAML Editor";
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
        editor.setLanguageId("yaml");
        editor.setText(SAMPLE_TEXT);
        editor.bindThemeProperty(themeProperty);

        var leaf = dm.createLeaf("deployment.yaml", editor);
        leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);

        DockTabGroup tabGroup = dm.createTabGroup();
        tabGroup.addLeaf(leaf);
        dm.setRoot(tabGroup);

        return dm.getRootPane();
    }
}
