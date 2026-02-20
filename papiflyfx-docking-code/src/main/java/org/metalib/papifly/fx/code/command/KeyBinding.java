package org.metalib.papifly.fx.code.command;

import javafx.scene.input.KeyCode;

/**
 * Immutable key combination used as a lookup key in the keymap table.
 */
public record KeyBinding(KeyCode code, boolean shift, boolean shortcut, boolean alt) {
}
