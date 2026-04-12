package org.metalib.papifly.fx.settings.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.categories.KeyboardShortcutsCategory;
import org.metalib.papifly.fx.settings.categories.McpServersCategory;
import org.metalib.papifly.fx.settings.categories.SecurityCategory;
import org.metalib.papifly.fx.settings.persist.JsonSettingsStorage;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.secret.InMemorySecretStore;
import org.metalib.papifly.fx.settings.ui.controls.NumberSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.PathSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.SecretSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.StringSettingControl;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class SettingsPanelFxTest {

    @TempDir
    Path tempDir;

    private SettingsRuntime runtime;
    private SettingsPanel shownPanel;

    @Start
    void start(Stage stage) {
        runtime = new SettingsRuntime(
            tempDir.resolve("app"),
            tempDir.resolve("workspace"),
            new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace")),
            new InMemorySecretStore(),
            new SimpleObjectProperty<>(Theme.dark())
        );
        shownPanel = new SettingsPanel(runtime);
        stage.setScene(new Scene(shownPanel, 960, 720));
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

    @Test
    void selectedCategoryUsesExplicitInactiveTokensWhenListLosesFocus() {
        SettingsCategoryList list = callFx(this::categoryList);
        Theme theme = Theme.dark();
        Color expectedText = UiCommonThemeSupport.textPrimary(theme);
        Color expectedFocusedBackground = UiCommonThemeSupport.alpha(UiCommonThemeSupport.accent(theme), 0.16);
        Color expectedInactiveBackground = UiCommonThemeSupport.alpha(UiCommonThemeSupport.accent(theme), 0.10);

        runFx(() -> {
            shownPanel.selectCategory("appearance");
            list.requestFocus();
            shownPanel.applyCss();
            shownPanel.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callFx(list::isFocused));
        ListCell<?> focusedCell = selectedCategoryCell();
        Label focusedLabel = selectedCategoryLabel(focusedCell);
        assertColorEquals(expectedText, callFx(() -> requireColor(focusedLabel.getTextFill())));
        assertColorEquals(expectedFocusedBackground, callFx(() -> backgroundColor(focusedCell)));

        runFx(() -> {
            shownPanel.searchBar().getSearchField().requestFocus();
            shownPanel.applyCss();
            shownPanel.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(callFx(list::isFocused));
        assertTrue(callFx(() -> shownPanel.searchBar().getSearchField().isFocused()));
        ListCell<?> inactiveCell = selectedCategoryCell();
        Label inactiveLabel = selectedCategoryLabel(inactiveCell);
        assertColorEquals(expectedText, callFx(() -> requireColor(inactiveLabel.getTextFill())));
        assertColorEquals(expectedInactiveBackground, callFx(() -> backgroundColor(inactiveCell)));
    }

    @Test
    void settingsEditorsReuseSharedCompactFieldStyleClass() {
        CompactFieldAudit audit = callFx(() -> {
            List<Node> roots = List.of(
                shownPanel.searchBar(),
                new StringSettingControl(SettingDefinition.of("test.string", "String", SettingType.STRING, "")),
                new NumberSettingControl<>(SettingDefinition.of("test.number", "Number", SettingType.INTEGER, 1)),
                new PathSettingControl(SettingDefinition.of("test.path", "Path", SettingType.FILE_PATH, "")),
                new SecretSettingControl(SettingDefinition.of("test.secret", "Secret", SettingType.SECRET, "")),
                new KeyboardShortcutsCategory().buildSettingsPane(runtime.context(SettingScope.APPLICATION)),
                new McpServersCategory().buildSettingsPane(runtime.context(SettingScope.WORKSPACE)),
                new SecurityCategory().buildSettingsPane(runtime.context(SettingScope.APPLICATION))
            );

            List<TextInputControl> fields = new ArrayList<>();
            for (Node root : roots) {
                fields.addAll(collectNodes(root, TextInputControl.class));
            }
            List<String> missing = fields.stream()
                .filter(field -> !field.getStyleClass().contains(SettingsUiStyles.COMPACT_FIELD))
                .map(this::describeField)
                .toList();
            return new CompactFieldAudit(fields.size(), missing);
        });

        assertTrue(audit.fieldCount() >= 12, () -> "Expected multiple compact fields but found " + audit.fieldCount());
        assertTrue(audit.missing().isEmpty(), () -> "Inputs missing compact field styling: " + audit.missing());
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

    private SettingsCategoryList categoryList() {
        SettingsCategoryList list = (SettingsCategoryList) shownPanel.lookup(".pf-settings-category-list");
        assertNotNull(list);
        return list;
    }

    private ListCell<?> selectedCategoryCell() {
        return callFx(() -> {
            shownPanel.applyCss();
            shownPanel.layout();
            SettingsCategoryList list = categoryList();
            list.applyCss();
            list.layout();
            return list.lookupAll(".list-cell").stream()
                .filter(ListCell.class::isInstance)
                .map(ListCell.class::cast)
                .filter(ListCell::isSelected)
                .filter(cell -> cell.getGraphic() != null)
                .findFirst()
                .orElseThrow();
        });
    }

    private Label selectedCategoryLabel(ListCell<?> cell) {
        return callFx(() -> {
            cell.applyCss();
            Label label = (Label) cell.lookup(".pf-settings-category-label");
            assertNotNull(label);
            return label;
        });
    }

    private <T> List<T> collectNodes(Node root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collectNodes(root, type, matches);
        return matches;
    }

    private <T> void collectNodes(Node node, Class<T> type, List<T> matches) {
        if (type.isInstance(node)) {
            matches.add(type.cast(node));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectNodes(child, type, matches);
            }
        }
    }

    private String describeField(TextInputControl field) {
        String prompt = field.getPromptText();
        if (prompt != null && !prompt.isBlank()) {
            return field.getClass().getSimpleName() + "[" + prompt + "]";
        }
        return field.getClass().getSimpleName();
    }

    private static Color backgroundColor(Region region) {
        Background background = region.getBackground();
        assertNotNull(background);
        assertFalse(background.getFills().isEmpty());
        assertTrue(background.getFills().getFirst().getFill() instanceof Color);
        return (Color) background.getFills().getFirst().getFill();
    }

    private static Color requireColor(Paint paint) {
        assertTrue(paint instanceof Color);
        return (Color) paint;
    }

    private static void assertColorEquals(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), 0.01);
        assertEquals(expected.getGreen(), actual.getGreen(), 0.01);
        assertEquals(expected.getBlue(), actual.getBlue(), 0.01);
        assertEquals(expected.getOpacity(), actual.getOpacity(), 0.01);
    }

    private record CompactFieldAudit(int fieldCount, List<String> missing) {
    }
}
