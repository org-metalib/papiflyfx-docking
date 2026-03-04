package org.metalib.papifly.fx.hugo.api;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.FxTestUtil;
import org.metalib.papifly.fx.hugo.process.HugoCliProbe;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class HugoPreviewPaneFxTest {

    private StackPane root;

    @Start
    void start(Stage stage) {
        root = new StackPane();
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @Test
    void paneRendersToolbarAndWebView(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20110, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(pane));

        assertNotNull(FxTestUtil.callFx(() -> pane.lookup("#hugo-preview-toolbar")));
        assertNotNull(FxTestUtil.callFx(() -> pane.lookup("#hugo-preview-webview")));
        assertNotNull(FxTestUtil.callFx(() -> pane.lookup("#hugo-preview-status")));

        FxTestUtil.runFx(pane::dispose);
    }

    @Test
    void startAndStopUpdateStatusAndUrl(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20120, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(pane));

        FxTestUtil.runFx(() -> pane.startServerAndLoad("/docs/"));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> {
                Label state = (Label) pane.lookup("#hugo-preview-status-state");
                return state != null && state.getText().startsWith("Running");
            }));

        Hyperlink address = FxTestUtil.callFx(() -> (Hyperlink) pane.lookup("#hugo-preview-address"));
        assertNotNull(address);
        assertTrue(address.getText().contains("http://127.0.0.1:"));

        FxTestUtil.runFx(pane::stopServer);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> {
                Label state = (Label) pane.lookup("#hugo-preview-status-state");
                return state != null && "Stopped".equals(state.getText());
            }));

        WebView webView = FxTestUtil.callFx(() -> (WebView) pane.lookup("#hugo-preview-webview"));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> "about:blank".equals(webView.getEngine().getLocation())));

        FxTestUtil.runFx(pane::dispose);
    }

    @Test
    void themeSwitchUpdatesToolbarBackground(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20130, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(pane));

        ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.dark());
        FxTestUtil.runFx(() -> pane.bindThemeProperty(theme));

        Paint darkPaint = FxTestUtil.callFx(() -> ((Region) pane.lookup("#hugo-preview-toolbar"))
            .getBackground().getFills().getFirst().getFill());
        FxTestUtil.runFx(() -> theme.set(Theme.light()));
        FxTestUtil.waitForFx();
        Paint lightPaint = FxTestUtil.callFx(() -> ((Region) pane.lookup("#hugo-preview-toolbar"))
            .getBackground().getFills().getFirst().getFill());

        assertTrue(!asColor(darkPaint).equals(asColor(lightPaint)));

        FxTestUtil.runFx(() -> {
            pane.unbindThemeProperty();
            pane.dispose();
        });
    }

    private HugoPreviewPane createPane(Path siteRoot, int preferredPort, boolean autoStart) throws Exception {
        Path script = writeReadyScript(siteRoot.resolve("hugo-ready.sh"));
        HugoCliProbe probe = new HugoCliProbe(List.of("sh", "-c", "echo hugo"));
        HugoServerProcessManager manager = new HugoServerProcessManager(
            (rootPath, port, options) -> List.of("sh", script.toString(), Integer.toString(port)),
            Duration.ofSeconds(2)
        );

        return FxTestUtil.callFx(() -> new HugoPreviewPane(
            new HugoPreviewConfig(siteRoot, "hugo:test", "/", preferredPort, autoStart, false),
            probe,
            manager
        ));
    }

    private Path writeReadyScript(Path path) throws Exception {
        Files.writeString(path, """
            #!/bin/sh
            PORT="$1"
            echo "Web Server is available at http://127.0.0.1:${PORT}/"
            while true; do
              sleep 1
            done
            """);
        assertTrue(path.toFile().setExecutable(true));
        return path;
    }

    private Color asColor(Paint paint) {
        return paint instanceof Color color ? color : Color.TRANSPARENT;
    }
}
