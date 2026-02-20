package org.metalib.papifly.fx.code.state;

import java.util.List;

/**
 * Serializable editor state payload &mdash; v2 persistence contract.
 *
 * <h3>V2 field invariants</h3>
 * <ul>
 *   <li>{@code filePath} &ndash; nullable input normalized to {@code ""}.</li>
 *   <li>{@code cursorLine} &ndash; {@code >= 0}.</li>
 *   <li>{@code cursorColumn} &ndash; {@code >= 0}.</li>
 *   <li>{@code anchorLine} &ndash; {@code >= 0}.</li>
 *   <li>{@code anchorColumn} &ndash; {@code >= 0}.</li>
 *   <li>{@code verticalScrollOffset} &ndash; {@code >= 0.0}.</li>
 *   <li>{@code languageId} &ndash; blank/null normalized to {@code "plain-text"}.</li>
 *   <li>{@code foldedLines} &ndash; non-null immutable list (empty in MVP).</li>
 *   <li>{@code secondaryCarets} &ndash; non-null immutable list.</li>
 * </ul>
 *
 * <p>The canonical map key set used by {@link EditorStateCodec} is:
 * {@code filePath, cursorLine, cursorColumn, anchorLine, anchorColumn, verticalScrollOffset,
 * languageId, foldedLines, secondaryCarets}. Unknown keys present in a deserialized map are
 * silently ignored; missing keys default to the invariants above. V1 payloads without anchor and
 * secondary caret fields are migrated by defaulting anchor to cursor and secondary carets to empty.</p>
 */
public record EditorStateData(
    String filePath,
    int cursorLine,
    int cursorColumn,
    double verticalScrollOffset,
    String languageId,
    List<Integer> foldedLines,
    int anchorLine,
    int anchorColumn,
    List<CaretStateData> secondaryCarets
) {
    /**
     * Creates normalized state defaults.
     */
    public EditorStateData {
        filePath = filePath == null ? "" : filePath;
        cursorLine = Math.max(0, cursorLine);
        cursorColumn = Math.max(0, cursorColumn);
        anchorLine = Math.max(0, anchorLine);
        anchorColumn = Math.max(0, anchorColumn);
        verticalScrollOffset = Math.max(0.0, verticalScrollOffset);
        languageId = languageId == null || languageId.isBlank() ? "plain-text" : languageId;
        foldedLines = foldedLines == null ? List.of() : List.copyOf(foldedLines);
        secondaryCarets = secondaryCarets == null ? List.of() : List.copyOf(secondaryCarets);
    }

    /**
     * Backward-compatible constructor for callers still creating v1-shaped state.
     */
    public EditorStateData(
        String filePath,
        int cursorLine,
        int cursorColumn,
        double verticalScrollOffset,
        String languageId,
        List<Integer> foldedLines
    ) {
        this(
            filePath,
            cursorLine,
            cursorColumn,
            verticalScrollOffset,
            languageId,
            foldedLines,
            cursorLine,
            cursorColumn,
            List.of()
        );
    }

    /**
     * Returns an empty default state.
     */
    public static EditorStateData empty() {
        return new EditorStateData("", 0, 0, 0.0, "plain-text", List.of(), 0, 0, List.of());
    }
}
