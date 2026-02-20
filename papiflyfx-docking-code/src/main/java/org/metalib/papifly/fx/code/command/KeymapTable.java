package org.metalib.papifly.fx.code.command;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps physical key combinations to {@link EditorCommand} identifiers.
 * <p>
 * Builds a platform-appropriate table on first use. On macOS the "word"
 * modifier is {@code Alt}; on Windows/Linux it is {@code Ctrl}.
 */
public final class KeymapTable {

    private static final Map<KeyBinding, EditorCommand> TABLE = buildTable();

    private KeymapTable() {}

    /**
     * Resolves a JavaFX key event to an editor command.
     */
    public static Optional<EditorCommand> resolve(KeyEvent event) {
        boolean shift = event.isShiftDown();
        boolean shortcut = event.isControlDown() || event.isMetaDown();
        boolean alt = event.isAltDown();
        KeyBinding binding = new KeyBinding(event.getCode(), shift, shortcut, alt);
        return Optional.ofNullable(TABLE.get(binding));
    }

    static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static Map<KeyBinding, EditorCommand> buildTable() {
        Map<KeyBinding, EditorCommand> map = new HashMap<>();
        boolean mac = isMac();

        // --- basic navigation (no modifiers) ---
        put(map, KeyCode.LEFT, false, false, false, EditorCommand.MOVE_LEFT);
        put(map, KeyCode.RIGHT, false, false, false, EditorCommand.MOVE_RIGHT);
        put(map, KeyCode.UP, false, false, false, EditorCommand.MOVE_UP);
        put(map, KeyCode.DOWN, false, false, false, EditorCommand.MOVE_DOWN);
        put(map, KeyCode.HOME, false, false, false, EditorCommand.LINE_START);
        put(map, KeyCode.END, false, false, false, EditorCommand.LINE_END);

        // --- shift selection ---
        put(map, KeyCode.LEFT, true, false, false, EditorCommand.SELECT_LEFT);
        put(map, KeyCode.RIGHT, true, false, false, EditorCommand.SELECT_RIGHT);
        put(map, KeyCode.UP, true, false, false, EditorCommand.SELECT_UP);
        put(map, KeyCode.DOWN, true, false, false, EditorCommand.SELECT_DOWN);
        put(map, KeyCode.HOME, true, false, false, EditorCommand.SELECT_TO_LINE_START);
        put(map, KeyCode.END, true, false, false, EditorCommand.SELECT_TO_LINE_END);

        // --- editing ---
        put(map, KeyCode.BACK_SPACE, false, false, false, EditorCommand.BACKSPACE);
        put(map, KeyCode.DELETE, false, false, false, EditorCommand.DELETE);
        put(map, KeyCode.ENTER, false, false, false, EditorCommand.ENTER);

        // --- clipboard / undo / redo / select-all (shortcut) ---
        put(map, KeyCode.A, false, true, false, EditorCommand.SELECT_ALL);
        put(map, KeyCode.C, false, true, false, EditorCommand.COPY);
        put(map, KeyCode.X, false, true, false, EditorCommand.CUT);
        put(map, KeyCode.V, false, true, false, EditorCommand.PASTE);
        put(map, KeyCode.Z, false, true, false, EditorCommand.UNDO);
        put(map, KeyCode.Z, true, true, false, EditorCommand.REDO);
        put(map, KeyCode.Y, false, true, false, EditorCommand.REDO);

        // --- search / go-to-line ---
        put(map, KeyCode.F, false, true, false, EditorCommand.OPEN_SEARCH);
        put(map, KeyCode.G, false, true, false, EditorCommand.GO_TO_LINE);

        // --- word navigation ---
        // macOS: Alt+Arrow   Windows/Linux: Ctrl+Arrow
        if (mac) {
            put(map, KeyCode.LEFT, false, false, true, EditorCommand.MOVE_WORD_LEFT);
            put(map, KeyCode.RIGHT, false, false, true, EditorCommand.MOVE_WORD_RIGHT);
            put(map, KeyCode.LEFT, true, false, true, EditorCommand.SELECT_WORD_LEFT);
            put(map, KeyCode.RIGHT, true, false, true, EditorCommand.SELECT_WORD_RIGHT);
            put(map, KeyCode.BACK_SPACE, false, false, true, EditorCommand.DELETE_WORD_LEFT);
            put(map, KeyCode.DELETE, false, false, true, EditorCommand.DELETE_WORD_RIGHT);
        } else {
            put(map, KeyCode.LEFT, false, true, false, EditorCommand.MOVE_WORD_LEFT);
            put(map, KeyCode.RIGHT, false, true, false, EditorCommand.MOVE_WORD_RIGHT);
            put(map, KeyCode.LEFT, true, true, false, EditorCommand.SELECT_WORD_LEFT);
            put(map, KeyCode.RIGHT, true, true, false, EditorCommand.SELECT_WORD_RIGHT);
            put(map, KeyCode.BACK_SPACE, false, true, false, EditorCommand.DELETE_WORD_LEFT);
            put(map, KeyCode.DELETE, false, true, false, EditorCommand.DELETE_WORD_RIGHT);
        }

        // --- document start / end ---
        if (mac) {
            // Cmd+Up / Cmd+Down on macOS
            put(map, KeyCode.UP, false, true, false, EditorCommand.DOCUMENT_START);
            put(map, KeyCode.DOWN, false, true, false, EditorCommand.DOCUMENT_END);
            put(map, KeyCode.UP, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_START);
            put(map, KeyCode.DOWN, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_END);
        } else {
            put(map, KeyCode.HOME, false, true, false, EditorCommand.DOCUMENT_START);
            put(map, KeyCode.END, false, true, false, EditorCommand.DOCUMENT_END);
            put(map, KeyCode.HOME, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_START);
            put(map, KeyCode.END, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_END);
        }

        // --- line operations ---
        // Delete line: Ctrl+Shift+K (both platforms)
        put(map, KeyCode.K, true, true, false, EditorCommand.DELETE_LINE);

        // Move line: Alt+Up / Alt+Down (both platforms)
        put(map, KeyCode.UP, false, false, true, EditorCommand.MOVE_LINE_UP);
        put(map, KeyCode.DOWN, false, false, true, EditorCommand.MOVE_LINE_DOWN);

        // Duplicate line: Alt+Shift+Up / Alt+Shift+Down
        put(map, KeyCode.UP, true, false, true, EditorCommand.DUPLICATE_LINE_UP);
        put(map, KeyCode.DOWN, true, false, true, EditorCommand.DUPLICATE_LINE_DOWN);

        // Join lines: Ctrl+J
        put(map, KeyCode.J, false, true, false, EditorCommand.JOIN_LINES);

        // --- macOS word-move keys override some Alt+Arrow entries ---
        // On macOS Alt+Up/Down are already word nav on some editors,
        // but VS Code uses them for line move, so we keep line-move.

        return Map.copyOf(map);
    }

    private static void put(Map<KeyBinding, EditorCommand> map,
                             KeyCode code, boolean shift, boolean shortcut, boolean alt,
                             EditorCommand command) {
        map.put(new KeyBinding(code, shift, shortcut, alt), command);
    }
}
