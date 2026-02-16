package org.metalib.papifly.fx.code.document;

/**
 * Delete text command.
 */
public final class DeleteEdit implements EditCommand {

    private final int startOffset;
    private final int endOffset;
    private String deletedText;

    /**
     * Creates delete command for range [startOffset, endOffset).
     */
    public DeleteEdit(int startOffset, int endOffset) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Override
    public void apply(TextSource textSource) {
        if (deletedText == null) {
            deletedText = textSource.delete(startOffset, endOffset);
            return;
        }
        textSource.delete(startOffset, startOffset + deletedText.length());
    }

    @Override
    public void undo(TextSource textSource) {
        if (deletedText == null || deletedText.isEmpty()) {
            return;
        }
        textSource.insert(startOffset, deletedText);
    }
}
