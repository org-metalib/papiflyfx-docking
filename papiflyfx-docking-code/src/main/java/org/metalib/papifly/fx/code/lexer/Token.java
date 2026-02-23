package org.metalib.papifly.fx.code.lexer;

import java.util.Objects;

/**
 * A token slice in a single line.
 *
 * @param startColumn token start column (inclusive)
 * @param length      token length in characters
 * @param type        token category
 */
public record Token(int startColumn, int length, TokenType type) {

    /**
     * Creates a token with validated bounds and type.
     */
    public Token {
        if (startColumn < 0) {
            throw new IllegalArgumentException("startColumn must be >= 0");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        type = Objects.requireNonNull(type, "type");
    }

    /**
     * Returns the end column (exclusive).
     *
     * @return end column (exclusive)
     */
    public int endColumn() {
        return startColumn + length;
    }
}
