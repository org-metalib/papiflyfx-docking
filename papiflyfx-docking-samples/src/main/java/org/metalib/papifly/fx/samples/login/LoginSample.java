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

import java.util.List;
import java.util.Objects;

public class LoginSample implements SampleScene {

    private final String sampleTitle;
    private final List<String> visibleProviderIds;

    public LoginSample() {
        this("Sign in with Google", "google");
    }

    public LoginSample(String sampleTitle, String providerId) {
        this(sampleTitle, List.of(Objects.requireNonNull(providerId, "providerId")));
    }

    private LoginSample(String sampleTitle, List<String> visibleProviderIds) {
        this.sampleTitle = Objects.requireNonNull(sampleTitle, "sampleTitle");
        this.visibleProviderIds = List.copyOf(visibleProviderIds);
    }

    @Override
    public String category() {
        return "Login";
    }

    @Override
    public String title() {
        return sampleTitle;
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        AuthSessionBroker broker = LoginRuntime.broker();
        ProviderRegistry runtimeRegistry = LoginRuntime.providerRegistry();
        ProviderRegistry displayRegistry = displayRegistry(runtimeRegistry);

        DockManager dockManager = new DockManager();
        dockManager.themeProperty().bind(themeProperty);
        dockManager.setOwnerStage(ownerStage);

        ContentStateRegistry stateRegistry = new ContentStateRegistry();
        stateRegistry.register(new LoginStateAdapter());
        dockManager.setContentStateRegistry(stateRegistry);
        dockManager.setContentFactory(new LoginFactory(broker, displayRegistry));

        LoginDockPane loginPane = new LoginDockPane(broker, displayRegistry);
        DockLeaf leaf = dockManager.createLeaf(sampleTitle, loginPane);
        leaf.setContentFactoryId(LoginFactory.FACTORY_ID);
        leaf.setContentData(LeafContentData.of(
            LoginFactory.FACTORY_ID,
            contentId(),
            LoginStateAdapter.VERSION
        ));

        DockTabGroup group = dockManager.createTabGroup();
        group.addLeaf(leaf);
        dockManager.setRoot(group);
        return dockManager.getRootPane();
    }

    private ProviderRegistry displayRegistry(ProviderRegistry runtimeRegistry) {
        if (visibleProviderIds.isEmpty()) {
            return runtimeRegistry;
        }
        ProviderRegistry filteredRegistry = new ProviderRegistry();
        for (String providerId : visibleProviderIds) {
            runtimeRegistry.get(providerId)
                .ifPresent(filteredRegistry::register);
        }
        return filteredRegistry.descriptors().isEmpty() ? runtimeRegistry : filteredRegistry;
    }

    private String contentId() {
        if (visibleProviderIds.isEmpty()) {
            return "login:main";
        }
        return "login:" + String.join("-", visibleProviderIds);
    }
}
