package org.metalib.papifly.fx.code.lexer;

/**
 * Line-exit lexer state used for incremental re-lex propagation.
 *
 * @param code state code
 */
public record LexState(int code) {

    /**
     * Default lexer state.
     */
    public static final LexState DEFAULT = new LexState(0);

    /**
     * Creates a lexer state.
     */
    public LexState {
        if (code < 0) {
            throw new IllegalArgumentException("code must be >= 0");
        }
    }

    /**
     * Returns a state instance for a code.
     *
     * @param code state code
     * @return cached default state for zero, otherwise a new state with the provided code
     */
    public static LexState of(int code) {
        if (code == 0) {
            return DEFAULT;
        }
        return new LexState(code);
    }
}
