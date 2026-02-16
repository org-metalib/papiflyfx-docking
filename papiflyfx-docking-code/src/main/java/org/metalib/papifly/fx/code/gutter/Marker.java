package org.metalib.papifly.fx.code.gutter;

/**
 * A single marker associated with a document line.
 *
 * @param line    zero-based line number
 * @param type    marker type
 * @param message optional descriptive message
 */
public record Marker(int line, MarkerType type, String message) {

    public Marker {
        if (line < 0) {
            throw new IllegalArgumentException("line must be >= 0");
        }
        if (type == null) {
            throw new NullPointerException("type");
        }
        message = message == null ? "" : message;
    }

    public Marker(int line, MarkerType type) {
        this(line, type, "");
    }
}
