package org.metalib.papifly.fx.code.command;

import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

/**
 * Immutable snapshot of a caret position with optional selection range.
 * <p>
 * All line/column values are zero-based. When anchor equals caret there
 * is no selection. Start/end helpers return positions ordered by document
 * offset (start &le; end).
 */
public record CaretRange(int anchorLine, int anchorColumn, int caretLine, int caretColumn) {

    /**
     * Returns {@code true} if this range has a non-empty selection.
     */
    public boolean hasSelection() {
        return anchorLine != caretLine || anchorColumn != caretColumn;
    }

    /**
     * Returns the start line (the earlier position in the document).
     */
    public int getStartLine() {
        if (anchorLine < caretLine) return anchorLine;
        if (anchorLine > caretLine) return caretLine;
        return anchorColumn <= caretColumn ? anchorLine : caretLine;
    }

    /**
     * Returns the start column (the earlier position in the document).
     */
    public int getStartColumn() {
        if (anchorLine < caretLine) return anchorColumn;
        if (anchorLine > caretLine) return caretColumn;
        return Math.min(anchorColumn, caretColumn);
    }

    /**
     * Returns the end line (the later position in the document).
     */
    public int getEndLine() {
        if (anchorLine > caretLine) return anchorLine;
        if (anchorLine < caretLine) return caretLine;
        return anchorColumn >= caretColumn ? anchorLine : caretLine;
    }

    /**
     * Returns the end column (the later position in the document).
     */
    public int getEndColumn() {
        if (anchorLine > caretLine) return anchorColumn;
        if (anchorLine < caretLine) return caretColumn;
        return Math.max(anchorColumn, caretColumn);
    }

    /**
     * Returns the document offset of the selection start.
     */
    public int getStartOffset(Document document) {
        return document.toOffset(getStartLine(), getStartColumn());
    }

    /**
     * Returns the document offset of the selection end.
     */
    public int getEndOffset(Document document) {
        return document.toOffset(getEndLine(), getEndColumn());
    }

    /**
     * Returns the document offset of the caret position.
     */
    public int getCaretOffset(Document document) {
        return document.toOffset(caretLine, caretColumn);
    }

    /**
     * Captures the current state of a {@link SelectionModel} as a {@code CaretRange}.
     */
    public static CaretRange fromSelectionModel(SelectionModel selectionModel) {
        return new CaretRange(
            selectionModel.getAnchorLine(),
            selectionModel.getAnchorColumn(),
            selectionModel.getCaretLine(),
            selectionModel.getCaretColumn()
        );
    }
}
