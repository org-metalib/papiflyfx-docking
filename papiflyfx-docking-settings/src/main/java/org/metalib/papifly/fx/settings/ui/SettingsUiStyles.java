package org.metalib.papifly.fx.settings.ui;

import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

public final class SettingsUiStyles {

    public static final String CATEGORY_ROW = "pf-settings-category-row";
    public static final String CATEGORY_LABEL = "pf-settings-category-label";
    public static final String COMPACT_FIELD = "pf-ui-compact-field";

    private SettingsUiStyles() {
    }

    public static <T extends Node> T apply(T node, String... styleClasses) {
        for (String styleClass : styleClasses) {
            if (styleClass != null
                && !styleClass.isBlank()
                && !node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        }
        return node;
    }

    public static <T extends TextInputControl> T applyCompactField(T field) {
        return apply(field, COMPACT_FIELD);
    }
}
