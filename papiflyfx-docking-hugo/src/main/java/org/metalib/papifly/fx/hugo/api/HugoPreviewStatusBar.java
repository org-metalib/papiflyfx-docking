package org.metalib.papifly.fx.hugo.api;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiStyleSupport;

public final class HugoPreviewStatusBar extends HBox {

    private static final String STATE_STOPPED_STYLE = compact("""
        -fx-background-color: linear-gradient(to bottom, #273a5b, #1f2d47);
        -fx-text-fill: #dde7ff;
        -fx-font-size: 11px;
        -fx-font-weight: 700;
        -fx-padding: 3 10 3 10;
        -fx-background-radius: 100;
        -fx-border-radius: 100;
        -fx-border-color: #3c5786;
        """);
    private static final String STATE_STARTING_STYLE = compact("""
        -fx-background-color: linear-gradient(to bottom, #6f5a1f, #564317);
        -fx-text-fill: #fff6d7;
        -fx-font-size: 11px;
        -fx-font-weight: 700;
        -fx-padding: 3 10 3 10;
        -fx-background-radius: 100;
        -fx-border-radius: 100;
        -fx-border-color: #93752a;
        """);
    private static final String STATE_RUNNING_STYLE = compact("""
        -fx-background-color: linear-gradient(to bottom, #2f7a53, #225a3d);
        -fx-text-fill: #ecfff4;
        -fx-font-size: 11px;
        -fx-font-weight: 700;
        -fx-padding: 3 10 3 10;
        -fx-background-radius: 100;
        -fx-border-radius: 100;
        -fx-border-color: #47a473;
        """);
    private static final String STATE_ERROR_STYLE = compact("""
        -fx-background-color: linear-gradient(to bottom, #8e3f4c, #6d2c38);
        -fx-text-fill: #fff1f3;
        -fx-font-size: 11px;
        -fx-font-weight: 700;
        -fx-padding: 3 10 3 10;
        -fx-background-radius: 100;
        -fx-border-radius: 100;
        -fx-border-color: #bf5f71;
        """);

    private final Label stateLabel = new Label("Stopped");
    private final Label messageLabel = new Label();
    private Theme currentTheme = Theme.dark();

    public HugoPreviewStatusBar() {
        setId("hugo-preview-status");
        stateLabel.setId("hugo-preview-status-state");
        messageLabel.setId("hugo-preview-status-message");
        setSpacing(UiMetrics.SPACE_3);
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_3, UiMetrics.SPACE_1, UiMetrics.SPACE_3));
        setAlignment(Pos.CENTER_LEFT);

        stateLabel.setMinWidth(120);
        stateLabel.setAlignment(Pos.CENTER);
        stateLabel.setStyle(STATE_STOPPED_STYLE);
        messageLabel.setStyle(compact("""
            -fx-text-fill: #b6c4de;
            -fx-font-size: 11px;
            """));
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);
        getChildren().setAll(stateLabel, messageLabel);

        applyVisualStyle();
    }

    public void setServerState(HugoServerProcessManager.State state, int port) {
        if (state == null) {
            stateLabel.setText("Unknown");
            stateLabel.setStyle(STATE_STOPPED_STYLE);
            return;
        }
        if (state == HugoServerProcessManager.State.RUNNING && port > 0) {
            stateLabel.setText("Running on " + port);
            stateLabel.setStyle(STATE_RUNNING_STYLE);
            return;
        }
        switch (state) {
            case STOPPED -> {
                stateLabel.setText("Stopped");
                stateLabel.setStyle(STATE_STOPPED_STYLE);
            }
            case STARTING -> {
                stateLabel.setText("Starting");
                stateLabel.setStyle(STATE_STARTING_STYLE);
            }
            case RUNNING -> {
                stateLabel.setText("Running");
                stateLabel.setStyle(STATE_RUNNING_STYLE);
            }
            case ERROR -> {
                stateLabel.setText("Error");
                stateLabel.setStyle(STATE_ERROR_STYLE);
            }
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

    public void applyVisualStyle() {
        Color background = UiStyleSupport.asColor(currentTheme.headerBackground(), Color.web("#0e1627"));
        Color border = UiStyleSupport.asColor(currentTheme.borderColor(), Color.web("#1f2f49"));
        setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            """.formatted(
            UiStyleSupport.paintToCss(background, "#0e1627"),
            UiStyleSupport.paintToCss(background.darker(), "#0a1120")
        )));
        setBorder(new Border(new BorderStroke(
            border,
            BorderStrokeStyle.SOLID,
            javafx.scene.layout.CornerRadii.EMPTY,
            new BorderWidths(1, 0, 0, 0)
        )));
    }

    public void applyTheme(Theme theme) {
        currentTheme = theme == null ? Theme.dark() : theme;
        applyVisualStyle();
        javafx.scene.text.Font font = currentTheme.contentFont();
        if (font != null) {
            stateLabel.setFont(font);
            messageLabel.setFont(font);
        }
    }

    private static String compact(String style) {
        return style.replace("\n", "").trim();
    }
}
