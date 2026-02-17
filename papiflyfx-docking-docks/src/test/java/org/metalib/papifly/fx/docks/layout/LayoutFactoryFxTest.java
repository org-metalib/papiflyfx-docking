package org.metalib.papifly.fx.docks.layout;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.theme.Theme;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
class LayoutFactoryFxTest {

    @Start
    void start(Stage stage) {
        // Intentionally empty: ApplicationExtension ensures JavaFX toolkit is initialized.
    }

    @Test
    void build_leaf_usesContentFactoryWhenProvided() {
        var themeProperty = new SimpleObjectProperty<>(Theme.dark());
        ContentFactory contentFactory = id -> new Label("content:" + id);
        LayoutFactory factory = new LayoutFactory(themeProperty, contentFactory);

        DockElement element = factory.build(LeafData.of("leaf-1", "Leaf 1", "factoryA"));
        assertInstanceOf(DockTabGroup.class, element);

        DockTabGroup group = (DockTabGroup) element;
        assertEquals(1, group.getTabs().size());
        var leaf = group.getTabs().getFirst();
        assertNotNull(leaf.getContent());
        assertInstanceOf(Label.class, leaf.getContent());
        assertEquals("content:factoryA", ((Label) leaf.getContent()).getText());
    }

    @Test
    void build_leaf_fallsToFactoryWhenAdapterMissing() {
        var themeProperty = new SimpleObjectProperty<>(Theme.dark());
        ContentFactory contentFactory = id -> new Label("factory:" + id);
        LayoutFactory factory = new LayoutFactory(themeProperty, contentFactory);

        // ContentData with a typeKey that has no registered adapter
        LeafContentData contentData = new LeafContentData(
            "unknown-type", "c1", 1, Map.of()
        );
        LeafData leafData = LeafData.of("leaf-2", "Leaf 2", "myFactory", contentData);

        DockElement element = factory.build(leafData);
        assertInstanceOf(DockTabGroup.class, element);

        DockTabGroup group = (DockTabGroup) element;
        var leaf = group.getTabs().getFirst();
        assertNotNull(leaf.getContent());
        // Factory should have been called since adapter is absent
        assertInstanceOf(Label.class, leaf.getContent());
        assertEquals("factory:myFactory", ((Label) leaf.getContent()).getText());
    }
}
