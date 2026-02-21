package org.metalib.papifly.fx.code.search;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Overlay UI for find/replace functionality.
 * <p>
 * Uses compact, chip-style controls and icon-only navigation actions while
 * keeping search model behavior and keyboard flow stable.
 */
public class SearchController extends VBox {

    private static final String STYLESHEET_NAME = "search-overlay.css";
    private static final PseudoClass NO_RESULTS_PSEUDO_CLASS = PseudoClass.getPseudoClass("no-results");
    private static final double FIELD_HEIGHT = 24.0;

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private final SearchModel searchModel;
    private final TextField searchField;
    private final TextField replaceField;
    private final Label matchCountLabel;
    private final ToggleButton regexToggle;
    private final ToggleButton caseSensitiveToggle;
    private final ToggleButton wholeWordToggle;
    private final ToggleButton inSelectionToggle;
    private final ToggleButton preserveCaseToggle;
    private final Button skipButton;
    private final Button replaceButton;
    private final Button replaceAllButton;
    private final Button chevronButton;
    private final SVGPath chevronIcon;
    private final HBox replaceRow;
    private final List<SVGPath> iconNodes = new ArrayList<>();

    private boolean replaceMode;
    private boolean programmaticUpdate;
    private Document document;
    private Supplier<int[]> selectionRangeSupplier;
    private Consumer<SearchMatch> onNavigate;
    private Runnable onClose;
    private Runnable onSearchChanged;

