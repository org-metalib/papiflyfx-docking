package org.metalib.papifly.fx.code.api;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.KeymapTable;
import org.metalib.papifly.fx.code.command.WordBoundary;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.gutter.GutterView;
import org.metalib.papifly.fx.code.gutter.MarkerModel;
import org.metalib.papifly.fx.code.lexer.IncrementalLexerPipeline;
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
import java.util.Optional;

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

    private final StringProperty filePath = new SimpleStringProperty(this, "filePath", "");
    private final IntegerProperty cursorLine = new SimpleIntegerProperty(this, "cursorLine", 0);
    private final IntegerProperty cursorColumn = new SimpleIntegerProperty(this, "cursorColumn", 0);
    private final DoubleProperty verticalScrollOffset = new SimpleDoubleProperty(this, "verticalScrollOffset", 0.0);
    private final StringProperty languageId = new SimpleStringProperty(this, "languageId", DEFAULT_LANGUAGE);

    private List<Integer> foldedLines = List.of();

    private final Document document;
    private final Viewport viewport;
    private final SelectionModel selectionModel;
    private final GutterView gutterView;
    private final MarkerModel markerModel;
    private final SearchModel searchModel;
    private final SearchController searchController;

    private final ChangeListener<Number> caretLineListener;
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) ->
        cursorColumn.set(newValue.intValue());
    private final ChangeListener<Number> scrollOffsetListener = (obs, oldValue, newValue) ->
        applyScrollOffset(newValue.doubleValue());
    private final ChangeListener<String> languageListener;
    private final DocumentChangeListener gutterWidthListener;
    private final MarkerModel.MarkerChangeListener markerModelChangeListener;
    private final IncrementalLexerPipeline lexerPipeline;

    private ObjectProperty<Theme> boundThemeProperty;
    private ChangeListener<Theme> themeChangeListener;
    private int gutterDigits;
    private boolean syncingScrollOffset;
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
        this.document = document;
        this.selectionModel = new SelectionModel();
        this.viewport = new Viewport(selectionModel);
        this.viewport.setDocument(document);

        // Gutter
        this.markerModel = new MarkerModel();
        this.gutterView = new GutterView(viewport.getGlyphCache());
        this.gutterView.setDocument(document);
        this.gutterView.setMarkerModel(markerModel);
        this.gutterDigits = computeGutterDigits(document.getLineCount());
        this.gutterWidthListener = event -> refreshGutterWidthIfNeeded();
        this.document.addChangeListener(gutterWidthListener);
        this.markerModelChangeListener = gutterView::markDirty;
        this.markerModel.addChangeListener(markerModelChangeListener);
        this.caretLineListener = (obs, oldValue, newValue) -> {
            cursorLine.set(newValue.intValue());
            gutterView.setActiveLineIndex(newValue.intValue());
        };

        // Search
        this.searchModel = new SearchModel();
        this.searchController = new SearchController(searchModel);
        this.searchController.setDocument(document);
        this.searchController.setOnNavigate(this::navigateToSearchMatch);
        this.searchController.setOnClose(this::onSearchClosed);
        this.searchController.setOnSearchChanged(this::onSearchResultsChanged);

        // Layout: gutter left, viewport center, search overlay on top
        BorderPane editorArea = new BorderPane();
        editorArea.setLeft(gutterView);
        editorArea.setCenter(viewport);

        setMinSize(0, 0);
        setPrefSize(640, 480);
        setFocusTraversable(true);
        getChildren().add(editorArea);

        // Search overlay anchored to top
        StackPane.setAlignment(searchController, Pos.TOP_CENTER);
        getChildren().add(searchController);

        // Bind cursor properties to selection model
        selectionModel.caretLineProperty().addListener(caretLineListener);
        selectionModel.caretColumnProperty().addListener(caretColumnListener);

        // Bind vertical scroll offset
        verticalScrollOffset.addListener(scrollOffsetListener);

        this.lexerPipeline = new IncrementalLexerPipeline(document, viewport::setTokenMap);
        this.languageListener = (obs, oldValue, newValue) -> lexerPipeline.setLanguageId(newValue);
        languageId.addListener(languageListener);
        lexerPipeline.setLanguageId(languageId.get());

        // Input handlers
        setOnKeyPressed(this::handleKeyPressed);
        setOnKeyTyped(this::handleKeyTyped);
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnScroll(this::handleScroll);
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
        String selectedText = selectionModel.hasSelection()
            ? selectionModel.getSelectedText(document)
            : null;
        searchController.open(selectedText);
    }

    /**
     * Opens a go-to-line dialog. Shortcut: Ctrl/Cmd+G.
     */
    public void goToLine() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Go to Line");
        dialog.setHeaderText(null);
        dialog.setContentText("Line number (1-" + document.getLineCount() + "):");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                int lineNumber = Integer.parseInt(input.trim());
                goToLine(lineNumber);
            } catch (NumberFormatException ignored) {
                // Invalid input, do nothing
            }
        });
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
        if (searchController.isOpen() && searchController.isFocusWithin()) {
            return; // Let search field handle typed input
        }
        String ch = event.getCharacter();
        if (ch.isEmpty() || ch.charAt(0) < 32 || ch.charAt(0) == 127) {
            return; // control characters handled by keyPressed
        }
        if (event.isControlDown() || event.isMetaDown()) {
            return;
        }
        deleteSelectionIfAny();
        int offset = selectionModel.getCaretOffset(document);
        document.insert(offset, ch);
        moveCaretToOffset(offset + ch.length());
        event.consume();
    }

    private void handleKeyPressed(KeyEvent event) {
        // Escape always closes search
        if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && searchController.isOpen()) {
            searchController.close();
            requestFocus();
            event.consume();
            return;
        }

        Optional<EditorCommand> resolved = KeymapTable.resolve(event);
        if (resolved.isEmpty()) {
            return;
        }
        EditorCommand cmd = resolved.get();

        // "Always-on" commands execute even when search is focused
        if (cmd == EditorCommand.OPEN_SEARCH || cmd == EditorCommand.GO_TO_LINE) {
            executeCommand(cmd);
            event.consume();
            return;
        }

        // If search is focused, don't process editing keys
        if (searchController.isOpen() && searchController.isFocusWithin()) {
            return;
        }

        executeCommand(cmd);
        event.consume();
    }

    /**
     * Dispatches an {@link EditorCommand} to the appropriate handler method.
     */
    void executeCommand(EditorCommand cmd) {
        switch (cmd) {
            // Navigation
            case MOVE_LEFT -> handleLeft(false);
            case MOVE_RIGHT -> handleRight(false);
            case MOVE_UP -> handleUp(false);
            case MOVE_DOWN -> handleDown(false);
            case SELECT_LEFT -> handleLeft(true);
            case SELECT_RIGHT -> handleRight(true);
            case SELECT_UP -> handleUp(true);
            case SELECT_DOWN -> handleDown(true);
            case LINE_START -> handleHome(false);
            case LINE_END -> handleEnd(false);
            case SELECT_TO_LINE_START -> handleHome(true);
            case SELECT_TO_LINE_END -> handleEnd(true);

            // Editing
            case BACKSPACE -> handleBackspace();
            case DELETE -> handleDelete();
            case ENTER -> handleEnter();

            // Clipboard & undo
            case SELECT_ALL -> handleSelectAll();
            case UNDO -> handleUndo();
            case REDO -> handleRedo();
            case COPY -> handleCopy();
            case CUT -> handleCut();
            case PASTE -> handlePaste();

            // Search
            case OPEN_SEARCH -> openSearch();
            case GO_TO_LINE -> goToLine();

            // Phase 1 — word navigation
            case MOVE_WORD_LEFT -> handleMoveWordLeft();
            case MOVE_WORD_RIGHT -> handleMoveWordRight();
            case SELECT_WORD_LEFT -> handleSelectWordLeft();
            case SELECT_WORD_RIGHT -> handleSelectWordRight();
            case DELETE_WORD_LEFT -> handleDeleteWordLeft();
            case DELETE_WORD_RIGHT -> handleDeleteWordRight();

            // Phase 1 — document boundaries
            case DOCUMENT_START -> handleDocumentStart(false);
            case DOCUMENT_END -> handleDocumentEnd(false);
            case SELECT_TO_DOCUMENT_START -> handleDocumentStart(true);
            case SELECT_TO_DOCUMENT_END -> handleDocumentEnd(true);

            // Phase 2 — line operations
            case DELETE_LINE -> handleDeleteLine();
            case MOVE_LINE_UP -> handleMoveLineUp();
            case MOVE_LINE_DOWN -> handleMoveLineDown();
            case DUPLICATE_LINE_UP -> handleDuplicateLineUp();
            case DUPLICATE_LINE_DOWN -> handleDuplicateLineDown();
            case JOIN_LINES -> handleJoinLines();
        }
    }

    private void handleBackspace() {
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int offset = selectionModel.getCaretOffset(document);
        if (offset > 0) {
            document.delete(offset - 1, offset);
            moveCaretToOffset(offset - 1);
        }
    }

    private void handleDelete() {
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int offset = selectionModel.getCaretOffset(document);
        if (offset < document.length()) {
            document.delete(offset, offset + 1);
        }
    }

    private void handleEnter() {
        deleteSelectionIfAny();
        int offset = selectionModel.getCaretOffset(document);
        document.insert(offset, "\n");
        int newLine = selectionModel.getCaretLine() + 1;
        moveCaret(newLine, 0, false);
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
        int col = selectionModel.getCaretColumn();
        if (line > 0) {
            int newCol = Math.min(col, document.getLineText(line - 1).length());
            moveCaret(line - 1, newCol, shift);
        }
    }

    private void handleDown(boolean shift) {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        if (line < document.getLineCount() - 1) {
            int newCol = Math.min(col, document.getLineText(line + 1).length());
            moveCaret(line + 1, newCol, shift);
        }
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
        if (selectionModel.hasSelection()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selectionModel.getSelectedText(document));
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void handleCut() {
        handleCopy();
        deleteSelectionIfAny();
    }

    private void handlePaste() {
        String text = Clipboard.getSystemClipboard().getString();
        if (text != null && !text.isEmpty()) {
            deleteSelectionIfAny();
            int offset = selectionModel.getCaretOffset(document);
            document.insert(offset, text);
            // Move caret to end of pasted text
            int newOffset = offset + text.length();
            moveCaretToOffset(newOffset);
        }
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
        int startLine;
        int endLine;
        if (selectionModel.hasSelection()) {
            startLine = selectionModel.getSelectionStartLine();
            endLine = selectionModel.getSelectionEndLine();
        } else {
            startLine = selectionModel.getCaretLine();
            endLine = startLine;
        }

        int startOffset = document.getLineStartOffset(startLine);
        int endOffset;
        if (endLine < document.getLineCount() - 1) {
            endOffset = document.getLineStartOffset(endLine + 1);
        } else {
            // Last line — also remove preceding newline if possible
            endOffset = document.length();
            if (startLine > 0) {
                startOffset = document.getLineStartOffset(startLine) - 1;
            }
        }

        if (startOffset < endOffset) {
            document.delete(startOffset, endOffset);
            int targetLine = Math.min(startLine, document.getLineCount() - 1);
            moveCaret(targetLine, 0, false);
        }
    }

    private void handleMoveLineUp() {
        int startLine;
        int endLine;
        if (selectionModel.hasSelection()) {
            startLine = selectionModel.getSelectionStartLine();
            endLine = selectionModel.getSelectionEndLine();
        } else {
            startLine = selectionModel.getCaretLine();
            endLine = startLine;
        }

        if (startLine <= 0) {
            return; // Already at top
        }

        // Swap block [startLine..endLine] with [startLine-1]
        int blockStart = document.getLineStartOffset(startLine);
        int blockEnd = endLine < document.getLineCount() - 1
            ? document.getLineStartOffset(endLine + 1)
            : document.length();
        String blockText = document.getText().substring(blockStart, blockEnd);

        int prevLineStart = document.getLineStartOffset(startLine - 1);
        String prevLineText = document.getText().substring(prevLineStart, blockStart);

        // Ensure both blocks end with newline for clean swap
        String normalizedBlock = blockText.endsWith("\n") ? blockText : blockText + "\n";
        String normalizedPrev = prevLineText.endsWith("\n") ? prevLineText : prevLineText + "\n";

        // Replace the whole range [prevLineStart..blockEnd]
        String combined = normalizedBlock + normalizedPrev;
        // Trim trailing newline if we're at document end and original didn't have one
        if (blockEnd == document.length() && !document.getText().endsWith("\n")) {
            combined = combined.substring(0, combined.length() - 1);
        }
        document.replace(prevLineStart, blockEnd, combined);

        int col = selectionModel.getCaretColumn();
        moveCaret(selectionModel.getCaretLine() - 1, col, false);
    }

    private void handleMoveLineDown() {
        int startLine;
        int endLine;
        if (selectionModel.hasSelection()) {
            startLine = selectionModel.getSelectionStartLine();
            endLine = selectionModel.getSelectionEndLine();
        } else {
            startLine = selectionModel.getCaretLine();
            endLine = startLine;
        }

        if (endLine >= document.getLineCount() - 1) {
            return; // Already at bottom
        }

        int blockStart = document.getLineStartOffset(startLine);
        int blockEnd = document.getLineStartOffset(endLine + 1);
        String blockText = document.getText().substring(blockStart, blockEnd);

        int nextLineEnd = endLine + 1 < document.getLineCount() - 1
            ? document.getLineStartOffset(endLine + 2)
            : document.length();
        String nextLineText = document.getText().substring(blockEnd, nextLineEnd);

        String normalizedNext = nextLineText.endsWith("\n") ? nextLineText : nextLineText + "\n";
        String normalizedBlock = blockText.endsWith("\n") ? blockText : blockText + "\n";

        String combined = normalizedNext + normalizedBlock;
        if (nextLineEnd == document.length() && !document.getText().endsWith("\n")) {
            combined = combined.substring(0, combined.length() - 1);
        }
        document.replace(blockStart, nextLineEnd, combined);

        int col = selectionModel.getCaretColumn();
        moveCaret(selectionModel.getCaretLine() + 1, col, false);
    }

    private void handleDuplicateLineUp() {
        int startLine;
        int endLine;
        if (selectionModel.hasSelection()) {
            startLine = selectionModel.getSelectionStartLine();
            endLine = selectionModel.getSelectionEndLine();
        } else {
            startLine = selectionModel.getCaretLine();
            endLine = startLine;
        }

        int blockStart = document.getLineStartOffset(startLine);
        int blockEnd = endLine < document.getLineCount() - 1
            ? document.getLineStartOffset(endLine + 1)
            : document.length();
        String blockText = document.getText().substring(blockStart, blockEnd);

        // Insert a copy above: the copy gets the newline, original stays
        String insertion = blockText.endsWith("\n") ? blockText : blockText + "\n";
        document.insert(blockStart, insertion);

        // Caret stays on the original line (which shifted down)
        // so caret position is unchanged
    }

    private void handleDuplicateLineDown() {
        int startLine;
        int endLine;
        if (selectionModel.hasSelection()) {
            startLine = selectionModel.getSelectionStartLine();
            endLine = selectionModel.getSelectionEndLine();
        } else {
            startLine = selectionModel.getCaretLine();
            endLine = startLine;
        }

        int blockStart = document.getLineStartOffset(startLine);
        int blockEnd = endLine < document.getLineCount() - 1
            ? document.getLineStartOffset(endLine + 1)
            : document.length();
        String blockText = document.getText().substring(blockStart, blockEnd);

        // Insert a copy below
        String insertion = blockText.startsWith("\n") ? blockText : "\n" + blockText;
        if (blockEnd == document.length() && !blockText.startsWith("\n")) {
            // At end of document, prepend newline
            document.insert(blockEnd, "\n" + blockText);
        } else {
            // Insert the block text right after the block
            String toInsert = blockText.endsWith("\n") ? blockText : blockText + "\n";
            document.insert(blockEnd, toInsert);
        }

        // Move caret down to the duplicated line
        int linesInserted = endLine - startLine + 1;
        int col = selectionModel.getCaretColumn();
        moveCaret(selectionModel.getCaretLine() + linesInserted, col, false);
    }

    private void handleJoinLines() {
        int line = selectionModel.getCaretLine();
        if (line >= document.getLineCount() - 1) {
            return; // Last line, nothing to join
        }
        // Replace the newline at end of current line with a single space
        int lineEnd = document.getLineStartOffset(line) + document.getLineText(line).length();
        // The newline character is at offset lineEnd
        document.replace(lineEnd, lineEnd + 1, " ");
    }

    // --- Mouse handling ---

    private void handleMousePressed(MouseEvent event) {
        requestFocus();
        int line = viewport.getLineAtY(event.getY());
        int col = viewport.getColumnAtX(event.getX());
        if (line >= 0) {
            col = Math.min(col, document.getLineText(line).length());
            if (event.isShiftDown()) {
                selectionModel.moveCaretWithSelection(line, col);
            } else {
                selectionModel.moveCaret(line, col);
            }
            viewport.markDirty();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        int line = viewport.getLineAtY(event.getY());
        int col = viewport.getColumnAtX(event.getX());
        if (line >= 0) {
            col = Math.min(col, document.getLineText(line).length());
            selectionModel.moveCaretWithSelection(line, col);
            viewport.markDirty();
        }
    }

    private void handleScroll(ScrollEvent event) {
        double delta = -event.getDeltaY() * SCROLL_LINE_FACTOR;
        double newOffset = viewport.getScrollOffset() + delta;
        setVerticalScrollOffset(newOffset);
        event.consume();
    }

    // --- Search navigation ---

    private void navigateToSearchMatch(SearchMatch match) {
        moveCaret(match.line(), match.startColumn(), false);
        onSearchResultsChanged();
    }

    private void onSearchClosed() {
        viewport.setSearchMatches(List.of(), -1);
        requestFocus();
    }

    private void onSearchResultsChanged() {
        viewport.setSearchMatches(searchModel.getMatches(), searchModel.getCurrentMatchIndex());
    }

    // --- Helpers ---

    private void moveCaret(int line, int col, boolean extendSelection) {
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
        offset = Math.max(0, Math.min(offset, document.length()));
        int line = document.getLineForOffset(offset);
        int col = document.getColumnForOffset(offset);
        selectionModel.moveCaret(line, col);
        viewport.ensureCaretVisible();
        syncVerticalScrollOffsetFromViewport();
        syncGutterScroll();
    }

    private void applyCaretState(int line, int column) {
        int safeLine = clampLine(line);
        int safeColumn = clampColumn(safeLine, column);
        selectionModel.moveCaret(safeLine, safeColumn);
        viewport.markDirty();
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
        return new EditorStateData(
            filePath.get(),
            cursorLine.get(),
            cursorColumn.get(),
            viewport.getScrollOffset(),
            languageId.get(),
            foldedLines
        );
    }

    /**
     * Applies state to the editor.
     */
    public void applyState(EditorStateData state) {
        if (state == null) {
            return;
        }
        setFilePath(state.filePath());
        setLanguageId(state.languageId());
        setFoldedLines(state.foldedLines());
        applyCaretState(state.cursorLine(), state.cursorColumn());
        setVerticalScrollOffset(state.verticalScrollOffset());
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
        applyCaretState(cursorLine, selectionModel.getCaretColumn());
    }

    public IntegerProperty cursorLineProperty() {
        return cursorLine;
    }

    public int getCursorColumn() {
        return cursorColumn.get();
    }

    public void setCursorColumn(int cursorColumn) {
        applyCaretState(selectionModel.getCaretLine(), cursorColumn);
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
        searchController.setOnNavigate(null);
        searchController.setOnClose(null);
        searchController.setOnSearchChanged(null);
        searchController.setDocument(null);
        searchController.close();
        unbindThemeProperty();
        setOnKeyPressed(null);
        setOnKeyTyped(null);
        setOnMousePressed(null);
        setOnMouseDragged(null);
        setOnScroll(null);
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
