package org.metalib.papifly.fx.code.document;

import java.util.Objects;

/**
 * Insert text command.
 */
public final class InsertEdit implements EditCommand {

    private final int offset;
    private final String text;

    /**
     * Creates insert command.
     */
    public InsertEdit(int offset, String text) {
        this.offset = offset;
        this.text = Objects.requireNonNullElse(text, "");
    }

    @Override
    public void apply(TextSource textSource) {
        textSource.insert(offset, text);
    }

    @Override
    public void undo(TextSource textSource) {
        if (text.isEmpty()) {
            return;
        }
        textSource.delete(offset, offset + text.length());
    }
}
