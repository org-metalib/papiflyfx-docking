package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;

/**
 * Descriptor for a ribbon toggle button bound to a command.
 *
 * <p>The host binds the toggle selection state to the command's
 * {@code selectedProperty}.</p>
 *
 * @param command command rendered by the toggle button
 */
public record RibbonToggleSpec(PapiflyCommand command) implements RibbonControlSpec {

    /**
     * Creates a toggle descriptor.
     *
     * @param command command rendered by the toggle button
     */
    public RibbonToggleSpec {
        Objects.requireNonNull(command, "command");
    }

    @Override
    public String id() {
        return command.id();
    }
}
