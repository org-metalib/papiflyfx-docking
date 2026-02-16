package org.metalib.papifly.fx.code.render;

import org.metalib.papifly.fx.code.lexer.Token;

import java.util.List;

/**
 * Per-line render data used by the viewport.
 *
 * @param lineIndex zero-based line number in the document
 * @param text      the line text content (without trailing newline)
 * @param y         y-coordinate in the canvas for this line
 * @param tokens    syntax tokens for this line
 */
public record RenderLine(int lineIndex, String text, double y, List<Token> tokens) {

    public RenderLine {
        text = text == null ? "" : text;
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
    }
}
