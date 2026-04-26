package org.metalib.papifly.fx.api.ribbon;

/**
 * Ribbon command with toggle selection state.
 *
 * <p>Use this contract only for controls that need a selected/unselected
 * value. Action-only controls should depend on {@link RibbonCommand}.</p>
 *
 * @since Ribbon 6
 */
public interface RibbonToggleCommand extends RibbonCommand {

    /**
     * Returns the observable selected state for this toggle command.
     *
     * @return selected state
     */
    MutableRibbonBooleanState selected();

    /**
     * Creates a toggle-capable command with default enabled state.
     *
     * @param id stable command identifier
     * @param label localized label
     * @param selected selected state
     * @param action execution callback
     * @return toggle command
     */
    static RibbonToggleCommand of(
        String id,
        String label,
        MutableRibbonBooleanState selected,
        Runnable action
    ) {
        return new DefaultRibbonToggleCommand(id, label, label, null, null, null, selected, action);
    }

    /**
     * Creates a toggle-capable command with explicit metadata.
     *
     * @param id stable command identifier
     * @param label localized label
     * @param tooltip localized tooltip
     * @param smallIcon small icon handle
     * @param largeIcon large icon handle
     * @param enabled enabled state; {@code null} defaults to enabled
     * @param selected selected state; {@code null} defaults to unselected
     * @param action execution callback
     * @return toggle command
     */
    static RibbonToggleCommand of(
        String id,
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        RibbonBooleanState enabled,
        MutableRibbonBooleanState selected,
        Runnable action
    ) {
        return new DefaultRibbonToggleCommand(id, label, tooltip, smallIcon, largeIcon, enabled, selected, action);
    }
}
