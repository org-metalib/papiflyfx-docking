package org.metalib.papifly.fx.code.state;

import java.util.List;

/**
 * Serializable editor state payload &mdash; v1 persistence contract.
 *
 * <h3>V1 field invariants</h3>
 * <ul>
 *   <li>{@code filePath} &ndash; nullable input normalized to {@code ""}.</li>
 *   <li>{@code cursorLine} &ndash; {@code >= 0}.</li>
 *   <li>{@code cursorColumn} &ndash; {@code >= 0}.</li>
 *   <li>{@code verticalScrollOffset} &ndash; {@code >= 0.0}.</li>
 *   <li>{@code languageId} &ndash; blank/null normalized to {@code "plain-text"}.</li>
 *   <li>{@code foldedLines} &ndash; non-null immutable list (empty in MVP).</li>
 * </ul>
 *
 * <p>The canonical map key set used by {@link EditorStateCodec} is:
 * {@code filePath, cursorLine, cursorColumn, verticalScrollOffset, languageId, foldedLines}.
 * Unknown keys present in a deserialized map are silently ignored; missing keys
 * default to the invariants above.</p>
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
