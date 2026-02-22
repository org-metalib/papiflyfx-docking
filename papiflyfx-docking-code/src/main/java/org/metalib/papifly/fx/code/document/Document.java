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

    private static final System.Logger LOGGER = System.getLogger(Document.class.getName());

    private final TextSource textSource;
    private final LineIndex lineIndex;
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();
    private final List<DocumentChangeListener> listeners = new CopyOnWriteArrayList<>();
    private List<EditCommand> compoundBuffer;

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
     * Returns substring in the range [startOffset, endOffset).
     */
    public String getSubstring(int startOffset, int endOffset) {
        return textSource.substring(startOffset, endOffset);
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
        String normalized = TextSource.normalizeLineEndings(text);
        EditCommand command = new InsertEdit(offset, normalized);
        command.apply(textSource);
        lineIndex.applyInsert(offset, normalized);
        recordEdit(command);
        fireChange(DocumentChangeEvent.insert(offset, normalized.length()));
    }

    /**
     * Deletes range [start, end) and records undo.
     */
    public void delete(int startOffset, int endOffset) {
        if (startOffset == endOffset) {
            return;
        }
        EditCommand command = new DeleteEdit(startOffset, endOffset);
        command.apply(textSource);
        lineIndex.applyDelete(startOffset, endOffset);
        recordEdit(command);
        fireChange(DocumentChangeEvent.delete(startOffset, endOffset - startOffset));
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
        String normalized = TextSource.normalizeLineEndings(safeReplacement);
        EditCommand command = new ReplaceEdit(startOffset, endOffset, normalized);
        command.apply(textSource);
        lineIndex.applyDelete(startOffset, endOffset);
        if (!normalized.isEmpty()) {
            lineIndex.applyInsert(startOffset, normalized);
        }
        recordEdit(command);
        fireChange(DocumentChangeEvent.replace(startOffset, endOffset - startOffset, normalized.length()));
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
     * Begins accumulating edits into a compound group.
     * <p>
     * While a compound edit is active, individual edits are applied
     * immediately but not pushed onto the undo stack. Call
     * {@link #endCompoundEdit()} to wrap them into a single undo entry.
     */
    public void beginCompoundEdit() {
        compoundBuffer = new ArrayList<>();
    }

    /**
     * Ends the current compound edit session and pushes the accumulated
     * edits as a single {@link CompoundEdit} onto the undo stack.
     */
    public void endCompoundEdit() {
        if (compoundBuffer != null && !compoundBuffer.isEmpty()) {
            undoStack.push(new CompoundEdit(List.copyOf(compoundBuffer)));
            redoStack.clear();
        }
        compoundBuffer = null;
    }

    /**
     * Returns {@code true} if a compound edit session is currently active.
     */
    public boolean isCompoundEditActive() {
        return compoundBuffer != null;
    }

    /**
     * Clears undo/redo history.
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private void recordEdit(EditCommand command) {
        if (compoundBuffer != null) {
            compoundBuffer.add(command);
        } else {
            undoStack.push(command);
            redoStack.clear();
        }
    }

    private void rebuildIndex() {
        lineIndex.rebuild(textSource.getText());
    }

    private void fireChange(DocumentChangeEvent event) {
        for (DocumentChangeListener listener : listeners) {
            try {
                listener.documentChanged(event);
            } catch (RuntimeException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Document change listener failed", exception);
            }
        }
    }
}
