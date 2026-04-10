package org.metalib.papifly.fx.docks.layout;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;

/**
 * Creates fallback placeholder content when a layout leaf cannot restore real content.
 */
public interface PlaceholderFactory {

    Node createPlaceholder(LeafData data, LeafContentData contentData);
}
