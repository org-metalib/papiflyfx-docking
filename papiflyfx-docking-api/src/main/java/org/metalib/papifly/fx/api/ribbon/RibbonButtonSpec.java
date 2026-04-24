package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;

/**
 * Descriptor for a standard ribbon push button bound to a command.
 *
 * @param command command rendered by the button
 */
public record RibbonButtonSpec(RibbonCommand command) implements RibbonControlSpec {

    /**
     * Creates a button descriptor.
     *
     * @param command command rendered by the button
     */
    public RibbonButtonSpec {
        Objects.requireNonNull(command, "command");
    }

    @Override
    public String id() {
        return command.id();
    }

    @Override
    public RibbonControlKind kind() {
        return RibbonControlKind.BUTTON;
    }
}
