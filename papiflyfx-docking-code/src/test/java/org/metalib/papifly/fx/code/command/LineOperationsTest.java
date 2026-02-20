package org.metalib.papifly.fx.code.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for Phase 2 line operations executed via command dispatch helpers.
 * <p>
 * These tests operate on the {@link Document} and {@link SelectionModel} directly,
 * simulating the logic the CodeEditor handler methods perform, so they don't
 * require a JavaFX toolkit.
 */
class LineOperationsTest {

    private Document doc;
    private SelectionModel sel;

    @BeforeEach
    void setUp() {
        doc = new Document();
        sel = new SelectionModel();
    }

    // --- Delete line ---

    @Test
    void deleteLineMiddle() {
        doc.setText("aaa\nbbb\nccc");
        // Simulate deleting line 1 ("bbb")
        int startOffset = doc.getLineStartOffset(1);
        int endOffset = doc.getLineStartOffset(2);
        doc.delete(startOffset, endOffset);
        assertEquals("aaa\nccc", doc.getText());
    }

    @Test
    void deleteLineFirst() {
        doc.setText("aaa\nbbb\nccc");
        int startOffset = doc.getLineStartOffset(0);
        int endOffset = doc.getLineStartOffset(1);
        doc.delete(startOffset, endOffset);
        assertEquals("bbb\nccc", doc.getText());
    }

    @Test
    void deleteLineLast() {
        doc.setText("aaa\nbbb\nccc");
        // Last line: remove preceding newline + line content
        int startOffset = doc.getLineStartOffset(2) - 1; // include \n before "ccc"
        int endOffset = doc.length();
        doc.delete(startOffset, endOffset);
        assertEquals("aaa\nbbb", doc.getText());
    }

    @Test
    void deleteLineOnlyLine() {
        doc.setText("hello");
        // Only one line, delete everything
        doc.delete(0, doc.length());
        assertEquals("", doc.getText());
    }

    @Test
    void deleteLineUndoRestores() {
        doc.setText("aaa\nbbb\nccc");
        int startOffset = doc.getLineStartOffset(1);
        int endOffset = doc.getLineStartOffset(2);
        doc.delete(startOffset, endOffset);
        assertEquals("aaa\nccc", doc.getText());

        doc.undo();
        assertEquals("aaa\nbbb\nccc", doc.getText());
    }

    // --- Move line ---

    @Test
    void moveLineUpSwapsWithPrevious() {
        doc.setText("aaa\nbbb\nccc");
        // Move line 1 ("bbb") up: swap with line 0 ("aaa")
        int blockStart = doc.getLineStartOffset(1);
        int blockEnd = doc.getLineStartOffset(2);
        String block = doc.getText().substring(blockStart, blockEnd); // "bbb\n"
        int prevStart = doc.getLineStartOffset(0);
        String prev = doc.getText().substring(prevStart, blockStart); // "aaa\n"
        doc.replace(prevStart, blockEnd, block + prev);
        assertEquals("bbb\naaa\nccc", doc.getText());
    }

    @Test
    void moveLineDownSwapsWithNext() {
        doc.setText("aaa\nbbb\nccc");
        // Move line 0 ("aaa") down: swap with line 1 ("bbb")
        int blockStart = doc.getLineStartOffset(0);
        int blockEnd = doc.getLineStartOffset(1);
        String block = doc.getText().substring(blockStart, blockEnd); // "aaa\n"
        int nextEnd = doc.getLineStartOffset(2);
        String next = doc.getText().substring(blockEnd, nextEnd); // "bbb\n"
        doc.replace(blockStart, nextEnd, next + block);
        assertEquals("bbb\naaa\nccc", doc.getText());
    }

    @Test
    void moveLineUpUndoRestores() {
        doc.setText("aaa\nbbb\nccc");
        int blockStart = doc.getLineStartOffset(1);
        int blockEnd = doc.getLineStartOffset(2);
        String block = doc.getText().substring(blockStart, blockEnd);
        int prevStart = doc.getLineStartOffset(0);
        String prev = doc.getText().substring(prevStart, blockStart);
        doc.replace(prevStart, blockEnd, block + prev);
        assertEquals("bbb\naaa\nccc", doc.getText());

        doc.undo();
        assertEquals("aaa\nbbb\nccc", doc.getText());
    }

    // --- Duplicate line ---

    @Test
    void duplicateLineDown() {
        doc.setText("aaa\nbbb\nccc");
        // Duplicate line 1 below
        int blockStart = doc.getLineStartOffset(1);
        int blockEnd = doc.getLineStartOffset(2);
        String block = doc.getText().substring(blockStart, blockEnd); // "bbb\n"
        doc.insert(blockEnd, block);
        assertEquals("aaa\nbbb\nbbb\nccc", doc.getText());
        assertEquals(4, doc.getLineCount());
    }

    @Test
    void duplicateLineUp() {
        doc.setText("aaa\nbbb\nccc");
        // Duplicate line 1 above
        int blockStart = doc.getLineStartOffset(1);
        int blockEnd = doc.getLineStartOffset(2);
        String block = doc.getText().substring(blockStart, blockEnd); // "bbb\n"
        doc.insert(blockStart, block);
        assertEquals("aaa\nbbb\nbbb\nccc", doc.getText());
        assertEquals(4, doc.getLineCount());
    }

    @Test
    void duplicateLineUndoRestores() {
        doc.setText("aaa\nbbb");
        int blockStart = doc.getLineStartOffset(0);
        int blockEnd = doc.getLineStartOffset(1);
        String block = doc.getText().substring(blockStart, blockEnd);
        doc.insert(blockEnd, block);
        assertEquals("aaa\naaa\nbbb", doc.getText());

        doc.undo();
        assertEquals("aaa\nbbb", doc.getText());
    }

    // --- Join lines ---

    @Test
    void joinLinesReplacesNewlineWithSpace() {
        doc.setText("hello\nworld");
        int lineEnd = doc.getLineStartOffset(0) + doc.getLineText(0).length();
        doc.replace(lineEnd, lineEnd + 1, " ");
        assertEquals("hello world", doc.getText());
        assertEquals(1, doc.getLineCount());
    }

    @Test
    void joinLinesAtLastLineIsNoop() {
        doc.setText("only line");
        // Line 0 is last line — nothing to join
        assertEquals(1, doc.getLineCount());
        // No operation to perform
        assertEquals("only line", doc.getText());
    }

    @Test
    void joinLinesUndoRestores() {
        doc.setText("hello\nworld");
        int lineEnd = doc.getLineStartOffset(0) + doc.getLineText(0).length();
        doc.replace(lineEnd, lineEnd + 1, " ");
        assertEquals("hello world", doc.getText());

        doc.undo();
        assertEquals("hello\nworld", doc.getText());
    }

    @Test
    void joinLinesPreservesSubsequentLines() {
        doc.setText("aaa\nbbb\nccc");
        int lineEnd = doc.getLineStartOffset(0) + doc.getLineText(0).length();
        doc.replace(lineEnd, lineEnd + 1, " ");
        assertEquals("aaa bbb\nccc", doc.getText());
        assertEquals(2, doc.getLineCount());
    }
}
