package org.metalib.papifly.fx.code.document;

import java.util.Objects;

/**
 * Replace text command.
 */
public final class ReplaceEdit implements EditCommand {

    private final int startOffset;
    private final int endOffset;
    private final String replacement;
    private String originalText;

    /**
     * Creates replace command for range [startOffset, endOffset).
     */
    public ReplaceEdit(int startOffset, int endOffset, String replacement) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.replacement = Objects.requireNonNullElse(replacement, "");
    }

    @Override
    public void apply(TextSource textSource) {
        if (originalText == null) {
            originalText = textSource.replace(startOffset, endOffset, replacement);
            return;
        }
        textSource.replace(startOffset, startOffset + originalText.length(), replacement);
    }

    @Override
    public void undo(TextSource textSource) {
        if (originalText == null) {
            return;
        }
        textSource.replace(startOffset, startOffset + replacement.length(), originalText);
    }
}
