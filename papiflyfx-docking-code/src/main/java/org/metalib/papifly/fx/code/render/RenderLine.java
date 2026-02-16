package org.metalib.papifly.fx.code.render;

/**
 * Per-line render data used by the viewport.
 *
 * @param lineIndex zero-based line number in the document
 * @param text      the line text content (without trailing newline)
 * @param y         y-coordinate in the canvas for this line
 */
public record RenderLine(int lineIndex, String text, double y) {
}