    public SearchController(SearchModel searchModel) {
        this.searchModel = searchModel;

        getStyleClass().add("pf-search-overlay");
        setPadding(new Insets(2, 4, 2, 4));
        setSpacing(2);
        setMinWidth(520);
        setPrefWidth(620);
        setMaxWidth(760);
        setMaxHeight(Region.USE_PREF_SIZE);
        setManaged(false);
        setVisible(false);
        ensureStylesheetLoaded();

        searchField = createTextField("Find");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setQuery(newValue);
            executeSearch();
        });
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) {
                    navigatePrevious();
                } else {
                    navigateNext();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        replaceField = createTextField("Replace");
        replaceField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setReplacement(newValue);
        });
        replaceField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                replaceCurrent();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        matchCountLabel = new Label();
        matchCountLabel.getStyleClass().add("pf-search-result-label");
        matchCountLabel.setMinWidth(76);
        matchCountLabel.setAlignment(Pos.CENTER_RIGHT);

        caseSensitiveToggle = createChipToggle("Aa");
        caseSensitiveToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setCaseSensitive(selected);
            executeSearch();
        });

        wholeWordToggle = createChipToggle("W");
        wholeWordToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setWholeWord(selected);
            executeSearch();
        });

        regexToggle = createChipToggle(".*");
        regexToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setRegexMode(selected);
            executeSearch();
        });

        inSelectionToggle = createChipToggle("In");
        SVGPath inSelectionIcon = createIcon(SearchIcons.FILTER, 10);
        inSelectionToggle.setGraphic(inSelectionIcon);
        inSelectionToggle.setContentDisplay(ContentDisplay.LEFT);
        inSelectionToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setSearchInSelection(selected);
            executeSearch();
        });
        inSelectionToggle.setDisable(true);

        preserveCaseToggle = createChipToggle("Aa");
        preserveCaseToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setPreserveCase(selected);
        });

        Button prevButton = createIconButton(createIcon(SearchIcons.ARROW_UP, 10), this::navigatePrevious);
        Button nextButton = createIconButton(createIcon(SearchIcons.ARROW_DOWN, 10), this::navigateNext);
        chevronIcon = createIcon(SearchIcons.CHEVRON_RIGHT, 10);
        chevronButton = createIconButton(chevronIcon, this::toggleReplaceMode);
        Button closeButton = createIconButton(createIcon(SearchIcons.CLOSE, 10), this::close);

        skipButton = createActionButton("Skip", this::skipCurrent, true);
        replaceButton = createActionButton("Replace", this::replaceCurrent, false);
        replaceAllButton = createActionButton("All", this::replaceAll, false);

        StackPane searchInput = createSearchInput();
        HBox searchMiddle = new HBox(2, caseSensitiveToggle, wholeWordToggle, regexToggle, inSelectionToggle, matchCountLabel);
        searchMiddle.setAlignment(Pos.CENTER_LEFT);
        HBox searchRight = new HBox(2, prevButton, nextButton, chevronButton, closeButton);
        searchRight.setAlignment(Pos.CENTER_RIGHT);
        HBox searchRow = new HBox(2, searchInput, searchMiddle, searchRight);
        searchRow.getStyleClass().add("pf-search-row");
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchInput, Priority.ALWAYS);

        HBox replaceMiddle = new HBox(2, preserveCaseToggle);
        replaceMiddle.setAlignment(Pos.CENTER_LEFT);
        HBox replaceRight = new HBox(2, skipButton, replaceButton, replaceAllButton);
        replaceRight.setAlignment(Pos.CENTER_RIGHT);
        replaceRow = new HBox(2, replaceField, replaceMiddle, replaceRight);
        replaceRow.getStyleClass().add("pf-search-row");
        replaceRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(replaceField, Priority.ALWAYS);
        replaceRow.setManaged(false);
        replaceRow.setVisible(false);

        getChildren().addAll(searchRow, replaceRow);

        withProgrammaticUpdate(() -> {
            caseSensitiveToggle.setSelected(searchModel.isCaseSensitive());
            wholeWordToggle.setSelected(searchModel.isWholeWord());
            regexToggle.setSelected(searchModel.isRegexMode());
            inSelectionToggle.setSelected(searchModel.isSearchInSelection());
            preserveCaseToggle.setSelected(searchModel.isPreserveCase());
        });

        updateMatchLabel();
        applyThemeColors();
    }

    /**
     * Sets the document to search in.
     */
    public void setDocument(Document document) {
        this.document = document;
    }

    /**
     * Sets a supplier that provides active selection offsets as {start, end}.
     */
    public void setSelectionRangeSupplier(Supplier<int[]> selectionRangeSupplier) {
        this.selectionRangeSupplier = selectionRangeSupplier;
        refreshSelectionScope();
    }

    /**
     * Re-evaluates selection scope based on the current selection supplier.
     */
    public void refreshSelectionScope() {
        int[] scope = selectionRangeSupplier == null ? null : selectionRangeSupplier.get();
        boolean validScope = scope != null && scope.length >= 2 && scope[0] < scope[1];
        if (validScope) {
            searchModel.setSelectionScope(scope[0], scope[1]);
            inSelectionToggle.setDisable(false);
            return;
        }
        searchModel.clearSelectionScope();
        inSelectionToggle.setDisable(true);
        if (searchModel.isSearchInSelection()) {
            searchModel.setSearchInSelection(false);
            withProgrammaticUpdate(() -> inSelectionToggle.setSelected(false));
        }
    }

    /**
     * Sets the editor theme and refreshes overlay styling.
     */
    public void setTheme(CodeEditorTheme theme) {
        this.theme = theme == null ? CodeEditorTheme.dark() : theme;
        applyThemeColors();
    }

    /**
     * Returns the current editor theme.
     */
    public CodeEditorTheme getTheme() {
        return theme;
    }

    /**
     * Sets the callback invoked when navigating to a match.
     */
    public void setOnNavigate(Consumer<SearchMatch> onNavigate) {
        this.onNavigate = onNavigate;
    }

    /**
     * Sets the callback invoked when the search panel is closed.
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Sets the callback invoked when search results change (for highlight refresh).
     */
    public void setOnSearchChanged(Runnable onSearchChanged) {
        this.onSearchChanged = onSearchChanged;
    }

    /**
     * Shows the search overlay and focuses the search field.
     */
    public void open() {
        refreshSelectionScope();
        setManaged(true);
        setVisible(true);
        searchField.requestFocus();
        searchField.selectAll();
    }

    /**
     * Shows the search overlay with existing query text selected.
     */
    public void open(String initialQuery) {
        if (initialQuery != null && !initialQuery.isEmpty()) {
            withProgrammaticUpdate(() -> searchField.setText(initialQuery));
            searchModel.setQuery(initialQuery);
            executeSearch();
        }
        open();
    }

    /**
     * Opens the search overlay directly in replace mode.
     */
    public void openInReplaceMode(String initialQuery) {
        if (!replaceMode) {
            toggleReplaceMode();
        }
        open(initialQuery);
    }

    /**
     * Hides the search overlay and clears highlights.
     */
    public void close() {
        setManaged(false);
        setVisible(false);
        searchModel.clear();
        withProgrammaticUpdate(() -> {
            searchField.clear();
            replaceField.clear();
        });
        updateMatchLabel();
        if (onSearchChanged != null) {
            onSearchChanged.run();
        }
        if (onClose != null) {
            onClose.run();
        }
    }

    /**
     * Returns true if the search overlay is currently visible.
     */
    public boolean isOpen() {
        return isVisible();
    }

    /**
     * Returns true if replace mode is active (replace row visible).
     */
    public boolean isReplaceMode() {
        return replaceMode;
    }

    /**
     * Returns the search model.
     */
    public SearchModel getSearchModel() {
        return searchModel;
    }

    /**
     * Refreshes the match label and button states from current search model state.
     */
    public void refreshMatchDisplay() {
        updateMatchLabel();
    }

    private void toggleReplaceMode() {
        replaceMode = !replaceMode;
        replaceRow.setManaged(replaceMode);
        replaceRow.setVisible(replaceMode);
        chevronIcon.setContent(replaceMode ? SearchIcons.CHEVRON_DOWN : SearchIcons.CHEVRON_RIGHT);
    }

    private void executeSearch() {
        refreshSelectionScope();
        if (document != null) {
            searchModel.search(document);
        }
        updateMatchLabel();
        if (onSearchChanged != null) {
            onSearchChanged.run();
        }
        SearchMatch current = searchModel.getCurrentMatch();
        if (current != null && onNavigate != null) {
            onNavigate.accept(current);
        }
    }

    private void navigateNext() {
        SearchMatch match = searchModel.nextMatch();
        if (match != null && onNavigate != null) {
            onNavigate.accept(match);
        }
        updateMatchLabel();
    }

    private void navigatePrevious() {
        SearchMatch match = searchModel.previousMatch();
        if (match != null && onNavigate != null) {
            onNavigate.accept(match);
        }
        updateMatchLabel();
    }

    private void skipCurrent() {
        navigateNext();
    }

    private void replaceCurrent() {
        if (document != null && searchModel.replaceCurrent(document)) {
            executeSearch();
        }
    }

    private void replaceAll() {
        if (document != null) {
            searchModel.replaceAll(document);
            executeSearch();
        }
    }

    private void updateMatchLabel() {
        int count = searchModel.getMatchCount();
        boolean noResults = !searchModel.getQuery().isEmpty() && count == 0;
        if (count == 0) {
            matchCountLabel.setText(searchModel.getQuery().isEmpty() ? "" : "No results");
        } else {
            int current = searchModel.getCurrentMatchIndex() + 1;
            matchCountLabel.setText(current + " of " + count);
        }
        searchField.pseudoClassStateChanged(NO_RESULTS_PSEUDO_CLASS, noResults);
        skipButton.setDisable(count == 0);
        replaceButton.setDisable(count == 0);
        replaceAllButton.setDisable(count == 0);
    }

    private TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("pf-search-field");
        field.setMinHeight(FIELD_HEIGHT);
        field.setPrefHeight(FIELD_HEIGHT);
        field.setMaxHeight(FIELD_HEIGHT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private StackPane createSearchInput() {
        StackPane input = new StackPane();
        input.getStyleClass().add("pf-search-input-wrap");
        SVGPath searchIcon = createIcon(SearchIcons.SEARCH, 11);
        Label leadingIcon = new Label();
        leadingIcon.getStyleClass().add("pf-search-leading-icon");
        leadingIcon.setGraphic(searchIcon);
        leadingIcon.setMouseTransparent(true);
        searchField.setPadding(new Insets(0, 6, 0, 20));
        StackPane.setAlignment(searchField, Pos.CENTER_LEFT);
        StackPane.setAlignment(leadingIcon, Pos.CENTER_LEFT);
        input.getChildren().addAll(searchField, leadingIcon);
        HBox.setHgrow(input, Priority.ALWAYS);
        return input;
    }

    private ToggleButton createChipToggle(String text) {
        ToggleButton toggle = new ToggleButton(text);
        toggle.getStyleClass().add("pf-search-chip");
        return toggle;
    }

    private Button createIconButton(SVGPath icon, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("pf-search-icon-button");
        button.setGraphic(icon);
        button.setOnAction(e -> action.run());
        return button;
    }

    private Button createActionButton(String text, Runnable action, boolean secondary) {
        Button button = new Button(text);
        button.getStyleClass().add("pf-search-action-button");
        if (secondary) {
            button.getStyleClass().add("pf-search-action-secondary");
        }
        button.setOnAction(e -> action.run());
        button.setDisable(true);
        return button;
    }

    private SVGPath createIcon(String svgPath, double size) {
        SVGPath icon = SearchIcons.createIcon(svgPath, size);
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

    private void ensureStylesheetLoaded() {
        URL stylesheetUrl = SearchController.class.getResource(STYLESHEET_NAME);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!getStylesheets().contains(stylesheet)) {
            getStylesheets().add(stylesheet);
        }
    }

    private void applyThemeColors() {
        setStyle("""
            -pf-search-bg: %s;
            -pf-search-panel-border: %s;
            -pf-search-accent: %s;
            -pf-search-text: %s;
            -pf-search-muted-text: %s;
            -pf-search-control-bg: %s;
            -pf-search-control-border: %s;
            -pf-search-control-hover-bg: %s;
            -pf-search-control-active-bg: %s;
            -pf-search-control-focused-border: %s;
            -pf-search-control-disabled-text: %s;
            -pf-search-no-results-border: %s;
            -pf-search-shadow: %s;
            -pf-search-integrated-toggle-active: %s;
            -pf-search-error-bg: %s;
            """.formatted(
            paintToCss(theme.searchOverlayBackground(), "#252526"),
            paintToCss(theme.searchOverlayPanelBorder(), "#3f3f46"),
            paintToCss(theme.searchOverlayAccentBorder(), "#007acc"),
            paintToCss(theme.searchOverlayPrimaryText(), "#d4d4d4"),
            paintToCss(theme.searchOverlaySecondaryText(), "#858585"),
            paintToCss(theme.searchOverlayControlBackground(), "#3c3c3c"),
            paintToCss(theme.searchOverlayControlBorder(), "#555555"),
            paintToCss(theme.searchOverlayControlHoverBackground(), "#4a4a4a"),
            paintToCss(theme.searchOverlayControlActiveBackground(), "#164f7a"),
            paintToCss(theme.searchOverlayControlFocusedBorder(), "#007acc"),
            paintToCss(theme.searchOverlayControlDisabledText(), "#7a7a7a"),
            paintToCss(theme.searchOverlayNoResultsBorder(), "#d16969"),
            paintToCss(theme.searchOverlayShadowColor(), "rgba(0, 0, 0, 0.25)"),
            paintToCss(theme.searchOverlayIntegratedToggleActive(), "#007acc"),
            paintToCss(theme.searchOverlayErrorBackground(), "rgba(209, 105, 105, 0.16)")
        ));
        Color iconColor = asColor(theme.searchOverlayPrimaryText(), Color.web("#d4d4d4"));
        for (SVGPath icon : iconNodes) {
            icon.setFill(iconColor);
        }
        Color shadowColor = asColor(theme.searchOverlayShadowColor(), Color.color(0, 0, 0, 0.25));
        setEffect(new DropShadow(10, shadowColor));
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
