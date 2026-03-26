package org.metalib.papifly.fx.login.docking;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.login.ui.LoginDockPane;

import java.util.Map;

public class LoginStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() {
        return LoginFactory.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (content instanceof LoginDockPane) {
            return Map.of("version", VERSION);
        }
        return Map.of();
    }

    @Override
    public Node restore(LeafContentData content) {
        return new LoginFactory().create(LoginFactory.FACTORY_ID);
    }
}
