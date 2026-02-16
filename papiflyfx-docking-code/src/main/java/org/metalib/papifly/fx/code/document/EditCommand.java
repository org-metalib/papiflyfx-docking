package org.metalib.papifly.fx.code.document;

/**
 * Internal command contract for document undo/redo operations.
 */
interface EditCommand {

    /**
     * Applies the command.
     */
    void apply(TextSource textSource);

    /**
     * Reverts the command.
     */
    void undo(TextSource textSource);
}
