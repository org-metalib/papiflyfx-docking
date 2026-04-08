package org.metalib.papifly.fx.tree.search;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.searchui.SearchIconPaths;
import org.metalib.papifly.fx.searchui.SearchOverlayBase;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class TreeSearchOverlay extends SearchOverlayBase {

    private static final String STYLESHEET_NAME = "tree-search-overlay.css";
    private static final double COMPACT_WIDTH_THRESHOLD = 260.0;

    private final TextField queryField = new TextField();
    private final Label resultLabel = new Label();
    private final List<SVGPath> iconNodes = new ArrayList<>();

    private Runnable onNext = () -> {};
    private Runnable onPrevious = () -> {};
    private Consumer<String> onQueryChanged = value -> {};
    private Runnable onClose = () -> {};

    private boolean programmaticUpdate;
    private boolean compactLayout;
    private boolean hasResultText;

    public TreeSearchOverlay() {
        super();
        getStyleClass().addAll("pf-tree-search-overlay", "pf-ui-popup-surface");
        setSpacing(UiMetrics.SPACE_1);
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));
        setPrefWidth(380.0);
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
        UiStyleSupport.ensureCommonStylesheetLoaded(this);
        ensureStylesheetLoaded();

        queryField.setPromptText("Find");
        queryField.getStyleClass().addAll("pf-tree-search-field", "pf-ui-compact-field");
        queryField.setMinWidth(48.0);
        queryField.setMinHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        queryField.setPrefHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        queryField.setMaxHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        queryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!programmaticUpdate) {
                onQueryChanged.accept(newValue == null ? "" : newValue);
            }
        });
        queryField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    onPrevious.run();
                } else {
                    onNext.run();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });

        resultLabel.getStyleClass().addAll("pf-tree-search-result-label", "pf-ui-result-label");
        resultLabel.setMinWidth(48.0);
        resultLabel.setPrefWidth(76.0);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);
        resultLabel.setManaged(false);
        resultLabel.setVisible(false);

        Button prevButton = createIconButton(SearchIconPaths.ARROW_UP, 10.0, onPrevious);
        Button nextButton = createIconButton(SearchIconPaths.ARROW_DOWN, 10.0, onNext);
        Button closeButton = createIconButton(SearchIconPaths.CLOSE, 10.0, this::close);

        HBox row = new HBox(UiMetrics.SPACE_1, queryField, resultLabel, prevButton, nextButton, closeButton);
        row.getStyleClass().add("pf-tree-search-row");
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(queryField, Priority.ALWAYS);
        getChildren().add(row);
        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            compactLayout = newWidth.doubleValue() > 0.0 && newWidth.doubleValue() < COMPACT_WIDTH_THRESHOLD;
            updateResultLabelVisibility();
        });
    }

    public void setOnQueryChanged(Consumer<String> onQueryChanged) {
        this.onQueryChanged = onQueryChanged == null ? value -> {} : onQueryChanged;
    }

    public void setOnNext(Runnable onNext) {
        this.onNext = onNext == null ? () -> {} : onNext;
    }

    public void setOnPrevious(Runnable onPrevious) {
        this.onPrevious = onPrevious == null ? () -> {} : onPrevious;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose == null ? () -> {} : onClose;
    }

    @Override
    public void open(String initialQuery) {
        showOverlay();
        if (initialQuery != null) {
            withProgrammaticUpdate(() -> queryField.setText(initialQuery));
            onQueryChanged.accept(initialQuery);
        }
        queryField.requestFocus();
        queryField.selectAll();
    }

    public void appendTyped(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!isOpen()) {
            open(text);
            queryField.positionCaret(queryField.getText().length());
            return;
        }
        withProgrammaticUpdate(() -> queryField.appendText(text));
        onQueryChanged.accept(queryField.getText());
        queryField.positionCaret(queryField.getText().length());
    }

    @Override
    public void close() {
        hideOverlay();
        withProgrammaticUpdate(queryField::clear);
        hasResultText = false;
        resultLabel.setText("");
        updateResultLabelVisibility();
        onClose.run();
    }

    public boolean isOpen() {
        return isVisible();
    }

    public String getQuery() {
        return queryField.getText();
    }

    public void updateCount(int currentIndex, int matchCount) {
        String query = getQuery();
        if (query == null || query.isBlank()) {
            hasResultText = false;
            resultLabel.setText("");
            updateResultLabelVisibility();
            return;
        }
        hasResultText = true;
        if (matchCount <= 0) {
            resultLabel.setText("No results");
            updateResultLabelVisibility();
            return;
        }
        int displayIndex = Math.max(1, Math.min(currentIndex + 1, matchCount));
        resultLabel.setText(displayIndex + " of " + matchCount);
        updateResultLabelVisibility();
    }

    public void setTheme(TreeViewTheme theme) {
        TreeViewTheme safeTheme = theme == null ? TreeViewTheme.dark() : theme;
        Color accent = UiStyleSupport.asColor(safeTheme.focusedBorder(), Color.web("#007acc"));
        Color success = Color.web("#47a473");
        Color warning = Color.web("#c69a31");
        Color danger = Color.web("#d16969");
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
            UiStyleSupport.paintToCss(safeTheme.rowBackgroundAlternate(), "#252526"),
            UiStyleSupport.paintToCss(safeTheme.rowBackground(), "#1e1e1e"),
            UiStyleSupport.paintToCss(safeTheme.rowBackgroundAlternate(), "#252526"),
            UiStyleSupport.paintToCss(safeTheme.rowBackground(), "#1e1e1e"),
            UiStyleSupport.paintToCss(safeTheme.hoverBackground(), "#2a2d2e"),
            UiStyleSupport.paintToCss(safeTheme.hoverBackground(), "#2a2d2e"),
            UiStyleSupport.paintToCss(UiStyleSupport.alpha(accent, Color.web("#007acc"), 0.12), "rgba(0, 122, 204, 0.12)"),
            UiStyleSupport.paintToCss(safeTheme.textColor(), "#d4d4d4"),
            UiStyleSupport.paintToCss(withOpacity(safeTheme.textColor(), 0.66), "#858585"),
            UiStyleSupport.paintToCss(withOpacity(safeTheme.textColor(), 0.5), "#7a7a7a"),
            UiStyleSupport.paintToCss(safeTheme.connectingLineColor(), "#3f3f46"),
            UiStyleSupport.paintToCss(safeTheme.connectingLineColor(), "#555555"),
            UiStyleSupport.paintToCss(accent, "#007acc"),
            UiStyleSupport.paintToCss(accent, "#007acc"),
            UiStyleSupport.paintToCss(UiStyleSupport.alpha(accent, Color.web("#007acc"), 0.12), "rgba(0, 122, 204, 0.12)"),
            UiStyleSupport.paintToCss(success, "#47a473"),
            UiStyleSupport.paintToCss(new Color(success.getRed(), success.getGreen(), success.getBlue(), 0.14), "rgba(71, 164, 115, 0.14)"),
            UiStyleSupport.paintToCss(warning, "#c69a31"),
            UiStyleSupport.paintToCss(new Color(warning.getRed(), warning.getGreen(), warning.getBlue(), 0.14), "rgba(198, 154, 49, 0.14)"),
            UiStyleSupport.paintToCss(danger, "#d16969"),
            UiStyleSupport.paintToCss(new Color(danger.getRed(), danger.getGreen(), danger.getBlue(), 0.16), "rgba(209, 105, 105, 0.16)"),
            "rgba(0, 0, 0, 0.25)"
        ));
        Color iconColor = UiStyleSupport.asColor(safeTheme.textColor(), Color.web("#d4d4d4"));
        for (SVGPath icon : iconNodes) {
            icon.setFill(iconColor);
        }
    }

    private Button createIconButton(String path, double size, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("pf-tree-search-icon-button", "pf-ui-icon-button");
        button.setGraphic(createIcon(path, size));
        button.setFocusTraversable(false);
        button.setOnAction(event -> action.run());
        button.setMinSize(22.0, 22.0);
        button.setPrefSize(22.0, 22.0);
        button.setMaxSize(22.0, 22.0);
        return button;
    }

    private SVGPath createIcon(String path, double size) {
        SVGPath icon = SearchIconPaths.createIcon(path, size);
        icon.getStyleClass().add("pf-ui-icon");
        iconNodes.add(icon);
        return icon;
    }

    private void withProgrammaticUpdate(Runnable action) {
        boolean previous = programmaticUpdate;
        programmaticUpdate = true;
        try {
            action.run();
        } finally {
            programmaticUpdate = previous;
        }
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

    private static Color asColor(Paint paint, Color fallback) {
        if (paint instanceof Color color) {
            return color;
        }
        return fallback;
    }

    private void updateResultLabelVisibility() {
        boolean visible = hasResultText && !compactLayout;
        resultLabel.setManaged(visible);
        resultLabel.setVisible(visible);
    }

    private void ensureStylesheetLoaded() {
        URL stylesheetUrl = TreeSearchOverlay.class.getResource(STYLESHEET_NAME);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!getStylesheets().contains(stylesheet)) {
            getStylesheets().add(stylesheet);
        }
    }

    private static Paint withOpacity(Paint paint, double opacity) {
        if (paint instanceof Color color) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
        }
        return paint;
    }
}
