package org.metalib.papifly.fx.samples.github;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.docks.ribbon.RibbonDockHost;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.SamplesRuntimeSupport;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class GitHubRibbonSampleFxTest {

    private RibbonDockHost host;

    @Start
    void start(Stage stage) {
        SimpleObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());
        System.setProperty(
            "papiflyfx.app.dir",
            Path.of(System.getProperty("java.io.tmpdir"), "papiflyfx-github-ribbon-test-" + System.nanoTime()).toString()
        );
        SamplesRuntimeSupport.resetForTests();
        SamplesRuntimeSupport.initialize(themeProperty);

        GitHubRibbonSample sample = new GitHubRibbonSample();
        Node content = sample.build(stage, themeProperty);
        host = (RibbonDockHost) content;
        stage.setScene(new Scene(new StackPane(content), 1280, 760));
        stage.show();
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void showsGitHubProviderTab(FxRobot robot) {
        assertTrue(host.getRibbonManager().hasTab("github"));
        assertTrue(hasTab(robot, "GitHub"));
    }

    @Test
    void fetchCommandUpdatesSampleStatus() {
        runFx(() -> {
            RibbonCommand command = host.getRibbonManager()
                .getCommandRegistry()
                .find("github.ribbon.fetch")
                .orElseThrow();
            assertTrue(command.enabled().get(), "Fetch command should be enabled for sample content");
            command.execute();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Fetch: Fetched sample remote references", statusText());
    }

    private String statusText() {
        AtomicReference<String> text = new AtomicReference<>();
        runFx(() -> {
            Node node = host.lookup("#" + GitHubRibbonSample.STATUS_LABEL_ID);
            assertNotNull(node, "Sample status label should be present");
            text.set(((Label) node).getText());
        });
        return text.get();
    }

    private static boolean hasTab(FxRobot robot, String label) {
        return robot.lookup(node ->
            node instanceof ToggleButton toggleButton
                && label.equals(toggleButton.getText())
                && toggleButton.isVisible()
        ).tryQuery().isPresent();
    }

    private static void runFx(Runnable action) {
        try {
            WaitForAsyncUtils.asyncFx(action).get();
        } catch (Exception ex) {
            throw new AssertionError("FX action failed", ex);
        }
    }
}
