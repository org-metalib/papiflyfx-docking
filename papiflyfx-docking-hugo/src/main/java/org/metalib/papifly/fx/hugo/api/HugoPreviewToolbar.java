package org.metalib.papifly.fx.hugo.api;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
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

public final class HugoPreviewToolbar extends HBox {

    private static final String BUTTON_NEUTRAL_BASE = compact("""
        -fx-background-color: linear-gradient(to bottom, #253652, #1b2940);
        -fx-border-color: #334f7a;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #edf3ff;
        -fx-font-size: 12px;
        -fx-font-weight: 600;
        -fx-padding: 6 10 6 10;
        """);
    private static final String BUTTON_NEUTRAL_HOVER = compact("""
        -fx-background-color: linear-gradient(to bottom, #2f4466, #223352);
        -fx-border-color: #3f6397;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #edf3ff;
        -fx-font-size: 12px;
        -fx-font-weight: 600;
        -fx-padding: 6 10 6 10;
        """);
    private static final String BUTTON_START_BASE = compact("""
        -fx-background-color: linear-gradient(to bottom, #2f7a53, #225a3d);
        -fx-border-color: #44a06f;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #f4fff8;
        -fx-font-size: 12px;
        -fx-font-weight: 700;
        -fx-padding: 6 12 6 12;
        """);
    private static final String BUTTON_START_HOVER = compact("""
        -fx-background-color: linear-gradient(to bottom, #3a9163, #2b704c);
        -fx-border-color: #58bc82;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #f4fff8;
        -fx-font-size: 12px;
        -fx-font-weight: 700;
        -fx-padding: 6 12 6 12;
        """);
    private static final String BUTTON_STOP_BASE = compact("""
        -fx-background-color: linear-gradient(to bottom, #8a3946, #692733);
        -fx-border-color: #b14f5f;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #fff3f4;
        -fx-font-size: 12px;
        -fx-font-weight: 700;
        -fx-padding: 6 12 6 12;
        """);
    private static final String BUTTON_STOP_HOVER = compact("""
        -fx-background-color: linear-gradient(to bottom, #9a4655, #78303c);
        -fx-border-color: #cb6576;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #fff3f4;
        -fx-font-size: 12px;
        -fx-font-weight: 700;
        -fx-padding: 6 12 6 12;
        """);
    private static final String BUTTON_OPEN_BASE = compact("""
        -fx-background-color: linear-gradient(to bottom, #2a587f, #1d4365);
        -fx-border-color: #3e7aaa;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #eff8ff;
        -fx-font-size: 12px;
        -fx-font-weight: 600;
        -fx-padding: 6 12 6 12;
        """);
    private static final String BUTTON_OPEN_HOVER = compact("""
        -fx-background-color: linear-gradient(to bottom, #326998, #245279);
        -fx-border-color: #5194cd;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #eff8ff;
        -fx-font-size: 12px;
        -fx-font-weight: 600;
        -fx-padding: 6 12 6 12;
        """);
    private static final String BUTTON_DISABLED_COMPACT = compact("""
        -fx-background-color: linear-gradient(to bottom, #2a3240, #1f2733);
        -fx-border-color: #3b4558;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #8290a8;
        -fx-font-size: 12px;
        -fx-font-weight: 600;
        -fx-padding: 6 10 6 10;
        -fx-opacity: 1.0;
        """);
    private static final String BUTTON_DISABLED_WIDE = compact("""
        -fx-background-color: linear-gradient(to bottom, #2a3240, #1f2733);
        -fx-border-color: #3b4558;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-text-fill: #8290a8;
        -fx-font-size: 12px;
        -fx-font-weight: 600;
        -fx-padding: 6 12 6 12;
        -fx-opacity: 1.0;
        """);

    private final Button startButton = new Button("Start");
    private final Button stopButton = new Button("Stop");
    private final Button backButton = new Button("<");
    private final Button forwardButton = new Button(">");
    private final Button reloadButton = new Button("Reload");
    private final Button openInBrowserButton = new Button("Open Browser");
    private final Hyperlink addressLink = new Hyperlink("-");
    private final Tooltip addressTooltip = new Tooltip("-");
    private final Tooltip backTooltip = new Tooltip("Go to previous page");
    private final Tooltip forwardTooltip = new Tooltip("Go to next page");
    private final Tooltip reloadTooltip = new Tooltip("Reload current page");
    private final Tooltip openInBrowserTooltip = new Tooltip("Open current page in system browser");
    private Theme currentTheme = Theme.dark();

