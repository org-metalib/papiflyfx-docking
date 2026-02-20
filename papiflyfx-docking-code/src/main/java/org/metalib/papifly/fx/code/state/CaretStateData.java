package org.metalib.papifly.fx.code.state;

/**
 * Serializable caret snapshot with optional selection.
 * <p>
 * All line/column values are zero-based and normalized to non-negative values.
 */
public record CaretStateData(
    int anchorLine,
    int anchorColumn,
    int caretLine,
    int caretColumn
) {
    public CaretStateData {
        anchorLine = Math.max(0, anchorLine);
        anchorColumn = Math.max(0, anchorColumn);
        caretLine = Math.max(0, caretLine);
        caretColumn = Math.max(0, caretColumn);
    }
}
