package org.metalib.papifly.fx.samples.settings;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SampleScene;
import org.metalib.papifly.fx.samples.SamplesRuntimeSupport;
import org.metalib.papifly.fx.settings.docking.SettingsContentFactory;
import org.metalib.papifly.fx.settings.docking.SettingsStateAdapter;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.ui.SettingsPanel;

public class SettingsPanelSample implements SampleScene {

    @Override
    public String category() {
        return "Settings";
    }

    @Override
    public String title() {
        return "Settings Panel";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        SettingsRuntime runtime = SamplesRuntimeSupport.settingsRuntime(themeProperty);

        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new SettingsStateAdapter());
        dockManager.setContentStateRegistry(registry);
        dockManager.setContentFactory(new SettingsContentFactory(runtime));

        DockLeaf leaf = dockManager.createLeaf("Settings", new SettingsPanel(runtime));
        leaf.setContentFactoryId(SettingsContentFactory.FACTORY_ID);
        leaf.setContentData(LeafContentData.of(
            SettingsContentFactory.FACTORY_ID,
            "settings:main",
            SettingsStateAdapter.VERSION
        ));

        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(leaf);
        dockManager.setRoot(group);
        return dockManager.getRootPane();
    }
}
