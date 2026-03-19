package org.metalib.papifly.fx.settings.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.persist.JsonSettingsStorage;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.secret.InMemorySecretStore;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class SettingsPanelFxTest {

    @TempDir
    Path tempDir;

    private SettingsRuntime runtime;

    @Start
    void start(Stage stage) {
        runtime = new SettingsRuntime(
            tempDir.resolve("app"),
            tempDir.resolve("workspace"),
            new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace")),
            new InMemorySecretStore(),
            new SimpleObjectProperty<>(Theme.dark())
        );
        stage.setScene(new Scene(new SettingsPanel(runtime), 960, 720));
        stage.show();
    }

    @Test
    void searchFiltersVisibleCategories() {
        SettingsPanel panel = callFx(() -> new SettingsPanel(runtime));

        runFx(() -> panel.searchBar().getSearchField().setText("layout preset"));
        List<String> visible = callFx(panel::visibleCategoryIds);

        assertEquals(List.of("workspace"), visible);
    }

    @Test
    void applyAndResetUpdateStorageAndTheme() {
        SettingsPanel panel = callFx(() -> new SettingsPanel(runtime));

        runFx(() -> {
            panel.selectCategory("appearance");
            panel.searchBar().getSearchField().clear();
            panel.applyActiveCategory();
        });

        assertTrue(callFx(() ->
            runtime.storage().getString(SettingScope.APPLICATION, "appearance.theme", "dark").equals("dark")
        ));
    }

    private void runFx(Runnable action) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        future.join();
    }

    private <T> T callFx(java.util.concurrent.Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future.join();
    }
}
