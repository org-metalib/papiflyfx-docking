package org.metalib.papifly.fx.settings.docking;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.ui.SettingsPanel;

import java.util.Map;

public class SettingsStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() {
        return SettingsContentFactory.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (content instanceof SettingsPanel panel && panel.getActiveCategoryId() != null) {
            return Map.of("activeCategory", panel.getActiveCategoryId());
        }
        return Map.of();
    }

    @Override
    public Node restore(LeafContentData content) {
        String activeCategory = null;
        if (content != null && content.state() != null) {
            Object value = content.state().get("activeCategory");
            if (value != null) {
                activeCategory = String.valueOf(value);
            }
        }
        return new SettingsPanel(SettingsRuntime.getDefault(), activeCategory);
    }
}
