package org.metalib.papifly.fx.code.api;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;
import org.metalib.papifly.fx.code.state.EditorStateData;

import java.util.List;

/**
 * Canvas-based code editor component.
 * <p>
 * Renders document text via a virtualized {@link Viewport} and handles
 * keyboard/mouse input for editing, caret movement, and selection.
 */
public class CodeEditor extends StackPane {

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
    private final ChangeListener<Number> caretLineListener = (obs, oldValue, newValue) ->
        cursorLine.set(newValue.intValue());
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) ->
        cursorColumn.set(newValue.intValue());
    private final ChangeListener<Number> scrollOffsetListener = (obs, oldValue, newValue) ->
        applyScrollOffset(newValue.doubleValue());

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

        setMinSize(0, 0);
        setPrefSize(640, 480);
        setFocusTraversable(true);
        getChildren().add(viewport);

        // Bind cursor properties to selection model
        selectionModel.caretLineProperty().addListener(caretLineListener);
        selectionModel.caretColumnProperty().addListener(caretColumnListener);

        // Bind vertical scroll offset
        verticalScrollOffset.addListener(scrollOffsetListener);

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

    // --- Key handling ---

    private void handleKeyTyped(KeyEvent event) {
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
        moveCaretRight(ch.length(), false);
        event.consume();
    }

    private void handleKeyPressed(KeyEvent event) {
        boolean shift = event.isShiftDown();
        boolean shortcut = event.isControlDown() || event.isMetaDown();
        KeyCode code = event.getCode();

        switch (code) {
            case BACK_SPACE -> { handleBackspace(); event.consume(); }
            case DELETE -> { handleDelete(); event.consume(); }
            case ENTER -> { handleEnter(); event.consume(); }
            case LEFT -> { handleLeft(shift); event.consume(); }
            case RIGHT -> { handleRight(shift); event.consume(); }
            case UP -> { handleUp(shift); event.consume(); }
            case DOWN -> { handleDown(shift); event.consume(); }
            case HOME -> { handleHome(shift); event.consume(); }
            case END -> { handleEnd(shift); event.consume(); }
            case A -> { if (shortcut) { handleSelectAll(); event.consume(); } }
            case Z -> {
                if (shortcut && shift) { handleRedo(); event.consume(); }
                else if (shortcut) { handleUndo(); event.consume(); }
            }
            case Y -> { if (shortcut) { handleRedo(); event.consume(); } }
            case C -> { if (shortcut) { handleCopy(); event.consume(); } }
            case X -> { if (shortcut) { handleCut(); event.consume(); } }
            case V -> { if (shortcut) { handlePaste(); event.consume(); } }
            default -> { /* no-op */ }
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
    }

    private void moveCaretRight(int chars, boolean extendSelection) {
        int offset = selectionModel.getCaretOffset(document);
        // offset already advanced by insert, so we need to recalculate
        int line = document.getLineForOffset(offset);
        int col = document.getColumnForOffset(offset);
        moveCaret(line, col, extendSelection);
    }

    private void moveCaretToOffset(int offset) {
        offset = Math.max(0, Math.min(offset, document.length()));
        int line = document.getLineForOffset(offset);
        int col = document.getColumnForOffset(offset);
        selectionModel.moveCaret(line, col);
        viewport.ensureCaretVisible();
        syncVerticalScrollOffsetFromViewport();
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
            applyScrollOffset(safeOffset);
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
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        setOnKeyPressed(null);
        setOnKeyTyped(null);
        setOnMousePressed(null);
        setOnMouseDragged(null);
        setOnScroll(null);
        selectionModel.caretLineProperty().removeListener(caretLineListener);
        selectionModel.caretColumnProperty().removeListener(caretColumnListener);
        verticalScrollOffset.removeListener(scrollOffsetListener);
        viewport.dispose();
    }
}
