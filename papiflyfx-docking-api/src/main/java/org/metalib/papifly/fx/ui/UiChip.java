package org.metalib.papifly.fx.ui;

import javafx.scene.control.Label;

import java.util.Arrays;
import java.util.List;

/**
 * Shared pill-like label for compact status and metadata chips.
 */
public class UiChip extends Label {

    private static final List<String> VARIANT_CLASSES = Arrays.stream(UiChipVariant.values())
        .map(UiChipVariant::styleClass)
        .toList();

    private UiChipVariant variant = UiChipVariant.NEUTRAL;

    public UiChip() {
        this("");
    }

    public UiChip(String text) {
        super(text);
        getStyleClass().add("pf-ui-chip");
        setVariant(UiChipVariant.NEUTRAL);
    }

    public UiChipVariant getVariant() {
        return variant;
    }

    public void setVariant(UiChipVariant variant) {
        UiChipVariant resolved = variant == null ? UiChipVariant.NEUTRAL : variant;
        getStyleClass().removeAll(VARIANT_CLASSES);
        getStyleClass().add(resolved.styleClass());
        this.variant = resolved;
    }
}
