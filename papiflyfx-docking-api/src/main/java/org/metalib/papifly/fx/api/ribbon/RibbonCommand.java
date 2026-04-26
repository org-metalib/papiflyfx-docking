package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;

/**
 * UI-neutral ribbon action command.
 *
 * <p>Action commands expose identity, presentation metadata, enabled state,
 * and execution only. Toggle selection state is intentionally not available on
 * this base contract.</p>
 *
 * @since Ribbon 6
 */
public interface RibbonCommand {

    String id();

    String label();

    String tooltip();

    RibbonIconHandle smallIcon();

    RibbonIconHandle largeIcon();

    RibbonBooleanState enabled();

    Runnable action();

    /**
     * Executes the command if it is currently enabled.
     */
    default void execute() {
        if (enabled().get()) {
            action().run();
        }
    }

    /**
     * Creates an action-only command with default enabled state.
     *
     * @param id stable command identifier
     * @param label localized label
     * @param action execution callback
     * @return action-only ribbon command
     */
    static RibbonCommand of(String id, String label, Runnable action) {
        return new DefaultRibbonCommand(id, label, label, null, null, null, action);
    }

    /**
     * Creates an action-only command with explicit metadata.
     *
     * @param id stable command identifier
     * @param label localized label
     * @param tooltip localized tooltip
     * @param smallIcon small icon handle
     * @param largeIcon large icon handle
     * @param enabled enabled state; {@code null} defaults to enabled
     * @param action execution callback
     * @return action-only ribbon command
     */
    static RibbonCommand of(
        String id,
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        RibbonBooleanState enabled,
        Runnable action
    ) {
        return new DefaultRibbonCommand(id, label, tooltip, smallIcon, largeIcon, enabled, action);
    }

    static void validate(String id, String label, Runnable action) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
    }
}
