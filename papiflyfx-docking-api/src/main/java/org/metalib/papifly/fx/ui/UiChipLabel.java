package org.metalib.papifly.fx.ui;

import javafx.scene.control.Label;

import java.util.Arrays;

/**
 * Shared lightweight chip label with semantic variants.
 */
public class UiChipLabel extends Label {

    public UiChipLabel() {
        this("", UiChipVariant.NEUTRAL);
    }

    public UiChipLabel(String text, UiChipVariant variant) {
        super(text);
        getStyleClass().add("pf-ui-chip");
        setVariant(variant == null ? UiChipVariant.NEUTRAL : variant);
    }

    public final void setVariant(UiChipVariant variant) {
        getStyleClass().removeAll(Arrays.stream(UiChipVariant.values()).map(UiChipVariant::styleClass).toList());
        getStyleClass().add((variant == null ? UiChipVariant.NEUTRAL : variant).styleClass());
    }
}
