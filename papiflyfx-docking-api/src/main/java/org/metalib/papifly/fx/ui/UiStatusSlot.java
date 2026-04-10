package org.metalib.papifly.fx.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/**
 * Shared status container for compact inline progress and message content.
 */
public class UiStatusSlot extends HBox {

    public UiStatusSlot(Node... children) {
        super(children);
        getStyleClass().add("pf-ui-status-slot");
        setAlignment(Pos.CENTER_LEFT);
    }
}
