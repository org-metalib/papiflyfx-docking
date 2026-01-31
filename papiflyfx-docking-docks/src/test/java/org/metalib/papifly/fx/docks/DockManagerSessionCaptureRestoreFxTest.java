package org.metalib.papifly.fx.docks;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.SplitData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationExtension.class)
class DockManagerSessionCaptureRestoreFxTest {

    private DockManager dockManager;

    @Start
    private void start(Stage stage) {
        dockManager = new DockManager();
        stage.setScene(new Scene((Region) dockManager.getRootPane(), 800, 600));
        stage.show();
    }

    @Test
    void restoreThenCapture_yieldsStructurallyEquivalentLayout(FxRobot robot) {
        LayoutNode layout = new SplitData(
            "split-1",
            Orientation.VERTICAL,
            0.6,
            new TabGroupData(
                "tabs-1",
                List.of(
                    LeafData.of("leaf-1", "Leaf 1"),
                    LeafData.of("leaf-2", "Leaf 2")
                ),
                0
            ),
            new TabGroupData(
                "tabs-2",
                List.of(LeafData.of("leaf-3", "Leaf 3")),
                0
            )
        );

        DockSessionData session = DockSessionData.of(layout, List.of(), List.of(), null);

        robot.interact(() -> dockManager.restoreSession(session));
        DockSessionData captured = FxTestUtil.callFx(dockManager::captureSession);

        assertEquals(session, captured);
    }
}
