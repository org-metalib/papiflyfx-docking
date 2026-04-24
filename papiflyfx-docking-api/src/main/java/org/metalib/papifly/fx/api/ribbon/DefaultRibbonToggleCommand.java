package org.metalib.papifly.fx.api.ribbon;

record DefaultRibbonToggleCommand(
    String id,
    String label,
    String tooltip,
    RibbonIconHandle smallIcon,
    RibbonIconHandle largeIcon,
    RibbonBooleanState enabled,
    MutableRibbonBooleanState selected,
    Runnable action
) implements RibbonToggleCommand {

    DefaultRibbonToggleCommand {
        RibbonCommand.validate(id, label, action);
        tooltip = tooltip == null || tooltip.isBlank() ? label : tooltip;
        enabled = enabled == null ? RibbonBooleanState.constant(true) : enabled;
        selected = selected == null ? new DefaultRibbonBooleanState(false) : selected;
    }
}
