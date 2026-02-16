package org.metalib.papifly.fx.code.document;

/**
 * Mutable text storage abstraction backed by StringBuilder.
 * All text is normalized to use {@code '\n'} line endings on input.
 */
public class TextSource {

    private final StringBuilder buffer;

    /**
     * Creates an empty text source.
     */
    public TextSource() {
        this("");
    }

    /**
     * Creates a text source initialized with the provided text.
     */
    public TextSource(String text) {
        this.buffer = new StringBuilder(normalizeLineEndings(text));
    }

    /**
     * Returns current text length.
     */
    public int length() {
        return buffer.length();
    }

    /**
     * Returns true if text is empty.
     */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /**
     * Returns full text snapshot.
     */
    public String getText() {
        return buffer.toString();
    }

    /**
     * Returns a substring in [start, end).
     */
    public String substring(int start, int end) {
        requireRange(start, end, buffer.length());
        return buffer.substring(start, end);
    }

    /**
     * Replaces all text with the provided value.
     * Line endings are normalized to {@code '\n'}.
     */
    public void setText(String text) {
        buffer.setLength(0);
        if (text != null && !text.isEmpty()) {
            buffer.append(normalizeLineEndings(text));
        }
    }

    /**
     * Inserts text at the provided offset.
     * Line endings are normalized to {@code '\n'}.
     */
    public void insert(int offset, String text) {
        requireOffset(offset, buffer.length());
        if (text == null || text.isEmpty()) {
            return;
        }
        buffer.insert(offset, normalizeLineEndings(text));
    }

    /**
     * Deletes text in [start, end) and returns deleted text.
     */
    public String delete(int start, int end) {
        requireRange(start, end, buffer.length());
        if (start == end) {
            return "";
        }
        String deleted = buffer.substring(start, end);
        buffer.delete(start, end);
        return deleted;
    }

    /**
     * Replaces text in [start, end) and returns the replaced text.
     * The replacement text has line endings normalized to {@code '\n'}.
     */
    public String replace(int start, int end, String replacement) {
        requireRange(start, end, buffer.length());
        String replaced = buffer.substring(start, end);
        buffer.replace(start, end, normalizeLineEndings(replacement));
        return replaced;
    }

    /**
     * Normalizes {@code '\r\n'} and standalone {@code '\r'} to {@code '\n'}.
     */
    static String normalizeLineEndings(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.indexOf('\r') < 0) {
            return text;
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void requireOffset(int offset, int length) {
        if (offset < 0 || offset > length) {
            throw new IndexOutOfBoundsException("Offset out of range: " + offset + ", length: " + length);
        }
    }

    private static void requireRange(int start, int end, int length) {
        if (start < 0 || end < start || end > length) {
            throw new IndexOutOfBoundsException(
                "Range out of bounds: [" + start + ", " + end + "), length: " + length
            );
        }
    }
}
