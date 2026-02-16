package org.metalib.papifly.fx.code.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps text offsets to line/column and line/column back to offsets.
 */
public class LineIndex {

    private final List<Integer> lineStarts = new ArrayList<>();

    /**
     * Creates an index for empty text.
     */
    public LineIndex() {
        this("");
    }

    /**
     * Creates an index initialized with given text.
     */
    public LineIndex(CharSequence text) {
        rebuild(text);
    }

    /**
     * Rebuilds the line index from full text.
     */
    public void rebuild(CharSequence text) {
        lineStarts.clear();
        lineStarts.add(0);
        if (text == null) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineStarts.add(i + 1);
            }
        }
    }

    /**
     * Returns number of lines in the text.
     */
    public int getLineCount() {
        return lineStarts.size();
    }

    /**
     * Returns unmodifiable line start offsets.
     */
    public List<Integer> getLineStarts() {
        return Collections.unmodifiableList(lineStarts);
    }

    /**
     * Returns start offset for a line.
     */
    public int getLineStartOffset(int line) {
        requireLine(line);
        return lineStarts.get(line);
    }

    /**
     * Returns end offset (exclusive, without trailing newline) for a line.
     */
    public int getLineEndOffset(int line, int textLength) {
        requireLine(line);
        requireTextLength(textLength);

        if (line == lineStarts.size() - 1) {
            return textLength;
        }
        return lineStarts.get(line + 1) - 1;
    }

    /**
     * Returns line index for an offset.
     */
    public int getLineForOffset(int offset, int textLength) {
        requireOffset(offset, textLength);
        int position = Collections.binarySearch(lineStarts, offset);
        if (position >= 0) {
            return position;
        }
        int insertionPoint = -position - 1;
        return insertionPoint - 1;
    }

    /**
     * Returns column for offset.
     */
    public int getColumnForOffset(int offset, int textLength) {
        int line = getLineForOffset(offset, textLength);
        return offset - getLineStartOffset(line);
    }

    /**
     * Returns offset for line and column, clamping column to line bounds.
     */
    public int toOffset(int line, int column, int textLength) {
        requireLine(line);
        requireTextLength(textLength);

        int lineStart = getLineStartOffset(line);
        int lineEnd = getLineEndOffset(line, textLength);
        int lineLength = lineEnd - lineStart;
        int safeColumn = Math.max(0, Math.min(column, lineLength));
        return lineStart + safeColumn;
    }

    private void requireLine(int line) {
        if (line < 0 || line >= lineStarts.size()) {
            throw new IndexOutOfBoundsException(
                "Line out of range: " + line + ", lineCount: " + lineStarts.size()
            );
        }
    }

    private static void requireTextLength(int textLength) {
        if (textLength < 0) {
            throw new IllegalArgumentException("Text length cannot be negative: " + textLength);
        }
    }

    private static void requireOffset(int offset, int textLength) {
        if (offset < 0 || offset > textLength) {
            throw new IndexOutOfBoundsException(
                "Offset out of range: " + offset + ", textLength: " + textLength
            );
        }
    }
}
