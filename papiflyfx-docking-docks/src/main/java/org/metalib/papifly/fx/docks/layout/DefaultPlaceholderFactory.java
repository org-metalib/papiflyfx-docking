package org.metalib.papifly.fx.docks.layout;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;

final class DefaultPlaceholderFactory implements PlaceholderFactory {

    @Override
    public Node createPlaceholder(LeafData data, LeafContentData contentData) {
        if (contentData != null) {
            String typeKey = contentData.typeKey() != null ? contentData.typeKey() : "unknown";
            String contentId = contentData.contentId();
            String labelText = contentId != null
                ? "Missing content: " + typeKey + " (" + contentId + ")"
                : "Missing content: " + typeKey;
            return new Label(labelText);
        }

        String labelText = data.title() != null
            ? "Missing content: " + data.title() + " (" + data.id() + ")"
            : "Missing content: " + data.id();
        return new Label(labelText);
    }
}
