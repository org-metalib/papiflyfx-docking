package org.metalib.papifly.fx.ui;

import javafx.scene.control.Button;

/**
 * Shared rounded button shell for pill-like toolbar actions.
 */
public class UiPillButton extends Button {

    public UiPillButton() {
        this("");
    }

    public UiPillButton(String text) {
        super(text);
        getStyleClass().add("pf-ui-pill");
    }
}
