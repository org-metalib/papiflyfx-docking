package org.metalib.papifly.fx.code.command;

/**
 * Identifies every editor action that can be triggered by a keystroke.
 */
public enum EditorCommand {

    // Undo / Redo
    UNDO, REDO,

    // Clipboard
    COPY, CUT, PASTE, SELECT_ALL,

    // Basic caret movement
    MOVE_LEFT, MOVE_RIGHT, MOVE_UP, MOVE_DOWN,

    // Basic selection
    SELECT_LEFT, SELECT_RIGHT, SELECT_UP, SELECT_DOWN,

    // Line start / end
    LINE_START, LINE_END,
    SELECT_TO_LINE_START, SELECT_TO_LINE_END,

    // Editing
    BACKSPACE, DELETE, ENTER,

    // Phase 1 — word navigation
    MOVE_WORD_LEFT, MOVE_WORD_RIGHT,
    SELECT_WORD_LEFT, SELECT_WORD_RIGHT,
    DELETE_WORD_LEFT, DELETE_WORD_RIGHT,

    // Phase 1 — document boundaries
    DOCUMENT_START, DOCUMENT_END,
    SELECT_TO_DOCUMENT_START, SELECT_TO_DOCUMENT_END,

    // Phase 2 — line operations
    DELETE_LINE,
    MOVE_LINE_UP, MOVE_LINE_DOWN,
    DUPLICATE_LINE_UP, DUPLICATE_LINE_DOWN,
    JOIN_LINES,

    // Always-on shortcuts (handled before search-focus guard)
    OPEN_SEARCH, GO_TO_LINE
}
