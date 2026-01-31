package org.metalib.papifly.fx.docks.layout;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.theme.Theme;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

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
}
