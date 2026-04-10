package org.metalib.papifly.fx.ui;

import javafx.scene.control.ToggleButton;

/**
 * Shared compact toggle chip used by lightweight overlay controls.
 */
public class UiChipToggle extends ToggleButton {

    public UiChipToggle() {
        this("");
    }

    public UiChipToggle(String text) {
        super(text);
        getStyleClass().addAll("pf-ui-chip-toggle");
    }
}
