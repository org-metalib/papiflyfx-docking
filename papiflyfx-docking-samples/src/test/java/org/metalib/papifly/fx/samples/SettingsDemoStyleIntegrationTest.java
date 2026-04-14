package org.metalib.papifly.fx.samples;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
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
import org.metalib.papifly.fx.settings.ui.SettingsPanel;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class SettingsDemoStyleIntegrationTest {

    private static final String CATEGORY_LIST_STYLE = "pf-settings-category-list";

    @TempDir
    Path tempDir;

    private SettingsPanel shownPanel;

    @Start
    void start(Stage stage) {
        SettingsRuntime runtime = new SettingsRuntime(
            tempDir.resolve("app"),
            tempDir.resolve("workspace"),
            new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace")),
            new InMemorySecretStore(),
            new SimpleObjectProperty<>(Theme.dark())
        );
        SettingsStateAdapter.setSharedRuntime(runtime);
        DefaultSettingsServicesProvider.setSharedRuntime(runtime);
        shownPanel = new SettingsPanel(runtime);
        stage.setScene(new Scene(shownPanel, 1024, 760));
        stage.show();
    }

    @Test
    void settingsDemoControlsUseSharedTokenDrivenStyleClasses() {
        StyleAudit audit = callFx(() -> {
            List<String> requiredCategories = List.of(
                "appearance",
                "authentication",
                "security",
                "github",
                "editor",
                "hugo",
                "profiles",
                "workspace",
                "mcp-servers"
            );
            List<String> visibleIds = shownPanel.visibleCategoryIds();
            List<String> missingCategories = requiredCategories.stream()
                .filter(id -> !visibleIds.contains(id))
                .toList();

            Set<Node> roots = Collections.newSetFromMap(new IdentityHashMap<>());
            roots.add(shownPanel);
            for (String categoryId : requiredCategories) {
                shownPanel.selectCategory(categoryId);
                shownPanel.applyCss();
                shownPanel.layout();
                Parent contentArea = (Parent) shownPanel.lookup(".pf-settings-content-area");
                roots.addAll(contentArea.getChildrenUnmodifiable());
            }

            List<TextInputControl> fields = collectNodes(roots, TextInputControl.class).stream()
                .filter(field -> !(field instanceof TextArea))
                .toList();
            List<ColorPicker> colorPickers = collectNodes(roots, ColorPicker.class);
            List<ComboBox> combos = collectNodes(roots, ComboBox.class);
            List<TextArea> textAreas = collectNodes(roots, TextArea.class);
            List<CheckBox> checkBoxes = collectNodes(roots, CheckBox.class);
            List<ListView> listViews = collectNodes(roots, ListView.class).stream()
                .filter(listView -> !hasAncestor(listView, ComboBox.class))
                .toList();
            List<Button> buttons = collectNodes(roots, Button.class);

            return new StyleAudit(
                missingCategories,
                fields.stream().filter(field -> !field.getStyleClass().contains("pf-ui-compact-field")).map(this::describeNode).toList(),
                colorPickers.stream().filter(colorPicker ->
                    !colorPicker.getStyleClass().contains("pf-ui-compact-field")
                        || !colorPicker.getStyleClass().contains("pf-settings-combo-box")
                ).map(this::describeNode).toList(),
                combos.stream().filter(combo ->
                    !combo.getStyleClass().contains("pf-ui-compact-field")
                        || !combo.getStyleClass().contains("pf-settings-combo-box")
                ).map(this::describeNode).toList(),
                textAreas.stream().filter(area -> !area.getStyleClass().contains("pf-settings-text-area")).map(this::describeNode).toList(),
                checkBoxes.stream().filter(box -> !box.getStyleClass().contains("pf-settings-check-box")).map(this::describeNode).toList(),
                listViews.stream().filter(list ->
                    !list.getStyleClass().contains(SettingsUiStyles.LIST)
                        && !list.getStyleClass().contains(CATEGORY_LIST_STYLE)
                ).map(this::describeNode).toList(),
                buttons.stream().filter(button ->
                    !button.getStyleClass().contains("pf-ui-pill")
                        && !button.getStyleClass().contains("pf-ui-compact-action-button")
                ).map(this::describeNode).toList(),
                fields.size(),
                colorPickers.size(),
                combos.size(),
                textAreas.size(),
                checkBoxes.size(),
                listViews.size(),
                buttons.size()
            );
        });

        assertTrue(audit.missingCategories().isEmpty(), () -> "Required settings categories were not loaded: " + audit.missingCategories());
        assertTrue(audit.fieldCount() >= 20, () -> "Expected multiple settings text/password fields but found " + audit.fieldCount());
        assertTrue(audit.colorPickerCount() >= 3, () -> "Expected appearance color pickers but found " + audit.colorPickerCount());
        assertTrue(audit.comboCount() >= 3, () -> "Expected settings combo boxes but found " + audit.comboCount());
        assertTrue(audit.textAreaCount() >= 1, () -> "Expected at least one settings text area but found " + audit.textAreaCount());
        assertTrue(audit.checkBoxCount() >= 8, () -> "Expected settings check boxes but found " + audit.checkBoxCount());
        assertTrue(audit.listCount() >= 4, () -> "Expected settings list views but found " + audit.listCount());
        assertTrue(audit.buttonCount() >= 10, () -> "Expected settings buttons but found " + audit.buttonCount());
        assertTrue(audit.missingFields().isEmpty(), () -> "Text/password fields missing shared field styling: " + audit.missingFields());
        assertTrue(audit.missingColorPickers().isEmpty(), () -> "Color pickers missing shared field styling: " + audit.missingColorPickers());
        assertTrue(audit.missingCombos().isEmpty(), () -> "Combo boxes missing shared field styling: " + audit.missingCombos());
        assertTrue(audit.missingTextAreas().isEmpty(), () -> "Text areas missing shared styling: " + audit.missingTextAreas());
        assertTrue(audit.missingCheckBoxes().isEmpty(), () -> "Check boxes missing shared styling: " + audit.missingCheckBoxes());
        assertTrue(audit.missingLists().isEmpty(), () -> "List views missing shared styling: " + audit.missingLists());
        assertTrue(audit.missingButtons().isEmpty(), () -> "Buttons missing shared styling: " + audit.missingButtons());
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

    private <T> List<T> collectNodes(Set<Node> roots, Class<T> type) {
        Set<T> matches = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Node root : roots) {
            collectNodes(root, type, matches);
        }
        return new ArrayList<>(matches);
    }

    private <T> void collectNodes(Node node, Class<T> type, Set<T> matches) {
        if (type.isInstance(node)) {
            matches.add(type.cast(node));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectNodes(child, type, matches);
            }
        }
    }

    private String describeNode(Node node) {
        StringBuilder description = new StringBuilder(node.getClass().getSimpleName());
        if (node instanceof TextInputControl input && input.getPromptText() != null && !input.getPromptText().isBlank()) {
            description.append('[').append(input.getPromptText()).append(']');
        } else if (node instanceof Button button && button.getText() != null && !button.getText().isBlank()) {
            description.append('[').append(button.getText()).append(']');
        } else if (node instanceof CheckBox checkBox && checkBox.getText() != null && !checkBox.getText().isBlank()) {
            description.append('[').append(checkBox.getText()).append(']');
        } else if (node instanceof ColorPicker colorPicker && colorPicker.getValue() != null) {
            description.append('[').append(colorPicker.getValue()).append(']');
        } else if (node instanceof ComboBox<?> comboBox && comboBox.getValue() != null) {
            description.append('[').append(comboBox.getValue()).append(']');
        }
        return description.toString();
    }

    private <T> boolean hasAncestor(Node node, Class<T> type) {
        Parent parent = node.getParent();
        while (parent != null) {
            if (type.isInstance(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private record StyleAudit(
        List<String> missingCategories,
        List<String> missingFields,
        List<String> missingColorPickers,
        List<String> missingCombos,
        List<String> missingTextAreas,
        List<String> missingCheckBoxes,
        List<String> missingLists,
        List<String> missingButtons,
        int fieldCount,
        int colorPickerCount,
        int comboCount,
        int textAreaCount,
        int checkBoxCount,
        int listCount,
        int buttonCount
    ) {
    }
}
