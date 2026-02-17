package org.metalib.papifly.fx.code.search;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.metalib.papifly.fx.code.document.Document;

import java.util.function.Consumer;

/**
 * Overlay UI for find/replace functionality.
 * <p>
 * Provides text fields for search/replace, navigation buttons,
 * and mode toggles (regex, case-sensitive).
 */
public class SearchController extends VBox {

    private static final Color BACKGROUND_COLOR = Color.web("#252526");
    private static final Color ACCENT_BORDER_COLOR = Color.web("#007acc");
    private static final Color CONTROL_BACKGROUND_COLOR = Color.web("#3c3c3c");
    private static final Color CONTROL_BORDER_COLOR = Color.web("#555555");
    private static final Color PRIMARY_TEXT_COLOR = Color.web("#d4d4d4");
    private static final Color SECONDARY_TEXT_COLOR = Color.web("#858585");

    private static final CornerRadii CONTROL_RADII = new CornerRadii(2);
    private static final Insets BUTTON_PADDING = new Insets(2, 6, 2, 6);
    private static final Insets CHECKBOX_PADDING = new Insets(0, 2, 0, 2);
    private static final Font SMALL_FONT = Font.font(11);

    private final SearchModel searchModel;
    private final TextField searchField;
    private final TextField replaceField;
    private final Label matchCountLabel;
    private final CheckBox regexToggle;
    private final CheckBox caseSensitiveToggle;

    private Document document;
    private Consumer<SearchMatch> onNavigate;
    private Runnable onClose;
    private Runnable onSearchChanged;

    public SearchController(SearchModel searchModel) {
        this.searchModel = searchModel;

        setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
            ACCENT_BORDER_COLOR,
            BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY,
            new BorderWidths(0, 0, 1, 0)
        )));
        setPadding(new Insets(4, 8, 4, 8));
        setSpacing(4);
        setManaged(false);
        setVisible(false);

        // Search row
        searchField = new TextField();
        searchField.setPromptText("Find");
        configureTextField(searchField);
        searchField.setPrefWidth(200);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        matchCountLabel = new Label("No results");
        matchCountLabel.setTextFill(SECONDARY_TEXT_COLOR);
        matchCountLabel.setFont(SMALL_FONT);
        matchCountLabel.setMinWidth(70);

        Button prevButton = new Button("\u25b2");
        configureButton(prevButton);
        prevButton.setOnAction(e -> navigatePrevious());

        Button nextButton = new Button("\u25bc");
        configureButton(nextButton);
        nextButton.setOnAction(e -> navigateNext());

        Button closeButton = new Button("\u2715");
        configureButton(closeButton);
        closeButton.setOnAction(e -> close());

        regexToggle = new CheckBox(".*");
        configureToggle(regexToggle);
        regexToggle.setOnAction(e -> {
            searchModel.setRegexMode(regexToggle.isSelected());
            executeSearch();
        });

        caseSensitiveToggle = new CheckBox("Aa");
        configureToggle(caseSensitiveToggle);
        caseSensitiveToggle.setOnAction(e -> {
            searchModel.setCaseSensitive(caseSensitiveToggle.isSelected());
            executeSearch();
        });

        HBox searchRow = new HBox(4, searchField, regexToggle, caseSensitiveToggle, matchCountLabel, prevButton, nextButton, closeButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        // Replace row
        replaceField = new TextField();
        replaceField.setPromptText("Replace");
        configureTextField(replaceField);
        replaceField.setPrefWidth(200);
        HBox.setHgrow(replaceField, Priority.ALWAYS);

        Button replaceButton = new Button("Replace");
        configureButton(replaceButton);
        replaceButton.setOnAction(e -> replaceCurrent());

        Button replaceAllButton = new Button("All");
        configureButton(replaceAllButton);
        replaceAllButton.setOnAction(e -> replaceAll());

        HBox replaceRow = new HBox(4, replaceField, replaceButton, replaceAllButton);
        replaceRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(searchRow, replaceRow);

        // Wire search on text change and Enter
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
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

        replaceField.textProperty().addListener((obs, oldValue, newValue) ->
            searchModel.setReplacement(newValue));
        replaceField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });
    }

    /**
     * Sets the document to search in.
     */
    public void setDocument(Document document) {
        this.document = document;
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
            searchField.setText(initialQuery);
        }
        open();
    }

    /**
     * Hides the search overlay and clears highlights.
     */
    public void close() {
        setManaged(false);
        setVisible(false);
        searchModel.clear();
        searchField.clear();
        replaceField.clear();
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
     * Returns the search model.
     */
    public SearchModel getSearchModel() {
        return searchModel;
    }

    private void executeSearch() {
        if (document != null) {
            searchModel.search(document);
        }
        updateMatchLabel();
        if (onSearchChanged != null) {
            onSearchChanged.run();
        }
        // Navigate to first match
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
        if (count == 0) {
            matchCountLabel.setText(searchModel.getQuery().isEmpty() ? "" : "No results");
        } else {
            int current = searchModel.getCurrentMatchIndex() + 1;
            matchCountLabel.setText(current + " of " + count);
        }
    }

    private void configureTextField(TextField field) {
        field.setBackground(new Background(new BackgroundFill(
            CONTROL_BACKGROUND_COLOR,
            CONTROL_RADII,
            Insets.EMPTY
        )));
        field.setBorder(new Border(new BorderStroke(
            CONTROL_BORDER_COLOR,
            BorderStrokeStyle.SOLID,
            CONTROL_RADII,
            BorderWidths.DEFAULT
        )));
    }

    private void configureButton(Button button) {
        button.setBackground(new Background(new BackgroundFill(
            CONTROL_BACKGROUND_COLOR,
            CONTROL_RADII,
            Insets.EMPTY
        )));
        button.setBorder(new Border(new BorderStroke(
            CONTROL_BORDER_COLOR,
            BorderStrokeStyle.SOLID,
            CONTROL_RADII,
            BorderWidths.DEFAULT
        )));
        button.setTextFill(PRIMARY_TEXT_COLOR);
        button.setPadding(BUTTON_PADDING);
    }

    private void configureToggle(CheckBox toggle) {
        toggle.setTextFill(PRIMARY_TEXT_COLOR);
        toggle.setFont(SMALL_FONT);
        toggle.setPadding(CHECKBOX_PADDING);
    }
}
