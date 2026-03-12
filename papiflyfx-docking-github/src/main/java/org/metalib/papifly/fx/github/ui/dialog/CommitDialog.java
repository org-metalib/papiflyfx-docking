package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.docking.api.Theme;

public class CommitDialog extends Dialog<String> {

    public CommitDialog() {
        this(null);
    }

    public CommitDialog(Theme theme) {
        setTitle("Commit Changes");

        ButtonType commitType = new ButtonType("Commit", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(commitType, ButtonType.CANCEL);

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Commit message");
        messageArea.setPrefRowCount(4);

        VBox content = new VBox(8, new Label("Message"), messageArea);
        content.setPadding(new Insets(12));
        getDialogPane().setContent(content);

        GitHubDialogStyler.apply(this, theme);

        setResultConverter(buttonType -> {
            if (buttonType != commitType) {
                return null;
            }
            String message = messageArea.getText() == null ? "" : messageArea.getText().trim();
            if (message.isBlank()) {
                return null;
            }
            return message;
        });
    }
}
