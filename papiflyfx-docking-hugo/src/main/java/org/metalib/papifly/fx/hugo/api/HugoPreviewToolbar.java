package org.metalib.papifly.fx.hugo.api;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.theme.HugoThemeMapper;

public final class HugoPreviewToolbar extends HBox {

    private final Button startButton = new Button("Start");
    private final Button stopButton = new Button("Stop");
    private final Button backButton = new Button("Back");
    private final Button forwardButton = new Button("Forward");
    private final Button reloadButton = new Button("Reload");
    private final Button openInBrowserButton = new Button("Open in Browser");
    private final Hyperlink addressLink = new Hyperlink("-");

    public HugoPreviewToolbar(
        Runnable onStart,
        Runnable onStop,
        Runnable onBack,
        Runnable onForward,
        Runnable onReload,
        Runnable onOpenExternal
    ) {
        setId("hugo-preview-toolbar");
        setSpacing(8);
        setPadding(new Insets(6));

        startButton.setId("hugo-preview-start");
        stopButton.setId("hugo-preview-stop");
        backButton.setId("hugo-preview-back");
        forwardButton.setId("hugo-preview-forward");
        reloadButton.setId("hugo-preview-reload");
        openInBrowserButton.setId("hugo-preview-open-browser");
        addressLink.setId("hugo-preview-address");

        startButton.setOnAction(event -> onStart.run());
        stopButton.setOnAction(event -> onStop.run());
        backButton.setOnAction(event -> onBack.run());
        forwardButton.setOnAction(event -> onForward.run());
        reloadButton.setOnAction(event -> onReload.run());
        openInBrowserButton.setOnAction(event -> onOpenExternal.run());
        addressLink.setOnAction(event -> onOpenExternal.run());

        addressLink.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addressLink, Priority.ALWAYS);

        getChildren().setAll(
            startButton,
            stopButton,
            backButton,
            forwardButton,
            reloadButton,
            addressLink,
            openInBrowserButton
        );
    }

    public void setAddress(String address) {
        if (address == null || address.isBlank()) {
            addressLink.setText("-");
            return;
        }
        addressLink.setText(address);
    }

    public String getAddress() {
        return addressLink.getText();
    }

    public Hyperlink getAddressLink() {
        return addressLink;
    }

    public void applyTheme(Theme theme) {
        setBackground(new Background(new BackgroundFill(
            HugoThemeMapper.toolbarBackground(theme),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        String textColor = HugoThemeMapper.toHex(theme.textColor());
        startButton.setStyle("-fx-text-fill: " + textColor + ";");
        stopButton.setStyle("-fx-text-fill: " + textColor + ";");
        backButton.setStyle("-fx-text-fill: " + textColor + ";");
        forwardButton.setStyle("-fx-text-fill: " + textColor + ";");
        reloadButton.setStyle("-fx-text-fill: " + textColor + ";");
        openInBrowserButton.setStyle("-fx-text-fill: " + textColor + ";");
        addressLink.setTextFill(HugoThemeMapper.toColor(HugoThemeMapper.linkColor(theme)));
    }
}
