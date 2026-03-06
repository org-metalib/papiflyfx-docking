package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public class TokenDialog extends Dialog<String> {

    public TokenDialog() {
        setTitle("GitHub Token");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearType = new ButtonType("Clear", ButtonBar.ButtonData.LEFT);
        getDialogPane().getButtonTypes().addAll(saveType, clearType, ButtonType.CANCEL);

        PasswordField tokenField = new PasswordField();
        tokenField.setPromptText("ghp_...");

        VBox content = new VBox(8, new Label("Personal access token"), tokenField);
        content.setPadding(new Insets(12));
        getDialogPane().setContent(content);

        setResultConverter(buttonType -> {
            if (buttonType == saveType) {
                return tokenField.getText() == null ? "" : tokenField.getText().trim();
            }
            if (buttonType == clearType) {
                return "";
            }
            return null;
        });
    }
}
