package org.metalib.papifly.fx.code.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineIndexTest {

    @Test
    void lineAndColumnMappingWorksWithTrailingNewline() {
        String text = "ab\nc\n";
        LineIndex index = new LineIndex(text);

        assertEquals(3, index.getLineCount());

        assertEquals(0, index.getLineForOffset(0, text.length()));
        assertEquals(0, index.getLineForOffset(2, text.length()));
        assertEquals(1, index.getLineForOffset(3, text.length()));
        assertEquals(1, index.getLineForOffset(4, text.length()));
        assertEquals(2, index.getLineForOffset(5, text.length()));

        assertEquals(1, index.getColumnForOffset(4, text.length()));

        assertEquals(2, index.toOffset(0, 99, text.length()));
        assertEquals(5, index.toOffset(2, 99, text.length()));
    }

    @Test
    void lineEndOffsetExcludesNewline() {
        String text = "abc\ndef";
        LineIndex index = new LineIndex(text);

        assertEquals(3, index.getLineEndOffset(0, text.length()));
        assertEquals(7, index.getLineEndOffset(1, text.length()));
    }

    @Test
    void emptyTextHasOneLine() {
        LineIndex index = new LineIndex("");
        assertEquals(1, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(0, index.getLineEndOffset(0, 0));
    }

    @Test
    void singleLineNoNewline() {
        String text = "hello";
        LineIndex index = new LineIndex(text);
        assertEquals(1, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(5, index.getLineEndOffset(0, text.length()));
        assertEquals(0, index.getLineForOffset(0, text.length()));
        assertEquals(0, index.getLineForOffset(4, text.length()));
    }

    @Test
    void nullTextHasOneLine() {
        LineIndex index = new LineIndex(null);
        assertEquals(1, index.getLineCount());
    }

    @Test
    void invalidLineThrows() {
        LineIndex index = new LineIndex("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineStartOffset(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineStartOffset(1));
    }

    @Test
    void invalidOffsetThrows() {
        String text = "abc";
        LineIndex index = new LineIndex(text);
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineForOffset(-1, text.length()));
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineForOffset(4, text.length()));
    }

    @Test
    void offsetAtEndOfTextIsValid() {
        String text = "abc";
        LineIndex index = new LineIndex(text);
        assertEquals(0, index.getLineForOffset(3, text.length()));
        assertEquals(3, index.getColumnForOffset(3, text.length()));
    }

    @Test
    void rebuildUpdatesIndex() {
        LineIndex index = new LineIndex("a\nb");
        assertEquals(2, index.getLineCount());

        index.rebuild("a\nb\nc\nd");
        assertEquals(4, index.getLineCount());
    }

    @Test
    void columnClampingOnToOffset() {
        String text = "ab\ncde";
        LineIndex index = new LineIndex(text);
        // Line 0 has 2 chars, column 100 should clamp to offset 2
        assertEquals(2, index.toOffset(0, 100, text.length()));
        // Line 1 has 3 chars, column 100 should clamp to offset 6
        assertEquals(6, index.toOffset(1, 100, text.length()));
    }
}
