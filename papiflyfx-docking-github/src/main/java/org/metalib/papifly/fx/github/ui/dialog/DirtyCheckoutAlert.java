package org.metalib.papifly.fx.github.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class DirtyCheckoutAlert {

    private DirtyCheckoutAlert() {
    }

    public static boolean confirm(String branchName) {
        Alert alert = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Working tree has changes. Force checkout to \"" + branchName + "\"?",
            ButtonType.OK,
            ButtonType.CANCEL
        );
        alert.setTitle("Force checkout");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
