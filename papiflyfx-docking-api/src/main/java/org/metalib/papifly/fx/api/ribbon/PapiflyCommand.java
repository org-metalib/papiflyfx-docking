package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;

/**
 * UI-agnostic command metadata used by ribbon controls.
 *
 * <p>A command exposes stable identity, localized presentation strings,
 * optional icon handles, observable {@link BoolState} fields for runtime state,
 * and an execution callback. The model intentionally avoids any dependency on
 * JavaFX scene graph or property types so the same command can back a ribbon
 * button, a context menu item, a keyboard shortcut, or a non-JavaFX surface.</p>
 *
 * <p><b>Ribbon 2 contract break:</b> the previous {@code enabledProperty} and
 * {@code selectedProperty} components were JavaFX {@code BooleanProperty}
 * values exposed in the shared API. They have been replaced with
 * {@link BoolState} components named {@code enabled} and {@code selected}.
 * Runtime hosts adapt these to their UI toolkit; see
 * {@code JavaFxCommandBindings} in {@code papiflyfx-docking-docks} for the
 * JavaFX adapter.</p>
 *
 * @param id stable command identifier used for lookup and persistence; hosts
 *     canonicalize by id, keep the first metadata surface, and refresh
 *     enabled/selected state plus action dispatch from later provider
 *     emissions with the same id
 * @param label localized user-facing label
 * @param tooltip localized descriptive tooltip
 * @param smallIcon small icon handle, typically for 16x16 assets
 * @param largeIcon large icon handle, typically for 32x32 assets
 * @param enabled mutable enabled state observed by the UI
 * @param selected mutable selected state for toggle-like commands
 * @param action execution callback invoked when the command runs
 * @since Ribbon 2
 */
public record PapiflyCommand(
    String id,
    String label,
    String tooltip,
    RibbonIconHandle smallIcon,
    RibbonIconHandle largeIcon,
    BoolState enabled,
    BoolState selected,
    Runnable action
) {

    /**
     * Creates a command with explicit metadata and state values.
     *
     * @param id stable command identifier used for lookup and persistence
     * @param label localized user-facing label
     * @param tooltip localized descriptive tooltip
     * @param smallIcon small icon handle, typically for 16x16 assets
     * @param largeIcon large icon handle, typically for 32x32 assets
     * @param enabled mutable enabled state observed by the UI; {@code null} defaults to enabled
     * @param selected mutable selected state for toggle-like commands; {@code null} defaults to unselected
     * @param action execution callback invoked when the command runs
     */
    public PapiflyCommand {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
        tooltip = tooltip == null || tooltip.isBlank() ? label : tooltip;
        enabled = enabled == null ? new MutableBoolState(true) : enabled;
        selected = selected == null ? new MutableBoolState(false) : selected;
    }

    /**
     * Creates a command with default tooltip text and default enabled/selected
     * states.
     *
     * @param id stable command identifier used for lookup and persistence
     * @param label localized user-facing label
     * @param action execution callback invoked when the command runs
     * @return a new command with default state
     */
    public static PapiflyCommand of(String id, String label, Runnable action) {
        return new PapiflyCommand(id, label, label, null, null, null, null, action);
    }

    /**
     * Executes the command if it is currently enabled.
     */
    public void execute() {
        if (enabled.get()) {
            action.run();
        }
    }
}
