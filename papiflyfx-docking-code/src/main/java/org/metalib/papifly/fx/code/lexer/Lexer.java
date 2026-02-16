package org.metalib.papifly.fx.code.lexer;

/**
 * Stateful single-line lexer contract.
 */
public interface Lexer {

    /**
     * Returns stable language identifier.
     */
    String languageId();

    /**
     * Returns the initial lexer state for line 0.
     */
    default LexState initialState() {
        return LexState.DEFAULT;
    }

    /**
     * Lexes one line using entry state and returns tokens with exit state.
     */
    LexResult lexLine(String lineText, LexState entryState);
}