    public HugoPreviewToolbar(
        Runnable onStart,
        Runnable onStop,
        Runnable onBack,
        Runnable onForward,
        Runnable onReload,
        Runnable onOpenExternal
    ) {
        setId("hugo-preview-toolbar");
        setSpacing(UiMetrics.SPACE_2);
        setPadding(new Insets(UiMetrics.SPACE_2, UiMetrics.SPACE_3, UiMetrics.SPACE_2, UiMetrics.SPACE_3));
        setAlignment(Pos.CENTER_LEFT);
        setMinHeight(UiMetrics.TOOLBAR_HEIGHT);

        startButton.setId("hugo-preview-start");
        stopButton.setId("hugo-preview-stop");
        backButton.setId("hugo-preview-back");
        forwardButton.setId("hugo-preview-forward");
        reloadButton.setId("hugo-preview-reload");
        openInBrowserButton.setId("hugo-preview-open-browser");
        addressLink.setId("hugo-preview-address");

        configureAccessibleButton(
            backButton,
            backTooltip,
            "Previous page",
            "Navigate to previous page in preview history"
        );
        configureAccessibleButton(
            forwardButton,
            forwardTooltip,
            "Next page",
            "Navigate to next page in preview history"
        );
        configureAccessibleButton(
            reloadButton,
            reloadTooltip,
            "Reload page",
            "Reload the current preview page"
        );
        configureAccessibleButton(
            openInBrowserButton,
            openInBrowserTooltip,
            "Open in browser",
            "Open the current preview page in the default system browser"
        );

        startButton.setOnAction(event -> onStart.run());
        stopButton.setOnAction(event -> onStop.run());
        backButton.setOnAction(event -> onBack.run());
        forwardButton.setOnAction(event -> onForward.run());
        reloadButton.setOnAction(event -> onReload.run());
        openInBrowserButton.setOnAction(event -> onOpenExternal.run());
        addressLink.setOnAction(event -> onOpenExternal.run());

        styleButton(startButton, BUTTON_START_BASE, BUTTON_START_HOVER, BUTTON_DISABLED_WIDE);
        styleButton(stopButton, BUTTON_STOP_BASE, BUTTON_STOP_HOVER, BUTTON_DISABLED_WIDE);
        styleButton(backButton, BUTTON_NEUTRAL_BASE, BUTTON_NEUTRAL_HOVER, BUTTON_DISABLED_COMPACT);
        styleButton(forwardButton, BUTTON_NEUTRAL_BASE, BUTTON_NEUTRAL_HOVER, BUTTON_DISABLED_COMPACT);
        styleButton(reloadButton, BUTTON_NEUTRAL_BASE, BUTTON_NEUTRAL_HOVER, BUTTON_DISABLED_COMPACT);
        styleButton(openInBrowserButton, BUTTON_OPEN_BASE, BUTTON_OPEN_HOVER, BUTTON_DISABLED_WIDE);

        backButton.setPrefWidth(UiMetrics.SPACE_5 * 2.0);
        forwardButton.setPrefWidth(UiMetrics.SPACE_5 * 2.0);
        reloadButton.setPrefWidth(UiMetrics.SPACE_4 * 4.0);

        addressLink.setTooltip(addressTooltip);
        addressLink.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        addressLink.setMaxWidth(Double.MAX_VALUE);
        addressLink.setStyle(compact("""
            -fx-text-fill: #8ad8ff;
            -fx-font-size: 12px;
            -fx-font-weight: 600;
            -fx-padding: 5 10 5 10;
            -fx-underline: false;
            """));

        HBox serverControls = createGroup(startButton, stopButton);
        HBox navigationControls = createGroup(backButton, forwardButton, reloadButton);
        HBox addressContainer = new HBox(addressLink);
        addressContainer.setAlignment(Pos.CENTER_LEFT);
        addressContainer.setPadding(new Insets(2, 8, 2, 8));
        addressContainer.setBorder(new Border(new BorderStroke(
            Color.web("#2a4366"),
            BorderStrokeStyle.SOLID,
            new javafx.scene.layout.CornerRadii(10),
            new BorderWidths(1)
        )));
        addressContainer.setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, #101b2e, #0c1526);
            -fx-background-radius: 10;
            """));

        HBox.setHgrow(addressContainer, Priority.ALWAYS);
        addressLink.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addressLink, Priority.ALWAYS);

        getChildren().setAll(
            serverControls,
            navigationControls,
            addressContainer,
            openInBrowserButton
        );
        applyVisualStyle();
        setServerState(HugoServerProcessManager.State.STOPPED);
    }

    public void setAddress(String address) {
        if (address == null || address.isBlank()) {
            addressLink.setText("-");
            addressTooltip.setText("-");
            return;
        }
        addressLink.setText(address);
        addressTooltip.setText(address);
    }

    public String getAddress() {
        return addressLink.getText();
    }

    public Hyperlink getAddressLink() {
        return addressLink;
    }

    public void applyVisualStyle() {
        Color toolbarBackground = UiStyleSupport.asColor(currentTheme.headerBackground(), Color.web("#111a2d"));
        Color toolbarBorder = UiStyleSupport.asColor(currentTheme.borderColor(), Color.web("#21324f"));
        setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            """.formatted(
            UiStyleSupport.paintToCss(toolbarBackground, "#111a2d"),
            UiStyleSupport.paintToCss(toolbarBackground.darker(), "#0b1323")
        )));
        setBorder(new Border(new BorderStroke(
            toolbarBorder,
            BorderStrokeStyle.SOLID,
            javafx.scene.layout.CornerRadii.EMPTY,
            new BorderWidths(0, 0, 1, 0)
        )));
    }

    public void applyTheme(Theme theme) {
        currentTheme = theme == null ? Theme.dark() : theme;
        applyVisualStyle();
        javafx.scene.text.Font font = currentTheme.contentFont();
        for (Button button : new Button[] {startButton, stopButton, backButton, forwardButton, reloadButton, openInBrowserButton}) {
            if (font != null) {
                button.setFont(font);
            }
            button.setTextFill(UiStyleSupport.asColor(currentTheme.textColorActive(), Color.web("#edf3ff")));
        }
        if (font != null) {
            addressLink.setFont(font);
        }
    }

    public void setServerState(HugoServerProcessManager.State state) {
        if (state == HugoServerProcessManager.State.RUNNING || state == HugoServerProcessManager.State.STARTING) {
            startButton.setDisable(true);
            stopButton.setDisable(false);
            return;
        }
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    private HBox createGroup(javafx.scene.Node... children) {
        HBox group = new HBox(UiMetrics.SPACE_2, children);
        group.setAlignment(Pos.CENTER_LEFT);
        group.setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));
        group.setBorder(new Border(new BorderStroke(
            Color.web("#2b4366"),
            BorderStrokeStyle.SOLID,
            new javafx.scene.layout.CornerRadii(10),
            new BorderWidths(1)
        )));
        group.setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, #152138, #101a2f);
            -fx-background-radius: 10;
            """));
        return group;
    }

    private void styleButton(Button button, String baseStyle, String hoverStyle, String disabledStyle) {
        button.setUserData(new ButtonStyles(baseStyle, hoverStyle, disabledStyle));
        button.setFocusTraversable(true);
        button.hoverProperty().addListener((obs, oldHover, hover) -> applyButtonStyle(button));
        button.disableProperty().addListener((obs, oldDisabled, disabled) -> applyButtonStyle(button));
        applyButtonStyle(button);
    }

    private void configureAccessibleButton(Button button, Tooltip tooltip, String accessibleText, String accessibleHelp) {
        button.setTooltip(tooltip);
        button.setAccessibleText(accessibleText);
        button.setAccessibleHelp(accessibleHelp);
    }

    private void applyButtonStyle(Button button) {
        Object userData = button.getUserData();
        if (!(userData instanceof ButtonStyles styles)) {
            return;
        }
        if (button.isDisable()) {
            button.setStyle(styles.disabledStyle());
            return;
        }
        button.setStyle(button.isHover() ? styles.hoverStyle() : styles.baseStyle());
    }

    private static String compact(String style) {
        return style.replace("\n", "").trim();
    }

    private record ButtonStyles(String baseStyle, String hoverStyle, String disabledStyle) {
    }
}
