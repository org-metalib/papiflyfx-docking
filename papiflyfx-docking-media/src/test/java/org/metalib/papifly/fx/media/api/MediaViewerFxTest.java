package org.metalib.papifly.fx.media.api;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class MediaViewerFxTest {

    private MediaViewer viewer;

    @Start
    void start(Stage stage) {
        viewer = new MediaViewer();
        stage.setScene(new Scene(viewer, 400, 300));
        stage.show();
    }

    @Test
    void createsWithoutError() {
        assertNotNull(viewer);
    }

    @Test
    void loadUnknownFormatShowsError(FxRobot robot) {
        robot.interact(() -> viewer.loadMedia("file:///unknown.xyz"));
        robot.interact(() -> assertFalse(viewer.getChildren().isEmpty()));
    }

    @Test
    void disposeDoesNotThrow(FxRobot robot) {
        robot.interact(() -> viewer.loadMedia("file:///sample.png"));
        assertDoesNotThrow(() -> robot.interact(() -> viewer.dispose()));
    }
}
