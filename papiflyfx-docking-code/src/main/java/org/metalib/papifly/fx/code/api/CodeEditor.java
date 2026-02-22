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
import org.metalib.papifly.fx.code.search.SearchModel;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;
import org.metalib.papifly.fx.code.theme.CodeEditorThemeMapper;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    private enum WordDirection {
        LEFT,
        RIGHT
    }

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
    private final EditorCommandRegistry commandRegistry;
    private final EditorLifecycleService lifecycleService;
    private final OccurrenceSelectionService occurrenceSelectionService;
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
            (line, column) -> moveCaret(line, column, false),
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
        this.occurrenceSelectionService = new OccurrenceSelectionService(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            viewport::markDirty
        );
        this.editController = createEditController();
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
            this::handleKeyPressed,
            this::handleKeyTyped,
            this::handleMousePressed,
            this::handleMouseDragged,
            this::handleMouseReleased,
            this::handleScroll,
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
        commandRegistry.register(executor, buildCommandHandlers());
        return executor;
    }

    private Map<EditorCommand, Runnable> buildCommandHandlers() {
        Map<EditorCommand, Runnable> handlers = new EnumMap<>(EditorCommand.class);

        // Navigation
        handlers.put(EditorCommand.MOVE_LEFT, () -> handleLeft(false));
        handlers.put(EditorCommand.MOVE_RIGHT, () -> handleRight(false));
        handlers.put(EditorCommand.MOVE_UP, () -> handleUp(false));
        handlers.put(EditorCommand.MOVE_DOWN, () -> handleDown(false));
        handlers.put(EditorCommand.MOVE_PAGE_UP, () -> handlePageUp(false));
        handlers.put(EditorCommand.MOVE_PAGE_DOWN, () -> handlePageDown(false));
        handlers.put(EditorCommand.SELECT_LEFT, () -> handleLeft(true));
        handlers.put(EditorCommand.SELECT_RIGHT, () -> handleRight(true));
        handlers.put(EditorCommand.SELECT_UP, () -> handleUp(true));
        handlers.put(EditorCommand.SELECT_DOWN, () -> handleDown(true));
        handlers.put(EditorCommand.SELECT_PAGE_UP, () -> handlePageUp(true));
        handlers.put(EditorCommand.SELECT_PAGE_DOWN, () -> handlePageDown(true));
        handlers.put(EditorCommand.SCROLL_PAGE_UP, this::handleScrollPageUp);
        handlers.put(EditorCommand.SCROLL_PAGE_DOWN, this::handleScrollPageDown);
        handlers.put(EditorCommand.LINE_START, () -> handleHome(false));
        handlers.put(EditorCommand.LINE_END, () -> handleEnd(false));
        handlers.put(EditorCommand.SELECT_TO_LINE_START, () -> handleHome(true));
        handlers.put(EditorCommand.SELECT_TO_LINE_END, () -> handleEnd(true));

        // Editing
        handlers.put(EditorCommand.BACKSPACE, this::handleBackspace);
        handlers.put(EditorCommand.DELETE, this::handleDelete);
        handlers.put(EditorCommand.ENTER, this::handleEnter);

        // Clipboard and undo
        handlers.put(EditorCommand.SELECT_ALL, this::handleSelectAll);
        handlers.put(EditorCommand.UNDO, this::handleUndo);
        handlers.put(EditorCommand.REDO, this::handleRedo);
        handlers.put(EditorCommand.COPY, this::handleCopy);
        handlers.put(EditorCommand.CUT, this::handleCut);
        handlers.put(EditorCommand.PASTE, this::handlePaste);

        // Search
        handlers.put(EditorCommand.OPEN_SEARCH, this::openSearch);
        handlers.put(EditorCommand.OPEN_REPLACE, this::openReplace);
        handlers.put(EditorCommand.GO_TO_LINE, this::goToLine);

        // Word navigation
        handlers.put(EditorCommand.MOVE_WORD_LEFT, this::handleMoveWordLeft);
        handlers.put(EditorCommand.MOVE_WORD_RIGHT, this::handleMoveWordRight);
        handlers.put(EditorCommand.SELECT_WORD_LEFT, this::handleSelectWordLeft);
        handlers.put(EditorCommand.SELECT_WORD_RIGHT, this::handleSelectWordRight);
        handlers.put(EditorCommand.DELETE_WORD_LEFT, this::handleDeleteWordLeft);
        handlers.put(EditorCommand.DELETE_WORD_RIGHT, this::handleDeleteWordRight);

        // Document boundaries
        handlers.put(EditorCommand.DOCUMENT_START, () -> handleDocumentStart(false));
        handlers.put(EditorCommand.DOCUMENT_END, () -> handleDocumentEnd(false));
        handlers.put(EditorCommand.SELECT_TO_DOCUMENT_START, () -> handleDocumentStart(true));
        handlers.put(EditorCommand.SELECT_TO_DOCUMENT_END, () -> handleDocumentEnd(true));

        // Line operations
        handlers.put(EditorCommand.DELETE_LINE, this::handleDeleteLine);
        handlers.put(EditorCommand.MOVE_LINE_UP, this::handleMoveLineUp);
        handlers.put(EditorCommand.MOVE_LINE_DOWN, this::handleMoveLineDown);
        handlers.put(EditorCommand.DUPLICATE_LINE_UP, this::handleDuplicateLineUp);
        handlers.put(EditorCommand.DUPLICATE_LINE_DOWN, this::handleDuplicateLineDown);
        handlers.put(EditorCommand.JOIN_LINES, this::handleJoinLines);

        // Multi-caret
        handlers.put(EditorCommand.SELECT_NEXT_OCCURRENCE, this::handleSelectNextOccurrence);
        handlers.put(EditorCommand.SELECT_ALL_OCCURRENCES, this::handleSelectAllOccurrences);
        handlers.put(EditorCommand.ADD_CURSOR_UP, this::handleAddCursorUp);
        handlers.put(EditorCommand.ADD_CURSOR_DOWN, this::handleAddCursorDown);
        handlers.put(EditorCommand.UNDO_LAST_OCCURRENCE, this::handleUndoLastOccurrence);

        return handlers;
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
        moveWord(WordDirection.LEFT, false);
    }

    private void handleMoveWordRight() {
        moveWord(WordDirection.RIGHT, false);
    }

    private void handleSelectWordLeft() {
        moveWord(WordDirection.LEFT, true);
    }

    private void handleSelectWordRight() {
        moveWord(WordDirection.RIGHT, true);
    }

    private void handleDeleteWordLeft() {
        deleteWord(WordDirection.LEFT);
    }

    private void handleDeleteWordRight() {
        deleteWord(WordDirection.RIGHT);
    }

    private void moveWord(WordDirection direction, boolean extendSelection) {
        int line = selectionModel.getCaretLine();
        int column = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        int target = findWordBoundary(lineText, column, direction);
        if (target == column) {
            if (direction == WordDirection.LEFT && line > 0) {
                line--;
                target = document.getLineText(line).length();
            } else if (direction == WordDirection.RIGHT && line < document.getLineCount() - 1) {
                line++;
                target = 0;
            }
        }
        moveCaret(line, target, extendSelection);
    }

    private void deleteWord(WordDirection direction) {
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int line = selectionModel.getCaretLine();
        int column = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        int targetColumn = findWordBoundary(lineText, column, direction);
        if (targetColumn == column) {
            int caretOffset = selectionModel.getCaretOffset(document);
            if (direction == WordDirection.LEFT && line > 0) {
                document.delete(caretOffset - 1, caretOffset);
                moveCaretToOffset(caretOffset - 1);
            } else if (direction == WordDirection.RIGHT && line < document.getLineCount() - 1) {
                document.delete(caretOffset, caretOffset + 1);
            }
            return;
        }
        int lineStart = document.getLineStartOffset(line);
        if (direction == WordDirection.LEFT) {
            document.delete(lineStart + targetColumn, lineStart + column);
            moveCaret(line, targetColumn, false);
            return;
        }
        document.delete(lineStart + column, lineStart + targetColumn);
    }

    private int findWordBoundary(String lineText, int column, WordDirection direction) {
        return direction == WordDirection.LEFT
            ? WordBoundary.findWordLeft(lineText, column)
            : WordBoundary.findWordRight(lineText, column);
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
        occurrenceSelectionService.selectNextOccurrence();
    }

    private void handleSelectAllOccurrences() {
        occurrenceSelectionService.selectAllOccurrences();
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

    private void onGoToLineClosed() {
        requestFocus();
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
