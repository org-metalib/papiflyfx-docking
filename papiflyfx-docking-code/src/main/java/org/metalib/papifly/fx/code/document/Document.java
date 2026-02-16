package org.metalib.papifly.fx.code.document;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Editable document model with line index, undo/redo support, and change notification.
 */
public class Document {

    private final TextSource textSource;
    private final LineIndex lineIndex;
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();
    private final List<DocumentChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates an empty document.
     */
    public Document() {
        this("");
    }

    /**
     * Creates a document initialized with text.
     */
    public Document(String initialText) {
        this.textSource = new TextSource(initialText);
        this.lineIndex = new LineIndex(textSource.getText());
    }

    /**
     * Returns a snapshot of lines.
     */
    public List<String> getLinesSnapshot() {
        int count = getLineCount();
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(getLineText(i));
        }
        return lines;
    }

    /**
     * Adds a change listener.
     */
    public void addChangeListener(DocumentChangeListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Removes a change listener.
     */
    public void removeChangeListener(DocumentChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns full document text.
     */
    public String getText() {
        return textSource.getText();
    }

    /**
     * Sets full document text and clears history.
     */
    public void setText(String text) {
        int oldLength = textSource.length();
        textSource.setText(text);
        rebuildIndex();
        clearHistory();
        fireChange(DocumentChangeEvent.setText(oldLength, textSource.length()));
    }

    /**
     * Returns text length.
     */
    public int length() {
        return textSource.length();
    }

    /**
     * Returns line count.
     */
    public int getLineCount() {
        return lineIndex.getLineCount();
    }

    /**
     * Returns line start offset.
     */
    public int getLineStartOffset(int line) {
        return lineIndex.getLineStartOffset(line);
    }

    /**
     * Returns line end offset (exclusive, without trailing newline).
     */
    public int getLineEndOffset(int line) {
        return lineIndex.getLineEndOffset(line, length());
    }

    /**
     * Returns line text without trailing newline.
     */
    public String getLineText(int line) {
        int start = getLineStartOffset(line);
        int end = getLineEndOffset(line);
        return textSource.substring(start, end);
    }

    /**
     * Returns line index for offset.
     */
    public int getLineForOffset(int offset) {
        return lineIndex.getLineForOffset(offset, length());
    }

    /**
     * Returns column for offset.
     */
    public int getColumnForOffset(int offset) {
        return lineIndex.getColumnForOffset(offset, length());
    }

    /**
     * Returns offset for line and column.
     */
    public int toOffset(int line, int column) {
        return lineIndex.toOffset(line, column, length());
    }

    /**
     * Inserts text at offset and records undo.
     */
    public void insert(int offset, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        applyAndRecord(new InsertEdit(offset, text),
            DocumentChangeEvent.insert(offset, text.length()));
    }

    /**
     * Deletes range [start, end) and records undo.
     */
    public void delete(int startOffset, int endOffset) {
        if (startOffset == endOffset) {
            return;
        }
        applyAndRecord(new DeleteEdit(startOffset, endOffset),
            DocumentChangeEvent.delete(startOffset, endOffset - startOffset));
    }

    /**
     * Replaces range [start, end) with replacement and records undo.
     */
    public void replace(int startOffset, int endOffset, String replacement) {
        String safeReplacement = replacement == null ? "" : replacement;
        if (startOffset == endOffset) {
            insert(startOffset, safeReplacement);
            return;
        }
        applyAndRecord(new ReplaceEdit(startOffset, endOffset, safeReplacement),
            DocumentChangeEvent.replace(startOffset, endOffset - startOffset, safeReplacement.length()));
    }

    /**
     * Returns true when undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Returns true when redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Undoes the last edit.
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        int lengthBefore = textSource.length();
        EditCommand command = undoStack.pop();
        command.undo(textSource);
        rebuildIndex();
        redoStack.push(command);
        fireChange(new DocumentChangeEvent(0, lengthBefore, textSource.length(),
            DocumentChangeEvent.ChangeType.UNDO));
        return true;
    }

    /**
     * Redoes the last undone edit.
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        int lengthBefore = textSource.length();
        EditCommand command = redoStack.pop();
        command.apply(textSource);
        rebuildIndex();
        undoStack.push(command);
        fireChange(new DocumentChangeEvent(0, lengthBefore, textSource.length(),
            DocumentChangeEvent.ChangeType.REDO));
        return true;
    }

    /**
     * Clears undo/redo history.
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private void applyAndRecord(EditCommand command, DocumentChangeEvent event) {
        command.apply(textSource);
        rebuildIndex();
        undoStack.push(command);
        redoStack.clear();
        fireChange(event);
    }

    private void rebuildIndex() {
        lineIndex.rebuild(textSource.getText());
    }

    private void fireChange(DocumentChangeEvent event) {
        for (DocumentChangeListener listener : listeners) {
            listener.documentChanged(event);
        }
    }
}
