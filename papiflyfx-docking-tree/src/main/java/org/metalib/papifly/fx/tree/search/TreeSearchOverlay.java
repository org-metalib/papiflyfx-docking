package org.metalib.papifly.fx.tree.search;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
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
        getStyleClass().add("pf-tree-search-overlay");
        setSpacing(2.0);
        setPadding(new Insets(2.0, 4.0, 2.0, 4.0));
        setPrefWidth(380.0);
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
        ensureStylesheetLoaded();

        queryField.setPromptText("Find");
        queryField.getStyleClass().add("pf-tree-search-field");
        queryField.setMinWidth(48.0);
        queryField.setMinHeight(24.0);
        queryField.setPrefHeight(24.0);
        queryField.setMaxHeight(24.0);
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

        resultLabel.getStyleClass().add("pf-tree-search-result-label");
        resultLabel.setMinWidth(48.0);
        resultLabel.setPrefWidth(76.0);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);
        resultLabel.setManaged(false);
        resultLabel.setVisible(false);

        Button prevButton = createIconButton(SearchIconPaths.ARROW_UP, 10.0, onPrevious);
        Button nextButton = createIconButton(SearchIconPaths.ARROW_DOWN, 10.0, onNext);
        Button closeButton = createIconButton(SearchIconPaths.CLOSE, 10.0, this::close);

        HBox row = new HBox(2.0, queryField, resultLabel, prevButton, nextButton, closeButton);
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
        String panelBackground = paintToCss(safeTheme.rowBackgroundAlternate(), "#252526");
        String panelBorder = paintToCss(safeTheme.connectingLineColor(), "#3f3f46");
        String accent = paintToCss(safeTheme.focusedBorder(), "#007acc");
        String text = paintToCss(safeTheme.textColor(), "#d4d4d4");
        String mutedText = paintToCss(withOpacity(safeTheme.textColor(), 0.66), "#858585");
        String controlBackground = paintToCss(safeTheme.rowBackground(), "#1e1e1e");
        String controlBorder = paintToCss(safeTheme.connectingLineColor(), "#555555");
        String controlHover = paintToCss(safeTheme.hoverBackground(), "#2a2d2e");
        setStyle("""
            -pf-tree-search-bg: %s;
            -pf-tree-search-panel-border: %s;
            -pf-tree-search-accent: %s;
            -pf-tree-search-text: %s;
            -pf-tree-search-muted-text: %s;
            -pf-tree-search-control-bg: %s;
            -pf-tree-search-control-border: %s;
            -pf-tree-search-control-hover-bg: %s;
            """.formatted(
            panelBackground,
            panelBorder,
            accent,
            text,
            mutedText,
            controlBackground,
            controlBorder,
            controlHover
        ));
        setEffect(new DropShadow(10.0, Color.color(0.0, 0.0, 0.0, 0.25)));
        Color iconColor = asColor(safeTheme.textColor(), Color.web("#d4d4d4"));
        for (SVGPath icon : iconNodes) {
            icon.setFill(iconColor);
        }
    }

    private Button createIconButton(String path, double size, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("pf-tree-search-icon-button");
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
