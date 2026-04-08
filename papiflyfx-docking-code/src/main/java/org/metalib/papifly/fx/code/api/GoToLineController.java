package org.metalib.papifly.fx.code.api;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.code.search.SearchIcons;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.net.URL;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Compact go-to-line overlay used by {@link CodeEditor}.
 */
public class GoToLineController extends VBox {

    private static final String STYLESHEET_NAME = "go-to-line-overlay.css";
    private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private final TextField lineField;
    private final Label rangeLabel;
    private final Button confirmButton;
    private Consumer<Integer> onGoToLine;
    private Runnable onClose;
    private int maxLine = 1;

    /**
     * Creates go-to-line overlay controller.
     */
    public GoToLineController() {
        getStyleClass().addAll("pf-goto-overlay", "pf-ui-popup-surface");
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));
        setSpacing(UiMetrics.SPACE_1);
        setMinWidth(260);
        setPrefWidth(300);
        setMaxWidth(360);
        setMaxHeight(Region.USE_PREF_SIZE);
        setManaged(false);
        setVisible(false);
        UiStyleSupport.ensureCommonStylesheetLoaded(this);
        ensureStylesheetLoaded();

        Label titleLabel = new Label("Go to line");
        titleLabel.getStyleClass().add("pf-goto-title");

        Button closeButton = new Button();
        closeButton.getStyleClass().addAll("pf-goto-icon-button", "pf-ui-icon-button");
        closeButton.setGraphic(createIcon(SearchIcons.CLOSE, 10));
        closeButton.setOnAction(e -> close());

        HBox titleRow = new HBox(UiMetrics.SPACE_1, titleLabel, new Region(), closeButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleRow.getChildren().get(1), Priority.ALWAYS);

        rangeLabel = new Label();
        rangeLabel.getStyleClass().addAll("pf-goto-range", "pf-ui-result-label");

        lineField = new TextField();
        lineField.getStyleClass().addAll("pf-goto-field", "pf-ui-compact-field");
        lineField.setPromptText("Line number");
        lineField.setPrefHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        lineField.setMinHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        lineField.setMaxHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        lineField.setTextFormatter(new TextFormatter<>(lineNumberFilter()));
        HBox.setHgrow(lineField, Priority.ALWAYS);

        confirmButton = new Button("Go");
        confirmButton.getStyleClass().addAll("pf-goto-action-button", "pf-ui-compact-action-button");
        confirmButton.setDisable(true);
        confirmButton.setOnAction(e -> submit());

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("pf-goto-action-button", "pf-ui-compact-action-button");
        cancelButton.getStyleClass().addAll("pf-goto-action-secondary", "pf-ui-compact-action-button-secondary");
        cancelButton.setOnAction(e -> close());

        HBox actionRow = new HBox(UiMetrics.SPACE_1, lineField, confirmButton, cancelButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(titleRow, rangeLabel, actionRow);

        lineField.textProperty().addListener((obs, oldValue, newValue) -> updateValidationState());
        lineField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                submit();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        updateValidationState();
        applyThemeColors();
    }

    /**
     * Sets callback fired with 1-based line number when user confirms.
     *
     * @param onGoToLine callback receiving selected one-based line number
     */
    public void setOnGoToLine(Consumer<Integer> onGoToLine) {
        this.onGoToLine = onGoToLine;
    }

    /**
     * Sets callback fired when overlay is closed.
     *
     * @param onClose callback invoked when overlay closes
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Opens the overlay with current line and maximum line count context.
     *
     * @param currentLine one-based current line number
     * @param maxLine maximum one-based line number allowed
     */
    public void open(int currentLine, int maxLine) {
        this.maxLine = Math.max(1, maxLine);
        rangeLabel.setText("Line number (1-" + this.maxLine + ")");
        lineField.setText(String.valueOf(clampLine(currentLine)));
        updateValidationState();
        setManaged(true);
        setVisible(true);
        lineField.requestFocus();
        lineField.selectAll();
    }

    /**
     * Closes the overlay.
     */
    public void close() {
        if (!isVisible()) {
            return;
        }
        setManaged(false);
        setVisible(false);
        if (onClose != null) {
            onClose.run();
        }
    }

    /**
     * Returns true when overlay is currently visible.
     *
     * @return {@code true} when go-to-line overlay is visible
     */
    public boolean isOpen() {
        return isVisible();
    }

    /**
     * Applies editor theme to this overlay.
     *
     * @param theme editor theme palette
     */
    public void setTheme(CodeEditorTheme theme) {
        this.theme = theme == null ? CodeEditorTheme.dark() : theme;
        applyThemeColors();
    }

    /**
     * Returns active theme used by this controller.
     *
     * @return currently applied editor theme
     */
    public CodeEditorTheme getTheme() {
        return theme;
    }

    private void submit() {
        Integer parsedLine = parseLine(lineField.getText());
        if (parsedLine == null) {
            updateValidationState();
            return;
        }
        if (onGoToLine != null) {
            onGoToLine.accept(parsedLine);
        }
        close();
    }

    private void updateValidationState() {
        Integer parsedLine = parseLine(lineField.getText());
        boolean valid = parsedLine != null;
        lineField.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, !valid && !lineField.getText().isBlank());
        confirmButton.setDisable(!valid);
    }

    private int clampLine(int line) {
        return Math.max(1, Math.min(line, maxLine));
    }

    private Integer parseLine(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < 1) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private UnaryOperator<TextFormatter.Change> lineNumberFilter() {
        return change -> {
            String next = change.getControlNewText();
            if (next.matches("\\d{0,9}")) {
                return change;
            }
            return null;
        };
    }

    private void ensureStylesheetLoaded() {
        URL stylesheetUrl = GoToLineController.class.getResource(STYLESHEET_NAME);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!getStylesheets().contains(stylesheet)) {
            getStylesheets().add(stylesheet);
        }
    }

    private SVGPath createIcon(String svgPath, double size) {
        SVGPath icon = SearchIcons.createIcon(svgPath, size);
        icon.getStyleClass().addAll("pf-goto-icon", "pf-ui-icon");
        Color iconColor = UiStyleSupport.asColor(theme.searchOverlayPrimaryText(), Color.web("#d4d4d4"));
        icon.setFill(iconColor);
        return icon;
    }

    private void applyThemeColors() {
        Color success = Color.web("#47a473");
        Color warning = Color.web("#c69a31");
        Color danger = UiStyleSupport.asColor(theme.searchOverlayNoResultsBorder(), Color.web("#d16969"));
        setStyle(UiStyleSupport.metricVariables() + UiStyleSupport.fontVariables(null) + """
            -pf-ui-surface-panel: %s;
            -pf-ui-surface-panel-subtle: %s;
            -pf-ui-surface-overlay: %s;
            -pf-ui-surface-control: %s;
            -pf-ui-surface-control-hover: %s;
            -pf-ui-surface-control-pressed: %s;
            -pf-ui-surface-selected: %s;
            -pf-ui-text-primary: %s;
            -pf-ui-text-muted: %s;
            -pf-ui-text-disabled: %s;
            -pf-ui-border-default: %s;
            -pf-ui-border-subtle: %s;
            -pf-ui-border-focus: %s;
            -pf-ui-accent: %s;
            -pf-ui-accent-subtle: %s;
            -pf-ui-success: %s;
            -pf-ui-success-subtle: %s;
            -pf-ui-warning: %s;
            -pf-ui-warning-subtle: %s;
            -pf-ui-danger: %s;
            -pf-ui-danger-subtle: %s;
            -pf-ui-shadow-overlay: %s;
            """.formatted(
            UiStyleSupport.paintToCss(theme.searchOverlayBackground(), "#252526"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlBackground(), "#3c3c3c"),
            UiStyleSupport.paintToCss(theme.searchOverlayBackground(), "#252526"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlBackground(), "#3c3c3c"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlHoverBackground(), "#4a4a4a"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlHoverBackground(), "#4a4a4a"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlHoverBackground(), "#4a4a4a"),
            UiStyleSupport.paintToCss(theme.searchOverlayPrimaryText(), "#d4d4d4"),
            UiStyleSupport.paintToCss(theme.searchOverlaySecondaryText(), "#858585"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlDisabledText(), "#7a7a7a"),
            UiStyleSupport.paintToCss(theme.searchOverlayPanelBorder(), "#3f3f46"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlBorder(), "#555555"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlFocusedBorder(), "#007acc"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlFocusedBorder(), "#007acc"),
            UiStyleSupport.paintToCss(theme.searchOverlayControlHoverBackground(), "#4a4a4a"),
            UiStyleSupport.paintToCss(success, "#47a473"),
            UiStyleSupport.paintToCss(new Color(success.getRed(), success.getGreen(), success.getBlue(), 0.14), "rgba(71, 164, 115, 0.14)"),
            UiStyleSupport.paintToCss(warning, "#c69a31"),
            UiStyleSupport.paintToCss(new Color(warning.getRed(), warning.getGreen(), warning.getBlue(), 0.14), "rgba(198, 154, 49, 0.14)"),
            UiStyleSupport.paintToCss(danger, "#d16969"),
            UiStyleSupport.paintToCss(new Color(danger.getRed(), danger.getGreen(), danger.getBlue(), 0.16), "rgba(209, 105, 105, 0.16)"),
            UiStyleSupport.paintToCss(theme.searchOverlayShadowColor(), "rgba(0, 0, 0, 0.25)")
        ));
        Color iconColor = UiStyleSupport.asColor(theme.searchOverlayPrimaryText(), Color.web("#d4d4d4"));
        lookupAll(".pf-goto-icon").forEach(node -> {
            if (node instanceof SVGPath icon) {
                icon.setFill(iconColor);
            }
        });
    }
}
