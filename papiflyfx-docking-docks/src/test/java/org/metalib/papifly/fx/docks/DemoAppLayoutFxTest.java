package org.metalib.papifly.fx.docks;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.metalib.papifly.fx.docking.api.Theme;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
class DemoAppLayoutFxTest {

    private DockManager dockManager;

    @Start
    private void start(Stage stage) {
        dockManager = new DockManager(Theme.dark());
        stage.setScene(new Scene((Region) dockManager.getRootPane(), 1200, 800));
        stage.show();
    }

    @Test
    void initialDemoLayout_hasExpectedDockTreeAndDividerPositions() {
        DockElement layout = FxTestUtil.callFx(() -> DemoApp.createInitialLayout(dockManager));
        FxTestUtil.runFx(() -> dockManager.setRoot(layout));

        DockElement root = FxTestUtil.callFx(dockManager::getRoot);
        assertNotNull(root);
        assertInstanceOf(DockSplitGroup.class, root);

        DockSplitGroup fullLayout = (DockSplitGroup) root;
        assertEquals(Orientation.HORIZONTAL, fullLayout.getOrientation());
        assertEquals(0.8, fullLayout.getDividerPosition(), 0.0001);

        assertInstanceOf(DockSplitGroup.class, fullLayout.getFirst());
        assertInstanceOf(DockTabGroup.class, fullLayout.getSecond());

        DockSplitGroup mainArea = (DockSplitGroup) fullLayout.getFirst();
        DockTabGroup properties = (DockTabGroup) fullLayout.getSecond();

        assertEquals(Orientation.HORIZONTAL, mainArea.getOrientation());
        assertEquals(0.2, mainArea.getDividerPosition(), 0.0001);
        assertEquals("Properties", properties.getTabs().getFirst().getMetadata().title());

        assertInstanceOf(DockTabGroup.class, mainArea.getFirst());
        assertInstanceOf(DockSplitGroup.class, mainArea.getSecond());

        DockTabGroup files = (DockTabGroup) mainArea.getFirst();
        DockSplitGroup centerArea = (DockSplitGroup) mainArea.getSecond();

        assertEquals("Files", files.getTabs().getFirst().getMetadata().title());
        assertEquals(Orientation.VERTICAL, centerArea.getOrientation());
        assertEquals(0.7, centerArea.getDividerPosition(), 0.0001);

        assertInstanceOf(DockTabGroup.class, centerArea.getFirst());
        assertInstanceOf(DockTabGroup.class, centerArea.getSecond());

        DockTabGroup editorTabs = (DockTabGroup) centerArea.getFirst();
        DockTabGroup console = (DockTabGroup) centerArea.getSecond();
        assertEquals("Console", console.getTabs().getFirst().getMetadata().title());

        List<String> editorTitles = editorTabs.getTabs().stream()
            .map(t -> t.getMetadata().title())
            .toList();
        assertEquals(List.of("Editor 1", "Editor 2"), editorTitles);
    }
}
