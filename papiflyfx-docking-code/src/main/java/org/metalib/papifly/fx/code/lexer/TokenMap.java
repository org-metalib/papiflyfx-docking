package org.metalib.papifly.fx.code.lexer;

import java.util.List;

/**
 * Immutable per-line token storage.
 */
public final class TokenMap {

    private static final TokenMap EMPTY = new TokenMap(List.of());

    private final List<LineTokens> lines;

    /**
     * Creates an immutable token map.
     */
    public TokenMap(List<LineTokens> lines) {
        this.lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /**
     * Returns the shared empty token map.
     */
    public static TokenMap empty() {
        return EMPTY;
    }

    /**
     * Returns line count in this token map.
     */
    public int lineCount() {
        return lines.size();
    }

    /**
     * Returns the tokenized line entry or null if out of range.
     */
    public LineTokens lineAt(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return null;
        }
        return lines.get(lineIndex);
    }

    /**
     * Returns tokens for a line or an empty list if out of range.
     */
    public List<Token> tokensForLine(int lineIndex) {
        LineTokens line = lineAt(lineIndex);
        if (line == null) {
            return List.of();
        }
        return line.tokens();
    }

    /**
     * Returns immutable line entries.
     */
    public List<LineTokens> lines() {
        return lines;
    }
}
