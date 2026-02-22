package org.metalib.papifly.fx.code.api;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.LineEditService;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.gutter.GutterView;
import org.metalib.papifly.fx.code.gutter.MarkerModel;
import org.metalib.papifly.fx.code.lexer.IncrementalLexerPipeline;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;
import org.metalib.papifly.fx.code.search.SearchController;
import org.metalib.papifly.fx.code.search.SearchModel;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;
import org.metalib.papifly.fx.code.theme.CodeEditorThemeMapper;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Canvas-based code editor component.
 * <p>
 * Renders document text via a virtualized {@link Viewport} and handles
 * keyboard/mouse input for editing, caret movement, and selection.
 * Includes a line number gutter, marker lane, search/replace overlay,
 * and go-to-line navigation.
 */
public class CodeEditor extends StackPane implements DisposableContent {

    private static final String DEFAULT_LANGUAGE = "plain-text";
    private static final double SCROLL_LINE_FACTOR = 3.0;
    private static final int MAX_RESTORED_SECONDARY_CARETS = 2_048;

    private final StringProperty filePath = new SimpleStringProperty(this, "filePath", "");
    private final IntegerProperty cursorLine = new SimpleIntegerProperty(this, "cursorLine", 0);
    private final IntegerProperty cursorColumn = new SimpleIntegerProperty(this, "cursorColumn", 0);
    private final DoubleProperty verticalScrollOffset = new SimpleDoubleProperty(this, "verticalScrollOffset", 0.0);
    private final StringProperty languageId = new SimpleStringProperty(this, "languageId", DEFAULT_LANGUAGE);

    private List<Integer> foldedLines = List.of();

    private final Document document;
    private final Viewport viewport;
    private final SelectionModel selectionModel;
    private final MultiCaretModel multiCaretModel;
    private final GutterView gutterView;
    private final MarkerModel markerModel;
    private final SearchModel searchModel;
    private final SearchController searchController;
    private final GoToLineController goToLineController;
    private final EditorCommandExecutor commandExecutor;
    private final EditorStateCoordinator stateCoordinator;
    private final EditorInputController inputController;
    private final EditorEditController editController;
    private final EditorPointerController pointerController;
    private final EditorCaretCoordinator caretCoordinator;
    private final EditorNavigationController navigationController;
    private final EditorCommandRegistry commandRegistry;
    private final EditorLifecycleService lifecycleService;
    private final EditorSearchCoordinator searchCoordinator;

