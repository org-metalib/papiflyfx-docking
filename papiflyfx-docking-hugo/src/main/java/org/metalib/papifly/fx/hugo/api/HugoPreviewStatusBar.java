package org.metalib.papifly.fx.hugo.api;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.metalib.papifly.fx.hugo.theme.HugoThemeMapper;

public final class HugoPreviewStatusBar extends HBox {

    private final Label stateLabel = new Label("Stopped");
    private final Label messageLabel = new Label();

    public HugoPreviewStatusBar() {
        setId("hugo-preview-status");
        stateLabel.setId("hugo-preview-status-state");
        messageLabel.setId("hugo-preview-status-message");
        setSpacing(10);
        setPadding(new Insets(4, 8, 4, 8));
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);
        getChildren().setAll(stateLabel, messageLabel);
    }

    public void setServerState(HugoServerProcessManager.State state, int port) {
        if (state == null) {
            stateLabel.setText("Unknown");
            return;
        }
        if (state == HugoServerProcessManager.State.RUNNING && port > 0) {
            stateLabel.setText("Running on " + port);
            return;
        }
        switch (state) {
            case STOPPED -> stateLabel.setText("Stopped");
            case STARTING -> stateLabel.setText("Starting");
            case RUNNING -> stateLabel.setText("Running");
            case ERROR -> stateLabel.setText("Error");
        }
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }

    public String getStateText() {
        return stateLabel.getText();
    }

    public String getMessageText() {
        return messageLabel.getText();
    }

    public void applyTheme(Theme theme) {
        setBackground(new Background(new BackgroundFill(
            HugoThemeMapper.statusBackground(theme),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        stateLabel.setTextFill(HugoThemeMapper.toColor(HugoThemeMapper.statusText(theme)));
        messageLabel.setTextFill(HugoThemeMapper.toColor(HugoThemeMapper.statusText(theme)));
    }
}
