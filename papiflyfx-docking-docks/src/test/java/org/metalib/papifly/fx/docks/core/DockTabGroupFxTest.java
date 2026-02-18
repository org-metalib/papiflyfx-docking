package org.metalib.papifly.fx.docks.core;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.metalib.papifly.fx.docking.api.Theme;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class DockTabGroupFxTest {

    SimpleObjectProperty<Theme> themeProperty;

    @Start
    void start(Stage stage) {
        themeProperty = new SimpleObjectProperty<>(Theme.dark());
    }

    @Test
    void removeActiveTab_movesActiveIndexDeterministically() {
        DockTabGroup group = FxTestUtil.callFx(() -> new DockTabGroup("tabs", themeProperty));

        DockLeaf leaf1 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("l1", "L1")));
        DockLeaf leaf2 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("l2", "L2")));
        DockLeaf leaf3 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("l3", "L3")));

        FxTestUtil.runFx(() -> {
            group.addLeaf(leaf1);
            group.addLeaf(leaf2);
            group.addLeaf(leaf3);
            group.setActiveTab(1);
        });

        assertEquals(1, FxTestUtil.callFx(group::getActiveTabIndex));

        FxTestUtil.runFx(() -> group.removeLeaf(leaf2));

        assertEquals(0, FxTestUtil.callFx(group::getActiveTabIndex));
        assertSame(leaf1, FxTestUtil.callFx(group::getActiveTab));
    }

    @Test
    void removeNonActiveTab_keepsSameLeafActive() {
        DockTabGroup group = FxTestUtil.callFx(() -> new DockTabGroup("tabs", themeProperty));

        DockLeaf leaf1 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("l1", "L1")));
        DockLeaf leaf2 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("l2", "L2")));
        DockLeaf leaf3 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("l3", "L3")));

        FxTestUtil.runFx(() -> {
            group.addLeaf(leaf1);
            group.addLeaf(leaf2);
            group.addLeaf(leaf3);
            group.setActiveTab(2);
        });

        assertSame(leaf3, FxTestUtil.callFx(group::getActiveTab));

        FxTestUtil.runFx(() -> group.removeLeaf(leaf2));

        assertSame(leaf3, FxTestUtil.callFx(group::getActiveTab));
        assertEquals(1, FxTestUtil.callFx(group::getActiveTabIndex));
    }

    @Test
    void removeFirstActiveTab_displaysRemainingTabContent() {
        DockTabGroup group = FxTestUtil.callFx(() -> new DockTabGroup("tabs", themeProperty));

        Label content1 = new Label("Editor 2 Content");
        Label content2 = new Label("Files Content");

        DockLeaf leaf1 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("editor2", "Editor 2")).content(content1));
        DockLeaf leaf2 = FxTestUtil.callFx(() -> new DockLeaf(DockData.of("files", "Files")).content(content2));

        FxTestUtil.runFx(() -> {
            group.addLeaf(leaf1);
            group.addLeaf(leaf2);
            group.setActiveTab(0);
        });

        // Verify initial state: Editor 2 is active and its content is displayed
        assertEquals(0, FxTestUtil.callFx(group::getActiveTabIndex));
        assertSame(leaf1, FxTestUtil.callFx(group::getActiveTab));

        // Remove the first active tab (Editor 2)
        FxTestUtil.runFx(() -> group.removeLeaf(leaf1));

        // Verify Files tab becomes active at index 0 and its content is displayed
        assertEquals(0, FxTestUtil.callFx(group::getActiveTabIndex));
        assertSame(leaf2, FxTestUtil.callFx(group::getActiveTab));

        // This is the critical assertion: Files content should be visible in the content area
        // We check if content2 is present in the scene graph under the group's node
        boolean contentVisible = FxTestUtil.callFx(() -> {
            return isNodeInTree(group.getNode(), content2);
        });
        assertTrue(contentVisible, "Files content should be visible after removing Editor 2");
    }

    /**
     * Recursively checks if a node exists in the scene graph tree.
     */
    private boolean isNodeInTree(javafx.scene.Node root, javafx.scene.Node target) {
        if (root == target) {
            return true;
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                if (isNodeInTree(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }
}