    private final ChangeListener<Number> caretLineListener;
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) ->
        cursorColumn.set(newValue.intValue());
    private final ChangeListener<Number> scrollOffsetListener = (obs, oldValue, newValue) ->
        applyScrollOffset(newValue.doubleValue());
    private final ChangeListener<String> languageListener;
    private final ChangeListener<Boolean> focusListener;
    private final DocumentChangeListener gutterWidthListener;
    private final DocumentChangeListener searchRefreshListener;
    private final PauseTransition searchRefreshDebounce;
    private final MarkerModel.MarkerChangeListener markerModelChangeListener;
    private final IncrementalLexerPipeline lexerPipeline;

    private ObjectProperty<Theme> boundThemeProperty;
    private ChangeListener<Theme> themeChangeListener;
    private int gutterDigits;
    private boolean disposed;

    /**
     * Creates an empty editor.
     */
    public CodeEditor() {
        this(new Document());
    }

    /**
     * Creates an editor with the given document.
     */
    public CodeEditor(Document document) {
        this(document, null, null, null, null, null);
    }

    CodeEditor(
        Document document,
        SearchModel searchModel,
        SearchController searchController,
        GoToLineController goToLineController,
        BiFunction<Document, Consumer<TokenMap>, IncrementalLexerPipeline> lexerPipelineFactory,
        LineEditService lineEditService
    ) {
        this.document = document == null ? new Document() : document;
        this.selectionModel = new SelectionModel();
        this.multiCaretModel = new MultiCaretModel(selectionModel);
        this.viewport = new Viewport(selectionModel);
        this.viewport.setMultiCaretModel(multiCaretModel);
        this.viewport.setDocument(this.document);
        LineEditService resolvedLineEditService = lineEditService == null ? new LineEditService() : lineEditService;
        this.commandRegistry = new EditorCommandRegistry();
        this.lifecycleService = new EditorLifecycleService();

        // Gutter
        this.markerModel = new MarkerModel();
        this.gutterView = new GutterView(viewport.getGlyphCache());
        this.gutterView.setDocument(this.document);
        this.gutterView.setMarkerModel(markerModel);
        this.gutterDigits = computeGutterDigits(this.document.getLineCount());
        this.gutterWidthListener = event -> refreshGutterWidthIfNeeded();
        this.markerModelChangeListener = gutterView::markDirty;
        this.caretLineListener = (obs, oldValue, newValue) -> {
            cursorLine.set(newValue.intValue());
            gutterView.setActiveLineIndex(newValue.intValue());
        };
        this.caretCoordinator = new EditorCaretCoordinator(
            this.document,
            this.selectionModel,
            this.viewport,
            this.gutterView,
            this.verticalScrollOffset,
            () -> disposed
        );
        this.stateCoordinator = new EditorStateCoordinator(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            this.viewport,
            MAX_RESTORED_SECONDARY_CARETS,
            caretCoordinator::clearPreferredVerticalColumn
        );

        // Search
        this.searchModel = searchModel == null ? new SearchModel() : searchModel;
        this.searchController = searchController == null ? new SearchController(this.searchModel) : searchController;
        this.goToLineController = goToLineController == null ? new GoToLineController() : goToLineController;
        this.goToLineController.setOnGoToLine(this::goToLine);
        this.goToLineController.setOnClose(this::onGoToLineClosed);
        this.searchCoordinator = new EditorSearchCoordinator(
            this.document,
            this.selectionModel,
            this.searchModel,
            this.searchController,
            this.viewport,
            (line, column) -> caretCoordinator.moveCaret(line, column, false),
            this::requestFocus
        );
        this.searchCoordinator.bind();

        // Debounced search refresh on document edits
        this.searchRefreshDebounce = new PauseTransition(Duration.millis(150));
        this.searchRefreshDebounce.setOnFinished(evt -> searchCoordinator.refreshIfOpen());
        this.searchRefreshListener = event -> {
            if (this.searchController.isOpen()) {
                searchRefreshDebounce.playFromStart();
            }
        };

        // Layout: gutter left, viewport center, search overlay on top
        BorderPane editorArea = new BorderPane();
        editorArea.setLeft(gutterView);
        editorArea.setCenter(viewport);

        setMinSize(0, 0);
        setPrefSize(640, 480);
        setFocusTraversable(true);
        getChildren().add(editorArea);

        // Search overlay anchored to top-right
        StackPane.setAlignment(this.searchController, Pos.TOP_RIGHT);
        StackPane.setMargin(this.searchController, new Insets(0, 16, 0, 0));
        getChildren().add(this.searchController);
        // Go-to-line overlay anchored to top-right
        StackPane.setAlignment(this.goToLineController, Pos.TOP_RIGHT);
        StackPane.setMargin(this.goToLineController, new Insets(6, 16, 0, 0));
        getChildren().add(this.goToLineController);

        BiFunction<Document, Consumer<TokenMap>, IncrementalLexerPipeline> resolvedLexerPipelineFactory =
            lexerPipelineFactory == null ? IncrementalLexerPipeline::new : lexerPipelineFactory;
        this.lexerPipeline = resolvedLexerPipelineFactory.apply(this.document, viewport::setTokenMap);
        this.languageListener = (obs, oldValue, newValue) -> lexerPipeline.setLanguageId(newValue);
        this.focusListener = (obs, oldFocused, focused) -> viewport.setCaretBlinkActive(focused);
        lexerPipeline.setLanguageId(languageId.get());
        OccurrenceSelectionService resolvedOccurrenceSelectionService = new OccurrenceSelectionService(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            viewport::markDirty
        );
        this.editController = createEditController();
        this.navigationController = new EditorNavigationController(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            resolvedLineEditService,
            resolvedOccurrenceSelectionService,
            this.caretCoordinator,
            this.viewport::markDirty,
            this::setVerticalScrollOffset,
            this.viewport::getScrollOffset
        );
        this.pointerController = createPointerController();
        this.commandExecutor = createCommandExecutor();
        this.inputController = createInputController();
        lifecycleService.bindListeners(
            selectionModel,
            caretLineListener,
            caretColumnListener,
            this.document,
            gutterWidthListener,
            searchRefreshListener,
            markerModel,
            markerModelChangeListener,
            verticalScrollOffset,
            scrollOffsetListener,
            languageId,
            languageListener
        );
        lifecycleService.bindInputHandlers(
            this,
            event -> inputController.handleKeyPressed(event),
            event -> inputController.handleKeyTyped(event),
            event -> pointerController.handleMousePressed(event),
            event -> pointerController.handleMouseDragged(event),
            event -> pointerController.handleMouseReleased(),
            event -> pointerController.handleScroll(event),
            focusListener,
            () -> viewport.setCaretBlinkActive(isFocused())
        );
    }

    /**
     * Returns the document model.
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Convenience: sets document text content.
     */
    public void setText(String text) {
        document.setText(text);
        caretCoordinator.clearPreferredVerticalColumn();
        selectionModel.moveCaret(0, 0);
        setVerticalScrollOffset(0);
        gutterView.recomputeWidth();
    }

    /**
     * Convenience: returns document text content.
     */
    public String getText() {
        return document.getText();
    }

    /**
     * Returns the viewport for direct access.
     */
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * Returns the selection model.
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Returns the multi-caret model.
     */
    public MultiCaretModel getMultiCaretModel() {
        return multiCaretModel;
    }

    /**
     * Returns the gutter view.
     */
    public GutterView getGutterView() {
        return gutterView;
    }

    /**
     * Returns the marker model.
     */
    public MarkerModel getMarkerModel() {
        return markerModel;
    }

    /**
     * Returns the search model.
     */
    public SearchModel getSearchModel() {
        return searchModel;
    }

    /**
     * Returns the search controller (overlay UI).
     */
    public SearchController getSearchController() {
        return searchController;
    }

    /**
     * Returns the go-to-line overlay controller.
     */
    public GoToLineController getGoToLineController() {
        return goToLineController;
    }

    /**
     * Binds this editor to a docking {@link Theme} property.
     * <p>
     * The editor listens for changes and maps each new {@link Theme} to a
     * {@link CodeEditorTheme} via {@link CodeEditorThemeMapper}, refreshing
     * all visual components at runtime.
     */
    public void bindThemeProperty(ObjectProperty<Theme> themeProperty) {
        unbindThemeProperty();
        if (themeProperty == null) {
            return;
        }
        this.boundThemeProperty = themeProperty;
        this.themeChangeListener = (obs, oldTheme, newTheme) -> applyDockingTheme(newTheme);
        themeProperty.addListener(themeChangeListener);
        applyDockingTheme(themeProperty.get());
    }

    /**
     * Unbinds a previously bound docking theme property.
     */
    public void unbindThemeProperty() {
        if (boundThemeProperty != null && themeChangeListener != null) {
            boundThemeProperty.removeListener(themeChangeListener);
        }
        boundThemeProperty = null;
        themeChangeListener = null;
    }

    /**
     * Directly applies a {@link CodeEditorTheme} to the editor and its sub-components.
     */
    public void setEditorTheme(CodeEditorTheme editorTheme) {
        CodeEditorTheme resolved = editorTheme == null ? CodeEditorTheme.dark() : editorTheme;
        viewport.setTheme(resolved);
        gutterView.setTheme(resolved);
        searchController.setTheme(resolved);
        goToLineController.setTheme(resolved);
    }

    /**
     * Returns the current editor theme from the viewport.
     */
    public CodeEditorTheme getEditorTheme() {
        return viewport.getTheme();
    }

    private void applyDockingTheme(Theme dockingTheme) {
        setEditorTheme(CodeEditorThemeMapper.map(dockingTheme));
    }

    /**
     * Opens the search/replace overlay. Shortcut: Ctrl/Cmd+F.
     */
    public void openSearch() {
        goToLineController.close();
        String selectedText = selectionModel.hasSelection()
            ? selectionModel.getSelectedText(document)
            : null;
        searchController.open(selectedText);
    }

    /**
     * Opens the search/replace overlay in replace mode. Shortcut: Ctrl+H / Cmd+Option+F.
     */
    public void openReplace() {
        goToLineController.close();
        String selectedText = selectionModel.hasSelection()
            ? selectionModel.getSelectedText(document)
            : null;
        searchController.openInReplaceMode(selectedText);
    }

    /**
     * Opens a go-to-line dialog. Shortcut: Ctrl/Cmd+G.
     */
    public void goToLine() {
        searchController.close();
        goToLineController.open(selectionModel.getCaretLine() + 1, document.getLineCount());
    }

    /**
     * Navigates to the specified 1-based line number.
     */
    public void goToLine(int lineNumber) {
        int targetLine = Math.max(1, Math.min(lineNumber, document.getLineCount()));
        caretCoordinator.moveCaret(targetLine - 1, 0, false);
    }

    // --- Key handling ---

    /**
     * Dispatches an {@link EditorCommand} to the appropriate handler method.
     */
    void executeCommand(EditorCommand cmd) {
        commandExecutor.execute(cmd);
    }

    private EditorCommandExecutor createCommandExecutor() {
        EditorCommandExecutor executor = new EditorCommandExecutor(
            multiCaretModel,
            caretCoordinator::clearPreferredVerticalColumn,
            commandRegistry::isVerticalCaretCommand,
            () -> disposed
        );
        commandRegistry.registerDefault(
            executor,
            navigationController,
            editController,
            this::openSearch,
            this::openReplace,
            this::goToLine
        );
        return executor;
    }

    private EditorInputController createInputController() {
        return new EditorInputController(
            () -> disposed,
            searchController::isOpen,
            searchController::close,
            goToLineController::isOpen,
            goToLineController::close,
            () -> (searchController.isOpen() && searchController.isFocusWithin())
                || (goToLineController.isOpen() && goToLineController.isFocusWithin()),
            this::requestFocus,
            viewport::resetCaretBlink,
            this::executeCommand,
            editController::insertTypedCharacter
        );
    }

    private EditorEditController createEditController() {
        return new EditorEditController(
            document,
            selectionModel,
            multiCaretModel,
            viewport::markDirty,
            caretCoordinator::moveCaretToOffset,
            () -> Clipboard.getSystemClipboard().getString(),
            this::putClipboardText
        );
    }

    private EditorPointerController createPointerController() {
        return new EditorPointerController(
            () -> disposed,
            caretCoordinator::clearPreferredVerticalColumn,
            this::requestFocus,
            viewport::resetCaretBlink,
            viewport,
            document,
            selectionModel,
            multiCaretModel,
            viewport::markDirty,
            this::setVerticalScrollOffset,
            SCROLL_LINE_FACTOR
        );
    }

    private void putClipboardText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void onGoToLineClosed() {
        requestFocus();
    }

    private void applyScrollOffset(double requestedOffset) {
        caretCoordinator.applyScrollOffset(requestedOffset);
    }

    private void syncVerticalScrollOffsetFromViewport() {
        caretCoordinator.syncVerticalScrollOffsetFromViewport();
    }

    // --- State properties ---

    /**
     * Captures current editor state into a serializable DTO.
     */
    public EditorStateData captureState() {
        syncVerticalScrollOffsetFromViewport();
        return stateCoordinator.captureState(filePath::get, languageId::get, this::getFoldedLines);
    }

    /**
     * Applies state to the editor.
     */
    public void applyState(EditorStateData state) {
        stateCoordinator.applyState(state, this::setFilePath, this::setLanguageId, this::setFoldedLines,
            this::setVerticalScrollOffset);
    }

    public String getFilePath() {
        return filePath.get();
    }

    public void setFilePath(String filePath) {
        this.filePath.set(filePath == null ? "" : filePath);
    }

    public StringProperty filePathProperty() {
        return filePath;
    }

    public int getCursorLine() {
        return cursorLine.get();
    }

    public void setCursorLine(int cursorLine) {
        stateCoordinator.movePrimaryCaret(cursorLine, selectionModel.getCaretColumn());
    }

    public IntegerProperty cursorLineProperty() {
        return cursorLine;
    }

    public int getCursorColumn() {
        return cursorColumn.get();
    }

    public void setCursorColumn(int cursorColumn) {
        stateCoordinator.movePrimaryCaret(selectionModel.getCaretLine(), cursorColumn);
    }

    public IntegerProperty cursorColumnProperty() {
        return cursorColumn;
    }

    public double getVerticalScrollOffset() {
        return verticalScrollOffset.get();
    }

    public void setVerticalScrollOffset(double verticalScrollOffset) {
        double safeOffset = Math.max(0.0, verticalScrollOffset);
        if (Double.compare(this.verticalScrollOffset.get(), safeOffset) == 0) {
            return;
        }
        this.verticalScrollOffset.set(safeOffset);
    }

    public DoubleProperty verticalScrollOffsetProperty() {
        return verticalScrollOffset;
    }

    public String getLanguageId() {
        return languageId.get();
    }

    public void setLanguageId(String languageId) {
        this.languageId.set(languageId == null || languageId.isBlank() ? DEFAULT_LANGUAGE : languageId);
    }

    public StringProperty languageIdProperty() {
        return languageId;
    }

    public List<Integer> getFoldedLines() {
        return foldedLines;
    }

    public void setFoldedLines(List<Integer> foldedLines) {
        this.foldedLines = foldedLines == null ? List.of() : List.copyOf(foldedLines);
    }

    /**
     * Releases listeners and rendering resources associated with this editor.
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        pointerController.dispose();
        multiCaretModel.clearSecondaryCarets();
        searchCoordinator.clearAndDispose();
        goToLineController.setOnGoToLine(null);
        goToLineController.setOnClose(null);
        goToLineController.close();
        searchRefreshDebounce.stop();
        unbindThemeProperty();
        lifecycleService.unbindInputHandlers(this, focusListener);
        viewport.setCaretBlinkActive(false);
        lifecycleService.unbindListeners(
            selectionModel,
            caretLineListener,
            caretColumnListener,
            document,
            gutterWidthListener,
            searchRefreshListener,
            markerModel,
            markerModelChangeListener,
            verticalScrollOffset,
            scrollOffsetListener,
            languageId,
            languageListener
        );
        lexerPipeline.dispose();
        viewport.dispose();
    }

    private void refreshGutterWidthIfNeeded() {
        int nextDigits = computeGutterDigits(document.getLineCount());
        if (nextDigits == gutterDigits) {
            return;
        }
        gutterDigits = nextDigits;
        if (Platform.isFxApplicationThread()) {
            gutterView.recomputeWidth();
            return;
        }
        Platform.runLater(gutterView::recomputeWidth);
    }

    private static int computeGutterDigits(int lineCount) {
        return Math.max(2, String.valueOf(Math.max(1, lineCount)).length());
    }
}
