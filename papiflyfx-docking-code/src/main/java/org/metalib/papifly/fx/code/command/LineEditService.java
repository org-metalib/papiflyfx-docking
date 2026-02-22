package org.metalib.papifly.fx.code.command;

import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

/**
 * Shared line-level edit operations used by editor commands.
 */
public class LineEditService {

    /**
     * Resolves selected line block or current caret line block.
     */
    public LineBlock resolveSelectionOrCaretBlock(Document document, SelectionModel selectionModel) {
        return LineBlock.fromSelectionOrCaret(document, selectionModel);
    }

    /**
     * Deletes the provided block.
     */
    public boolean deleteBlock(Document document, LineBlock block) {
        int startOffset = block.startOffset();
        int endOffset = block.endOffset();
        if (block.endLine() >= document.getLineCount() - 1) {
            endOffset = document.length();
            if (block.startLine() > 0) {
                startOffset = document.getLineStartOffset(block.startLine()) - 1;
            }
        }
        if (startOffset >= endOffset) {
            return false;
        }
        document.delete(startOffset, endOffset);
        return true;
    }

    /**
     * Moves the provided block one line up.
     */
    public boolean moveBlockUp(Document document, LineBlock block) {
        if (block.startLine() <= 0) {
            return false;
        }
        LineBlock previousLine = LineBlock.fromLines(document, block.startLine() - 1, block.startLine() - 1);
        String combined = ensureTrailingNewline(block.text()) + ensureTrailingNewline(previousLine.text());
        if (block.reachesDocumentEnd() && !document.getText().endsWith("\n")) {
            combined = stripSingleTrailingNewline(combined);
        }
        document.replace(previousLine.startOffset(), block.endOffset(), combined);
        return true;
    }

    /**
     * Moves the provided block one line down.
     */
    public boolean moveBlockDown(Document document, LineBlock block) {
        if (block.endLine() >= document.getLineCount() - 1) {
            return false;
        }
        LineBlock nextLine = LineBlock.fromLines(document, block.endLine() + 1, block.endLine() + 1);
        String combined = ensureTrailingNewline(nextLine.text()) + ensureTrailingNewline(block.text());
        if (nextLine.reachesDocumentEnd() && !document.getText().endsWith("\n")) {
            combined = stripSingleTrailingNewline(combined);
        }
        document.replace(block.startOffset(), nextLine.endOffset(), combined);
        return true;
    }

    /**
     * Duplicates the block above itself.
     */
    public void duplicateBlockUp(Document document, LineBlock block) {
        document.insert(block.startOffset(), ensureTrailingNewline(block.text()));
    }

    /**
     * Duplicates the block below itself.
     */
    public void duplicateBlockDown(Document document, LineBlock block) {
        if (block.reachesDocumentEnd() && !block.text().endsWith("\n")) {
            document.insert(block.endOffset(), "\n" + block.text());
            return;
        }
        document.insert(block.endOffset(), ensureTrailingNewline(block.text()));
    }

    /**
     * Joins the provided line with the next line using a single space.
     */
    public boolean joinLineWithNext(Document document, int line) {
        if (line >= document.getLineCount() - 1) {
            return false;
        }
        int lineEnd = document.getLineStartOffset(line) + document.getLineText(line).length();
        document.replace(lineEnd, lineEnd + 1, " ");
        return true;
    }

    private static String ensureTrailingNewline(String value) {
        if (value.endsWith("\n")) {
            return value;
        }
        return value + '\n';
    }

    private static String stripSingleTrailingNewline(String value) {
        if (value.endsWith("\n")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
