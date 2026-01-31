package org.metalib.papifly.fx.docks;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(ApplicationExtension.class)
class DockManagerCloseLeafFxTest {

    private DockManager dockManager;

    @Start
    private void start(Stage stage) {
        dockManager = new DockManager();
        stage.setScene(new Scene((Region) dockManager.getRootPane(), 800, 600));
        stage.show();
    }

    @Test
    void closingOnlyLeafInTabGroup_clearsRootAndDisposesLeaf() {
        DockLeaf leaf = FxTestUtil.callFx(() -> dockManager.createLeaf("Leaf", new Label("Content")));
        DockTabGroup tabs = FxTestUtil.callFx(dockManager::createTabGroup);
        FxTestUtil.runFx(() -> {
            tabs.addLeaf(leaf);
            dockManager.setRoot(tabs);
        });

        assertNotNull(FxTestUtil.callFx(dockManager::getRoot));
        assertNotNull(FxTestUtil.callFx(leaf::getContent));

        FxTestUtil.runFx(leaf::requestClose);

        assertNull(FxTestUtil.callFx(dockManager::getRoot));
        assertNull(FxTestUtil.callFx(leaf::getContent));
    }

    @Test
    void closingLeafInSplit_collapsesSplitAndKeepsSibling() {
        DockLeaf leftLeaf = FxTestUtil.callFx(() -> dockManager.createLeaf("Left", new Label("Left")));
        DockLeaf rightLeaf = FxTestUtil.callFx(() -> dockManager.createLeaf("Right", new Label("Right")));
        DockTabGroup leftTabs = FxTestUtil.callFx(dockManager::createTabGroup);
        DockTabGroup rightTabs = FxTestUtil.callFx(dockManager::createTabGroup);

        FxTestUtil.runFx(() -> {
            leftTabs.addLeaf(leftLeaf);
            rightTabs.addLeaf(rightLeaf);
            DockSplitGroup split = dockManager.createHorizontalSplit(leftTabs, rightTabs, 0.5);
            dockManager.setRoot(split);
        });

        FxTestUtil.runFx(leftLeaf::requestClose);

        assertSame(rightTabs, FxTestUtil.callFx(dockManager::getRoot));
        assertSame(rightTabs, FxTestUtil.callFx(rightLeaf::getParent));
        assertNotNull(FxTestUtil.callFx(rightLeaf::getContent));
    }
}
