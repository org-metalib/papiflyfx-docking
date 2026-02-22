package org.metalib.papifly.fx.code.command;

import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

/**
 * Immutable descriptor of a contiguous line block in the document.
 */
public record LineBlock(
    int startLine,
    int endLine,
    int startOffset,
    int endOffset,
    String text,
    boolean reachesDocumentEnd
) {

    /**
     * Resolves a block from the current selection or caret line.
     */
    public static LineBlock fromSelectionOrCaret(Document document, SelectionModel selectionModel) {
        int startLine;
        int endLine;
        if (selectionModel.hasSelection()) {
            startLine = selectionModel.getSelectionStartLine();
            endLine = selectionModel.getSelectionEndLine();
        } else {
            startLine = selectionModel.getCaretLine();
            endLine = startLine;
        }
        return fromLines(document, startLine, endLine);
    }

    /**
     * Resolves a block for the inclusive line range.
     */
    public static LineBlock fromLines(Document document, int startLine, int endLine) {
        int safeStartLine = Math.max(0, Math.min(startLine, document.getLineCount() - 1));
        int safeEndLine = Math.max(safeStartLine, Math.min(endLine, document.getLineCount() - 1));
        int startOffset = document.getLineStartOffset(safeStartLine);
        int endOffset = safeEndLine < document.getLineCount() - 1
            ? document.getLineStartOffset(safeEndLine + 1)
            : document.length();
        String text = document.getSubstring(startOffset, endOffset);
        return new LineBlock(safeStartLine, safeEndLine, startOffset, endOffset, text, endOffset == document.length());
    }

    /**
     * Returns number of lines in the block.
     */
    public int lineCount() {
        return endLine - startLine + 1;
    }
}
