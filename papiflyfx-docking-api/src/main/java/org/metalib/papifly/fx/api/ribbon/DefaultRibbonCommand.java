package org.metalib.papifly.fx.api.ribbon;

record DefaultRibbonCommand(
    String id,
    String label,
    String tooltip,
    RibbonIconHandle smallIcon,
    RibbonIconHandle largeIcon,
    RibbonBooleanState enabled,
    Runnable action
) implements RibbonCommand {

    DefaultRibbonCommand {
        RibbonCommand.validate(id, label, action);
        tooltip = tooltip == null || tooltip.isBlank() ? label : tooltip;
        enabled = enabled == null ? RibbonBooleanState.constant(true) : enabled;
    }
}
