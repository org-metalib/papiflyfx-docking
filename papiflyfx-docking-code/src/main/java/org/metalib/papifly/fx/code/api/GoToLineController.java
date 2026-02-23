package org.metalib.papifly.fx.code.api;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.code.search.SearchIcons;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import java.net.URL;
import java.util.Locale;
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
        getStyleClass().add("pf-goto-overlay");
        setPadding(new Insets(4, 6, 4, 6));
        setSpacing(4);
        setMinWidth(260);
        setPrefWidth(300);
        setMaxWidth(360);
        setMaxHeight(Region.USE_PREF_SIZE);
        setManaged(false);
        setVisible(false);
        ensureStylesheetLoaded();

        Label titleLabel = new Label("Go to line");
        titleLabel.getStyleClass().add("pf-goto-title");

        Button closeButton = new Button();
        closeButton.getStyleClass().add("pf-goto-icon-button");
        closeButton.setGraphic(createIcon(SearchIcons.CLOSE, 10));
        closeButton.setOnAction(e -> close());

        HBox titleRow = new HBox(4, titleLabel, new Region(), closeButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleRow.getChildren().get(1), Priority.ALWAYS);

        rangeLabel = new Label();
        rangeLabel.getStyleClass().add("pf-goto-range");

        lineField = new TextField();
        lineField.getStyleClass().add("pf-goto-field");
        lineField.setPromptText("Line number");
        lineField.setPrefHeight(24);
        lineField.setMinHeight(24);
        lineField.setMaxHeight(24);
        lineField.setTextFormatter(new TextFormatter<>(lineNumberFilter()));
        HBox.setHgrow(lineField, Priority.ALWAYS);

        confirmButton = new Button("Go");
        confirmButton.getStyleClass().add("pf-goto-action-button");
        confirmButton.setDisable(true);
        confirmButton.setOnAction(e -> submit());

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("pf-goto-action-button");
        cancelButton.getStyleClass().add("pf-goto-action-secondary");
        cancelButton.setOnAction(e -> close());

        HBox actionRow = new HBox(4, lineField, confirmButton, cancelButton);
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
        icon.getStyleClass().add("pf-goto-icon");
        Color iconColor = asColor(theme.searchOverlayPrimaryText(), Color.web("#d4d4d4"));
        icon.setFill(iconColor);
        return icon;
    }

    private void applyThemeColors() {
        setStyle("""
            -pf-goto-bg: %s;
            -pf-goto-panel-border: %s;
            -pf-goto-text: %s;
            -pf-goto-muted-text: %s;
            -pf-goto-control-bg: %s;
            -pf-goto-control-border: %s;
            -pf-goto-control-hover-bg: %s;
            -pf-goto-control-focused-border: %s;
            -pf-goto-control-disabled-text: %s;
            -pf-goto-invalid-border: %s;
            -pf-goto-shadow: %s;
            """.formatted(
            paintToCss(theme.searchOverlayBackground(), "#252526"),
            paintToCss(theme.searchOverlayPanelBorder(), "#3f3f46"),
            paintToCss(theme.searchOverlayPrimaryText(), "#d4d4d4"),
            paintToCss(theme.searchOverlaySecondaryText(), "#858585"),
            paintToCss(theme.searchOverlayControlBackground(), "#3c3c3c"),
            paintToCss(theme.searchOverlayControlBorder(), "#555555"),
            paintToCss(theme.searchOverlayControlHoverBackground(), "#4a4a4a"),
            paintToCss(theme.searchOverlayControlFocusedBorder(), "#007acc"),
            paintToCss(theme.searchOverlayControlDisabledText(), "#7a7a7a"),
            paintToCss(theme.searchOverlayNoResultsBorder(), "#d16969"),
            paintToCss(theme.searchOverlayShadowColor(), "rgba(0, 0, 0, 0.25)")
        ));
        Color shadowColor = asColor(theme.searchOverlayShadowColor(), Color.color(0, 0, 0, 0.25));
        setEffect(new DropShadow(10, shadowColor));
        Color iconColor = asColor(theme.searchOverlayPrimaryText(), Color.web("#d4d4d4"));
        lookupAll(".pf-goto-icon").forEach(node -> {
            if (node instanceof SVGPath icon) {
                icon.setFill(iconColor);
            }
        });
    }

    private static Color asColor(Paint paint, Color fallback) {
        if (paint instanceof Color color) {
            return color;
        }
        return fallback;
    }

    private static String paintToCss(Paint paint, String fallback) {
        if (paint instanceof Color color) {
            int red = (int) Math.round(color.getRed() * 255.0);
            int green = (int) Math.round(color.getGreen() * 255.0);
            int blue = (int) Math.round(color.getBlue() * 255.0);
            return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.3f)", red, green, blue, color.getOpacity());
        }
        return fallback;
    }
}
