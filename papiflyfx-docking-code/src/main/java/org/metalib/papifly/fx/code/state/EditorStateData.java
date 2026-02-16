package org.metalib.papifly.fx.code.state;

import java.util.List;

/**
 * Serializable editor state payload.
 */
public record EditorStateData(
    String filePath,
    int cursorLine,
    int cursorColumn,
    double verticalScrollOffset,
    String languageId,
    List<Integer> foldedLines
) {
    /**
     * Creates normalized state defaults.
     */
    public EditorStateData {
        filePath = filePath == null ? "" : filePath;
        cursorLine = Math.max(0, cursorLine);
        cursorColumn = Math.max(0, cursorColumn);
        verticalScrollOffset = Math.max(0.0, verticalScrollOffset);
        languageId = languageId == null || languageId.isBlank() ? "plain-text" : languageId;
        foldedLines = foldedLines == null ? List.of() : List.copyOf(foldedLines);
    }

    /**
     * Returns an empty default state.
     */
    public static EditorStateData empty() {
        return new EditorStateData("", 0, 0, 0.0, "plain-text", List.of());
    }
}
