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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.LineBlock;
import org.metalib.papifly.fx.code.command.LineEditService;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.command.WordBoundary;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.gutter.GutterView;
import org.metalib.papifly.fx.code.gutter.MarkerModel;
import org.metalib.papifly.fx.code.lexer.IncrementalLexerPipeline;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;
import org.metalib.papifly.fx.code.search.SearchController;
import org.metalib.papifly.fx.code.search.SearchMatch;
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
    private final LineEditService lineEditService;
    private final EditorCommandExecutor commandExecutor;
    private final EditorStateCoordinator stateCoordinator;
    private final EditorInputController inputController;
    private final EditorEditController editController;
    private final EditorPointerController pointerController;

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
    private boolean syncingScrollOffset;
    private boolean disposed;
    private int preferredVerticalColumn = -1;

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
        this.lineEditService = lineEditService == null ? new LineEditService() : lineEditService;
        this.stateCoordinator = new EditorStateCoordinator(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            this.viewport,
            MAX_RESTORED_SECONDARY_CARETS,
            this::clearPreferredVerticalColumn
        );

        // Gutter
        this.markerModel = new MarkerModel();
        this.gutterView = new GutterView(viewport.getGlyphCache());
        this.gutterView.setDocument(this.document);
        this.gutterView.setMarkerModel(markerModel);
        this.gutterDigits = computeGutterDigits(this.document.getLineCount());
        this.gutterWidthListener = event -> refreshGutterWidthIfNeeded();
        this.document.addChangeListener(gutterWidthListener);
        this.markerModelChangeListener = gutterView::markDirty;
        this.markerModel.addChangeListener(markerModelChangeListener);
        this.caretLineListener = (obs, oldValue, newValue) -> {
            cursorLine.set(newValue.intValue());
            gutterView.setActiveLineIndex(newValue.intValue());
        };

        // Search
        this.searchModel = searchModel == null ? new SearchModel() : searchModel;
        this.searchController = searchController == null ? new SearchController(this.searchModel) : searchController;
        this.searchController.setDocument(this.document);
        this.searchController.setSelectionRangeSupplier(this::currentSelectionRange);
        this.searchController.setOnNavigate(this::navigateToSearchMatch);
        this.searchController.setOnClose(this::onSearchClosed);
        this.searchController.setOnSearchChanged(this::onSearchResultsChanged);
        this.goToLineController = goToLineController == null ? new GoToLineController() : goToLineController;
        this.goToLineController.setOnGoToLine(this::goToLine);
        this.goToLineController.setOnClose(this::onGoToLineClosed);

        // Debounced search refresh on document edits
        this.searchRefreshDebounce = new PauseTransition(Duration.millis(150));
        this.searchRefreshDebounce.setOnFinished(evt -> refreshSearchIfOpen());
        this.searchRefreshListener = event -> {
            if (this.searchController.isOpen()) {
                searchRefreshDebounce.playFromStart();
            }
        };
        this.document.addChangeListener(searchRefreshListener);

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

        // Bind cursor properties to selection model
        selectionModel.caretLineProperty().addListener(caretLineListener);
        selectionModel.caretColumnProperty().addListener(caretColumnListener);

        // Bind vertical scroll offset
        verticalScrollOffset.addListener(scrollOffsetListener);

        BiFunction<Document, Consumer<TokenMap>, IncrementalLexerPipeline> resolvedLexerPipelineFactory =
            lexerPipelineFactory == null ? IncrementalLexerPipeline::new : lexerPipelineFactory;
        this.lexerPipeline = resolvedLexerPipelineFactory.apply(this.document, viewport::setTokenMap);
        this.languageListener = (obs, oldValue, newValue) -> lexerPipeline.setLanguageId(newValue);
        this.focusListener = (obs, oldFocused, focused) -> viewport.setCaretBlinkActive(focused);
        languageId.addListener(languageListener);
        lexerPipeline.setLanguageId(languageId.get());
        this.editController = createEditController();
        this.pointerController = createPointerController();
        this.commandExecutor = createCommandExecutor();
        this.inputController = createInputController();

        // Input handlers
        setOnKeyPressed(this::handleKeyPressed);
        setOnKeyTyped(this::handleKeyTyped);
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnScroll(this::handleScroll);
        focusedProperty().addListener(focusListener);
        viewport.setCaretBlinkActive(isFocused());
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
        clearPreferredVerticalColumn();
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
        moveCaret(targetLine - 1, 0, false);
    }

    // --- Key handling ---

    private void handleKeyTyped(KeyEvent event) {
        inputController.handleKeyTyped(event);
    }

    private void handleKeyPressed(KeyEvent event) {
        inputController.handleKeyPressed(event);
    }

    /**
     * Dispatches an {@link EditorCommand} to the appropriate handler method.
     */
    void executeCommand(EditorCommand cmd) {
        commandExecutor.execute(cmd);
    }

    private EditorCommandExecutor createCommandExecutor() {
        EditorCommandExecutor executor = new EditorCommandExecutor(
            multiCaretModel,
            this::clearPreferredVerticalColumn,
            this::isVerticalCaretCommand,
            () -> disposed
        );

        // Navigation
        executor.register(EditorCommand.MOVE_LEFT, () -> handleLeft(false));
        executor.register(EditorCommand.MOVE_RIGHT, () -> handleRight(false));
        executor.register(EditorCommand.MOVE_UP, () -> handleUp(false));
        executor.register(EditorCommand.MOVE_DOWN, () -> handleDown(false));
        executor.register(EditorCommand.MOVE_PAGE_UP, () -> handlePageUp(false));
        executor.register(EditorCommand.MOVE_PAGE_DOWN, () -> handlePageDown(false));
        executor.register(EditorCommand.SELECT_LEFT, () -> handleLeft(true));
        executor.register(EditorCommand.SELECT_RIGHT, () -> handleRight(true));
        executor.register(EditorCommand.SELECT_UP, () -> handleUp(true));
        executor.register(EditorCommand.SELECT_DOWN, () -> handleDown(true));
        executor.register(EditorCommand.SELECT_PAGE_UP, () -> handlePageUp(true));
        executor.register(EditorCommand.SELECT_PAGE_DOWN, () -> handlePageDown(true));
        executor.register(EditorCommand.SCROLL_PAGE_UP, this::handleScrollPageUp);
        executor.register(EditorCommand.SCROLL_PAGE_DOWN, this::handleScrollPageDown);
        executor.register(EditorCommand.LINE_START, () -> handleHome(false));
        executor.register(EditorCommand.LINE_END, () -> handleEnd(false));
        executor.register(EditorCommand.SELECT_TO_LINE_START, () -> handleHome(true));
        executor.register(EditorCommand.SELECT_TO_LINE_END, () -> handleEnd(true));

        // Editing
        executor.register(EditorCommand.BACKSPACE, this::handleBackspace);
        executor.register(EditorCommand.DELETE, this::handleDelete);
        executor.register(EditorCommand.ENTER, this::handleEnter);

        // Clipboard and undo
        executor.register(EditorCommand.SELECT_ALL, this::handleSelectAll);
        executor.register(EditorCommand.UNDO, this::handleUndo);
        executor.register(EditorCommand.REDO, this::handleRedo);
        executor.register(EditorCommand.COPY, this::handleCopy);
        executor.register(EditorCommand.CUT, this::handleCut);
        executor.register(EditorCommand.PASTE, this::handlePaste);

        // Search
        executor.register(EditorCommand.OPEN_SEARCH, this::openSearch);
        executor.register(EditorCommand.OPEN_REPLACE, this::openReplace);
        executor.register(EditorCommand.GO_TO_LINE, this::goToLine);

        // Word navigation
        executor.register(EditorCommand.MOVE_WORD_LEFT, this::handleMoveWordLeft);
        executor.register(EditorCommand.MOVE_WORD_RIGHT, this::handleMoveWordRight);
        executor.register(EditorCommand.SELECT_WORD_LEFT, this::handleSelectWordLeft);
        executor.register(EditorCommand.SELECT_WORD_RIGHT, this::handleSelectWordRight);
        executor.register(EditorCommand.DELETE_WORD_LEFT, this::handleDeleteWordLeft);
        executor.register(EditorCommand.DELETE_WORD_RIGHT, this::handleDeleteWordRight);

        // Document boundaries
        executor.register(EditorCommand.DOCUMENT_START, () -> handleDocumentStart(false));
        executor.register(EditorCommand.DOCUMENT_END, () -> handleDocumentEnd(false));
        executor.register(EditorCommand.SELECT_TO_DOCUMENT_START, () -> handleDocumentStart(true));
        executor.register(EditorCommand.SELECT_TO_DOCUMENT_END, () -> handleDocumentEnd(true));

        // Line operations
        executor.register(EditorCommand.DELETE_LINE, this::handleDeleteLine);
        executor.register(EditorCommand.MOVE_LINE_UP, this::handleMoveLineUp);
        executor.register(EditorCommand.MOVE_LINE_DOWN, this::handleMoveLineDown);
        executor.register(EditorCommand.DUPLICATE_LINE_UP, this::handleDuplicateLineUp);
        executor.register(EditorCommand.DUPLICATE_LINE_DOWN, this::handleDuplicateLineDown);
        executor.register(EditorCommand.JOIN_LINES, this::handleJoinLines);

        // Multi-caret
        executor.register(EditorCommand.SELECT_NEXT_OCCURRENCE, this::handleSelectNextOccurrence);
        executor.register(EditorCommand.SELECT_ALL_OCCURRENCES, this::handleSelectAllOccurrences);
        executor.register(EditorCommand.ADD_CURSOR_UP, this::handleAddCursorUp);
        executor.register(EditorCommand.ADD_CURSOR_DOWN, this::handleAddCursorDown);
        executor.register(EditorCommand.UNDO_LAST_OCCURRENCE, this::handleUndoLastOccurrence);

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
            this::insertTypedCharacter
        );
    }

    private EditorEditController createEditController() {
        return new EditorEditController(
            document,
            selectionModel,
            multiCaretModel,
            viewport::markDirty,
            this::moveCaretToOffset,
            () -> Clipboard.getSystemClipboard().getString(),
            this::putClipboardText
        );
    }

    private EditorPointerController createPointerController() {
        return new EditorPointerController(
            () -> disposed,
            this::clearPreferredVerticalColumn,
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

    private void insertTypedCharacter(String character) {
        editController.insertTypedCharacter(character);
    }

    private void handleBackspace() {
        editController.handleBackspace();
    }

    private void handleDelete() {
        editController.handleDelete();
    }

    private void handleEnter() {
        editController.handleEnter();
    }

    private void handleLeft(boolean shift) {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        if (col > 0) {
            moveCaret(line, col - 1, shift);
        } else if (line > 0) {
            int prevLineLen = document.getLineText(line - 1).length();
            moveCaret(line - 1, prevLineLen, shift);
        }
    }

    private void handleRight(boolean shift) {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        int lineLen = document.getLineText(line).length();
        if (col < lineLen) {
            moveCaret(line, col + 1, shift);
        } else if (line < document.getLineCount() - 1) {
            moveCaret(line + 1, 0, shift);
        }
    }

    private void handleUp(boolean shift) {
        int line = selectionModel.getCaretLine();
        if (line > 0) {
            moveCaretVertically(line - 1, shift);
        }
    }

    private void handleDown(boolean shift) {
        int line = selectionModel.getCaretLine();
        if (line < document.getLineCount() - 1) {
            moveCaretVertically(line + 1, shift);
        }
    }

    private void handlePageUp(boolean shift) {
        handlePageMove(-1, shift);
    }

    private void handlePageDown(boolean shift) {
        handlePageMove(1, shift);
    }

    private void handlePageMove(int direction, boolean shift) {
        int lineDelta = computePageLineDelta();
        int caretLine = selectionModel.getCaretLine();
        int targetLine = clampLine(caretLine + (direction * lineDelta));
        moveCaretVertically(targetLine, shift);
    }

    private void handleScrollPageUp() {
        handleScrollPage(-1);
    }

    private void handleScrollPageDown() {
        handleScrollPage(1);
    }

    private void handleScrollPage(int direction) {
        double pagePixels = computePagePixelDelta();
        double newOffset = viewport.getScrollOffset() + (direction * pagePixels);
        setVerticalScrollOffset(newOffset);
    }

    private void handleHome(boolean shift) {
        moveCaret(selectionModel.getCaretLine(), 0, shift);
    }

    private void handleEnd(boolean shift) {
        int line = selectionModel.getCaretLine();
        moveCaret(line, document.getLineText(line).length(), shift);
    }

    private void handleSelectAll() {
        selectionModel.selectAll(document);
        viewport.markDirty();
    }

    private void handleUndo() {
        int beforeLength = document.length();
        int beforeOffset = selectionModel.getCaretOffset(document);
        if (document.undo()) {
            int afterLength = document.length();
            int targetOffset = Math.max(0, Math.min(beforeOffset + (afterLength - beforeLength), afterLength));
            moveCaretToOffset(targetOffset);
        }
    }

    private void handleRedo() {
        int beforeLength = document.length();
        int beforeOffset = selectionModel.getCaretOffset(document);
        if (document.redo()) {
            int afterLength = document.length();
            int targetOffset = Math.max(0, Math.min(beforeOffset + (afterLength - beforeLength), afterLength));
            moveCaretToOffset(targetOffset);
        }
    }

    private void handleCopy() {
        editController.handleCopy();
    }

    private void handleCut() {
        editController.handleCut();
    }

    private void handlePaste() {
        editController.handlePaste();
    }

    // --- Phase 1: word / document navigation ---

    private void handleMoveWordLeft() {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        int target = WordBoundary.findWordLeft(document.getLineText(line), col);
        if (target == col && line > 0) {
            // At line start — jump to end of previous line
            line--;
            target = document.getLineText(line).length();
        }
        moveCaret(line, target, false);
    }

    private void handleMoveWordRight() {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        int target = WordBoundary.findWordRight(lineText, col);
        if (target == col && line < document.getLineCount() - 1) {
            // At line end — jump to start of next line
            line++;
            target = 0;
        }
        moveCaret(line, target, false);
    }

    private void handleSelectWordLeft() {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        int target = WordBoundary.findWordLeft(document.getLineText(line), col);
        if (target == col && line > 0) {
            line--;
            target = document.getLineText(line).length();
        }
        moveCaret(line, target, true);
    }

    private void handleSelectWordRight() {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        int target = WordBoundary.findWordRight(lineText, col);
        if (target == col && line < document.getLineCount() - 1) {
            line++;
            target = 0;
        }
        moveCaret(line, target, true);
    }

    private void handleDeleteWordLeft() {
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        int targetCol = WordBoundary.findWordLeft(document.getLineText(line), col);
        if (targetCol == col && line > 0) {
            // Delete back to end of previous line (joining lines)
            int caretOffset = selectionModel.getCaretOffset(document);
            document.delete(caretOffset - 1, caretOffset);
            moveCaretToOffset(caretOffset - 1);
            return;
        }
        int lineStart = document.getLineStartOffset(line);
        document.delete(lineStart + targetCol, lineStart + col);
        moveCaret(line, targetCol, false);
    }

    private void handleDeleteWordRight() {
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        int targetCol = WordBoundary.findWordRight(lineText, col);
        if (targetCol == col && line < document.getLineCount() - 1) {
            // Delete forward to start of next line (joining lines)
            int caretOffset = selectionModel.getCaretOffset(document);
            document.delete(caretOffset, caretOffset + 1);
            return;
        }
        int lineStart = document.getLineStartOffset(line);
        document.delete(lineStart + col, lineStart + targetCol);
    }

    private void handleDocumentStart(boolean shift) {
        moveCaret(0, 0, shift);
    }

    private void handleDocumentEnd(boolean shift) {
        int lastLine = document.getLineCount() - 1;
        int lastCol = document.getLineText(lastLine).length();
        moveCaret(lastLine, lastCol, shift);
    }

    // --- Phase 2: line operations ---

    private void handleDeleteLine() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        if (lineEditService.deleteBlock(document, block)) {
            int targetLine = Math.min(block.startLine(), document.getLineCount() - 1);
            moveCaret(targetLine, 0, false);
        }
    }

    private void handleMoveLineUp() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        if (!lineEditService.moveBlockUp(document, block)) {
            return;
        }
        int col = selectionModel.getCaretColumn();
        moveCaret(selectionModel.getCaretLine() - 1, col, false);
    }

    private void handleMoveLineDown() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        if (!lineEditService.moveBlockDown(document, block)) {
            return;
        }
        int col = selectionModel.getCaretColumn();
        moveCaret(selectionModel.getCaretLine() + 1, col, false);
    }

    private void handleDuplicateLineUp() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        lineEditService.duplicateBlockUp(document, block);
    }

    private void handleDuplicateLineDown() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        lineEditService.duplicateBlockDown(document, block);
        int linesInserted = block.lineCount();
        int col = selectionModel.getCaretColumn();
        moveCaret(selectionModel.getCaretLine() + linesInserted, col, false);
    }

    private void handleJoinLines() {
        lineEditService.joinLineWithNext(document, selectionModel.getCaretLine());
    }

    // --- Phase 3: multi-caret handlers ---

    private void handleSelectNextOccurrence() {
        String selectedText;
        if (selectionModel.hasSelection()) {
            selectedText = selectionModel.getSelectedText(document);
        } else {
            // Select the word under the caret
            int line = selectionModel.getCaretLine();
            int col = selectionModel.getCaretColumn();
            String lineText = document.getLineText(line);
            if (lineText.isEmpty() || col >= lineText.length() && col > 0) {
                return;
            }
            int wordStart = col;
            while (wordStart > 0 && WordBoundary.isWordChar(lineText.charAt(wordStart - 1))) {
                wordStart--;
            }
            int wordEnd = col;
            while (wordEnd < lineText.length() && WordBoundary.isWordChar(lineText.charAt(wordEnd))) {
                wordEnd++;
            }
            if (wordStart == wordEnd) {
                return;
            }
            // Select the word under caret as primary
            selectionModel.moveCaret(line, wordStart);
            selectionModel.moveCaretWithSelection(line, wordEnd);
            viewport.markDirty();
            return;
        }

        if (selectedText.isEmpty()) {
            return;
        }

        // Find next occurrence after the last caret
        String fullText = document.getText();
        List<CaretRange> all = multiCaretModel.allCarets(document);
        CaretRange lastCaret = all.get(all.size() - 1);
        int searchFrom = lastCaret.getEndOffset(document);

        int found = fullText.indexOf(selectedText, searchFrom);
        if (found < 0) {
            // Wrap around
            found = fullText.indexOf(selectedText);
        }
        if (found < 0) {
            return;
        }

        // Check we don't already have a caret at this position
        int foundEnd = found + selectedText.length();
        int anchorLine = document.getLineForOffset(found);
        int anchorCol = document.getColumnForOffset(found);
        int caretLine = document.getLineForOffset(foundEnd);
        int caretCol = document.getColumnForOffset(foundEnd);
        CaretRange newCaret = new CaretRange(anchorLine, anchorCol, caretLine, caretCol);

        // Don't add duplicates
        for (CaretRange existing : all) {
            if (existing.getStartOffset(document) == found && existing.getEndOffset(document) == foundEnd) {
                return;
            }
        }

        multiCaretModel.addCaret(newCaret);
        viewport.markDirty();
    }

    private void handleSelectAllOccurrences() {
        String selectedText;
        if (selectionModel.hasSelection()) {
            selectedText = selectionModel.getSelectedText(document);
        } else {
            // Select word under caret first (same as first Ctrl+D)
            handleSelectNextOccurrence();
            if (!selectionModel.hasSelection()) {
                return;
            }
            selectedText = selectionModel.getSelectedText(document);
        }

        if (selectedText.isEmpty()) {
            return;
        }

        String fullText = document.getText();
        int searchFrom = 0;
        boolean first = true;
        while (true) {
            int found = fullText.indexOf(selectedText, searchFrom);
            if (found < 0) {
                break;
            }
            int foundEnd = found + selectedText.length();
            int anchorLine = document.getLineForOffset(found);
            int anchorCol = document.getColumnForOffset(found);
            int caretLine = document.getLineForOffset(foundEnd);
            int caretCol = document.getColumnForOffset(foundEnd);

            if (first) {
                // Make the first occurrence the primary
                selectionModel.moveCaret(anchorLine, anchorCol);
                selectionModel.moveCaretWithSelection(caretLine, caretCol);
                first = false;
            } else {
                multiCaretModel.addCaretNoStack(
                    new CaretRange(anchorLine, anchorCol, caretLine, caretCol));
            }
            searchFrom = foundEnd;
        }
        viewport.markDirty();
    }

    private void handleAddCursorUp() {
        int caretLine = selectionModel.getCaretLine();
        int caretCol = selectionModel.getCaretColumn();
        if (caretLine <= 0) {
            return;
        }
        int newLine = caretLine - 1;
        int newCol = Math.min(caretCol, document.getLineText(newLine).length());
        multiCaretModel.addCaret(new CaretRange(newLine, newCol, newLine, newCol));
        viewport.markDirty();
    }

    private void handleAddCursorDown() {
        int caretLine = selectionModel.getCaretLine();
        int caretCol = selectionModel.getCaretColumn();
        if (caretLine >= document.getLineCount() - 1) {
            return;
        }
        int newLine = caretLine + 1;
        int newCol = Math.min(caretCol, document.getLineText(newLine).length());
        multiCaretModel.addCaret(new CaretRange(newLine, newCol, newLine, newCol));
        viewport.markDirty();
    }

    private void handleUndoLastOccurrence() {
        multiCaretModel.undoLastOccurrence();
        viewport.markDirty();
    }

    // --- Mouse handling ---

    private void handleMousePressed(MouseEvent event) {
        pointerController.handleMousePressed(event);
    }

    private void handleMouseDragged(MouseEvent event) {
        pointerController.handleMouseDragged(event);
    }

    private void handleMouseReleased(MouseEvent event) {
        pointerController.handleMouseReleased();
    }

    private void handleScroll(ScrollEvent event) {
        pointerController.handleScroll(event);
    }

    // --- Search navigation ---

    private void navigateToSearchMatch(SearchMatch match) {
        moveCaret(match.line(), match.startColumn(), false);
        onSearchResultsChanged();
    }

    private void refreshSearchIfOpen() {
        if (!searchController.isOpen()) {
            return;
        }
        int caretOffset = selectionModel.getCaretOffset(document);
        searchController.refreshSelectionScope();
        searchModel.search(document);
        searchModel.selectNearestMatch(caretOffset);
        onSearchResultsChanged();
        searchController.refreshMatchDisplay();
    }

    private void onSearchClosed() {
        viewport.setSearchMatches(List.of(), -1);
        requestFocus();
    }

    private void onGoToLineClosed() {
        requestFocus();
    }

    private void onSearchResultsChanged() {
        viewport.setSearchMatches(searchModel.getMatches(), searchModel.getCurrentMatchIndex());
    }

    private int[] currentSelectionRange() {
        if (!selectionModel.hasSelection()) {
            return null;
        }
        return new int[]{
            selectionModel.getSelectionStartOffset(document),
            selectionModel.getSelectionEndOffset(document)
        };
    }

    // --- Helpers ---

    private void moveCaret(int line, int col, boolean extendSelection) {
        clearPreferredVerticalColumn();
        moveCaretInternal(line, col, extendSelection);
    }

    private void moveCaretVertically(int targetLine, boolean extendSelection) {
        int safeLine = clampLine(targetLine);
        int preferredColumn = preferredVerticalColumn >= 0
            ? preferredVerticalColumn
            : selectionModel.getCaretColumn();
        int targetColumn = Math.min(preferredColumn, document.getLineText(safeLine).length());
        moveCaretInternal(safeLine, targetColumn, extendSelection);
        preferredVerticalColumn = preferredColumn;
    }

    private void moveCaretInternal(int line, int col, boolean extendSelection) {
        int safeLine = clampLine(line);
        int safeColumn = clampColumn(safeLine, col);
        if (extendSelection) {
            selectionModel.moveCaretWithSelection(safeLine, safeColumn);
        } else {
            selectionModel.moveCaret(safeLine, safeColumn);
        }
        viewport.ensureCaretVisible();
        syncVerticalScrollOffsetFromViewport();
        syncGutterScroll();
    }

    private void moveCaretToOffset(int offset) {
        clearPreferredVerticalColumn();
        offset = Math.max(0, Math.min(offset, document.length()));
        int line = document.getLineForOffset(offset);
        int col = document.getColumnForOffset(offset);
        selectionModel.moveCaret(line, col);
        viewport.ensureCaretVisible();
        syncVerticalScrollOffsetFromViewport();
        syncGutterScroll();
    }

    private boolean isVerticalCaretCommand(EditorCommand cmd) {
        return switch (cmd) {
            case MOVE_UP, MOVE_DOWN, SELECT_UP, SELECT_DOWN,
                 MOVE_PAGE_UP, MOVE_PAGE_DOWN, SELECT_PAGE_UP, SELECT_PAGE_DOWN -> true;
            default -> false;
        };
    }

    private void clearPreferredVerticalColumn() {
        preferredVerticalColumn = -1;
    }

    private int clampLine(int line) {
        int maxLine = Math.max(0, document.getLineCount() - 1);
        return Math.max(0, Math.min(line, maxLine));
    }

    private int clampColumn(int line, int column) {
        int maxColumn = document.getLineText(line).length();
        return Math.max(0, Math.min(column, maxColumn));
    }

    private void applyScrollOffset(double requestedOffset) {
        if (disposed) {
            return;
        }
        if (syncingScrollOffset) {
            return;
        }
        viewport.setScrollOffset(requestedOffset);
        syncVerticalScrollOffsetFromViewport();
        syncGutterScroll();
    }

    private void syncVerticalScrollOffsetFromViewport() {
        double actualOffset = viewport.getScrollOffset();
        if (Double.compare(verticalScrollOffset.get(), actualOffset) == 0) {
            return;
        }
        syncingScrollOffset = true;
        try {
            verticalScrollOffset.set(actualOffset);
        } finally {
            syncingScrollOffset = false;
        }
    }

    private void syncGutterScroll() {
        gutterView.setScrollOffset(viewport.getScrollOffset());
    }

    private int computePageLineDelta() {
        double lineHeight = viewport.getGlyphCache().getLineHeight();
        if (lineHeight <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(computePagePixelDelta() / lineHeight));
    }

    private double computePagePixelDelta() {
        double viewportHeight = viewport.getHeight();
        if (viewportHeight <= 0) {
            return Math.max(1.0, viewport.getGlyphCache().getLineHeight());
        }
        return viewportHeight;
    }

    private void deleteSelectionIfAny() {
        if (selectionModel.hasSelection()) {
            int start = selectionModel.getSelectionStartOffset(document);
            int end = selectionModel.getSelectionEndOffset(document);
            document.delete(start, end);
            moveCaretToOffset(start);
        }
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
        searchController.setOnNavigate(null);
        searchController.setOnClose(null);
        searchController.setOnSearchChanged(null);
        searchController.setSelectionRangeSupplier(null);
        searchController.setDocument(null);
        searchController.close();
        goToLineController.setOnGoToLine(null);
        goToLineController.setOnClose(null);
        goToLineController.close();
        searchRefreshDebounce.stop();
        document.removeChangeListener(searchRefreshListener);
        unbindThemeProperty();
        setOnKeyPressed(null);
        setOnKeyTyped(null);
        setOnMousePressed(null);
        setOnMouseDragged(null);
        setOnMouseReleased(null);
        setOnScroll(null);
        focusedProperty().removeListener(focusListener);
        viewport.setCaretBlinkActive(false);
        selectionModel.caretLineProperty().removeListener(caretLineListener);
        selectionModel.caretColumnProperty().removeListener(caretColumnListener);
        document.removeChangeListener(gutterWidthListener);
        markerModel.removeChangeListener(markerModelChangeListener);
        verticalScrollOffset.removeListener(scrollOffsetListener);
        languageId.removeListener(languageListener);
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
