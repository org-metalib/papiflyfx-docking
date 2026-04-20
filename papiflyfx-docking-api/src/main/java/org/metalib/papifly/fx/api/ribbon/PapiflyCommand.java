package org.metalib.papifly.fx.api.ribbon;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Objects;

/**
 * UI-agnostic command metadata used by ribbon controls.
 *
 * <p>A command exposes stable identity, localized presentation strings,
 * optional icon handles, state properties, and an execution callback. The
 * model avoids any dependency on JavaFX scene graph types so the same command
 * can back a ribbon button, a context menu item, or a keyboard shortcut.</p>
 *
 * @param id stable command identifier used for lookup and persistence
 * @param label localized user-facing label
 * @param tooltip localized descriptive tooltip
 * @param smallIcon small icon handle, typically for 16x16 assets
 * @param largeIcon large icon handle, typically for 32x32 assets
 * @param enabledProperty mutable enabled state observed by the UI
 * @param selectedProperty mutable selected state for toggle-like commands
 * @param action execution callback invoked when the command runs
 */
public record PapiflyCommand(
    String id,
    String label,
    String tooltip,
    RibbonIconHandle smallIcon,
    RibbonIconHandle largeIcon,
    BooleanProperty enabledProperty,
    BooleanProperty selectedProperty,
    Runnable action
) {

    /**
     * Creates a command with explicit metadata and state properties.
     *
     * @param id stable command identifier used for lookup and persistence
     * @param label localized user-facing label
     * @param tooltip localized descriptive tooltip
     * @param smallIcon small icon handle, typically for 16x16 assets
     * @param largeIcon large icon handle, typically for 32x32 assets
     * @param enabledProperty mutable enabled state observed by the UI
     * @param selectedProperty mutable selected state for toggle-like commands
     * @param action execution callback invoked when the command runs
     */
    public PapiflyCommand {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
        tooltip = tooltip == null || tooltip.isBlank() ? label : tooltip;
        enabledProperty = enabledProperty == null ? new SimpleBooleanProperty(true) : enabledProperty;
        selectedProperty = selectedProperty == null ? new SimpleBooleanProperty(false) : selectedProperty;
    }

    /**
     * Creates a command with default tooltip text and default enabled/selected
     * properties.
     *
     * @param id stable command identifier used for lookup and persistence
     * @param label localized user-facing label
     * @param action execution callback invoked when the command runs
     * @return a new command with default state properties
     */
    public static PapiflyCommand of(String id, String label, Runnable action) {
        return new PapiflyCommand(id, label, label, null, null, null, null, action);
    }

    /**
     * Executes the command if it is currently enabled.
     */
    public void execute() {
        if (enabledProperty.get()) {
            action.run();
        }
    }
}
