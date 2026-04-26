package org.metalib.papifly.fx.samples.docks;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.docking.SettingsStateAdapter;
import org.metalib.papifly.fx.settings.persist.JsonSettingsStorage;
import org.metalib.papifly.fx.settings.runtime.DefaultSettingsServicesProvider;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.secret.InMemorySecretStore;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class RibbonShellSampleIntegrationFxTest {

    @TempDir
    Path tempDir;

    @Start
    void start(Stage stage) {
        SimpleObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());
        SettingsRuntime runtime = new SettingsRuntime(
            tempDir.resolve("app"),
            tempDir.resolve("workspace"),
            new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace")),
            new InMemorySecretStore(),
            themeProperty
        );
        SettingsStateAdapter.setSharedRuntime(runtime);
        DefaultSettingsServicesProvider.setSharedRuntime(runtime);

        RibbonShellSample sample = new RibbonShellSample();
        Node content = sample.build(stage, themeProperty);
        stage.setScene(new Scene(new StackPane(content), 1280, 760));
        stage.show();
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void discoversGitHubAndHugoProvidersAndTogglesContextualHugoEditorTab(FxRobot robot) {
        assertTrue(hasTab(robot, "GitHub"));
        assertTrue(hasTab(robot, "Hugo"));
        assertTrue(hasTab(robot, "Hugo Editor"));

        robot.clickOn("Landing.java");
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(hasTab(robot, "Hugo Editor"));

        robot.clickOn("Post.md");
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(hasTab(robot, "Hugo Editor"));
    }

    private static boolean hasTab(FxRobot robot, String label) {
        return robot.lookup(node ->
            node instanceof ToggleButton toggleButton
                && label.equals(toggleButton.getText())
                && toggleButton.isVisible()
        ).tryQuery().isPresent();
    }
}
