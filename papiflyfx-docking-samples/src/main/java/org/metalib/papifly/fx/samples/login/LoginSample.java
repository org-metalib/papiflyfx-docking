package org.metalib.papifly.fx.samples.login;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.login.api.AuthSessionBroker;
import org.metalib.papifly.fx.login.docking.LoginFactory;
import org.metalib.papifly.fx.login.docking.LoginStateAdapter;
import org.metalib.papifly.fx.login.idapi.ProviderRegistry;
import org.metalib.papifly.fx.login.runtime.LoginRuntime;
import org.metalib.papifly.fx.login.ui.LoginDockPane;
import org.metalib.papifly.fx.samples.SampleScene;
import org.metalib.papifly.fx.samples.SamplesRuntimeSupport;

public class LoginSample implements SampleScene {

    @Override
    public String category() {
        return "Login";
    }

    @Override
    public String title() {
        return "Login Panel";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        AuthSessionBroker broker = LoginRuntime.broker();
        ProviderRegistry registry = SamplesRuntimeSupport.loginProviderRegistry();

        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        ContentStateRegistry stateRegistry = new ContentStateRegistry();
        stateRegistry.register(new LoginStateAdapter());
        dockManager.setContentStateRegistry(stateRegistry);
        dockManager.setContentFactory(new LoginFactory(broker, registry));

        LoginDockPane loginPane = new LoginDockPane(broker, registry);
        DockLeaf leaf = dockManager.createLeaf("Login", loginPane);
        leaf.setContentFactoryId(LoginFactory.FACTORY_ID);
        leaf.setContentData(LeafContentData.of(
            LoginFactory.FACTORY_ID,
            "login:main",
            LoginStateAdapter.VERSION
        ));

        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(leaf);
        dockManager.setRoot(group);
        return dockManager.getRootPane();
    }
}
