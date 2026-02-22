package org.metalib.papifly.fx.code.render;

/**
 * Selection geometry helpers used by selection render passes.
 */
final class SelectionGeometry {

    private SelectionGeometry() {
    }

    static SelectionSpan spanForLine(
        int line,
        double viewportWidth,
        double charWidth,
        int startLine,
        int startCol,
        int endLine,
        int endCol
    ) {
        if (line < startLine || line > endLine) {
            return null;
        }
        double x;
        double width;
        if (line == startLine && line == endLine) {
            x = startCol * charWidth;
            width = (endCol - startCol) * charWidth;
        } else if (line == startLine) {
            x = startCol * charWidth;
            width = viewportWidth - x;
        } else if (line == endLine) {
            x = 0;
            width = endCol * charWidth;
        } else {
            x = 0;
            width = viewportWidth;
        }
        if (width <= 0) {
            return null;
        }
        return new SelectionSpan(x, width);
    }

    record SelectionSpan(double x, double width) {
    }
}
