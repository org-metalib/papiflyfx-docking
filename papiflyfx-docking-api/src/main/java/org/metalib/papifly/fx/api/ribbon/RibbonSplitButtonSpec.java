package org.metalib.papifly.fx.api.ribbon;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor for a ribbon split button with a primary action plus secondary
 * command choices.
 *
 * @param primaryCommand primary command invoked by the main button surface
 * @param secondaryCommands commands shown in the split dropdown
 */
public record RibbonSplitButtonSpec(
    PapiflyCommand primaryCommand,
    List<PapiflyCommand> secondaryCommands
) implements RibbonControlSpec {

    /**
     * Creates a split button descriptor.
     *
     * @param primaryCommand primary command invoked by the main button surface
     * @param secondaryCommands commands shown in the split dropdown
     */
    public RibbonSplitButtonSpec {
        Objects.requireNonNull(primaryCommand, "primaryCommand");
        secondaryCommands = List.copyOf(Objects.requireNonNull(secondaryCommands, "secondaryCommands"));
        if (secondaryCommands.isEmpty()) {
            throw new IllegalArgumentException("secondaryCommands must not be empty");
        }
    }

    @Override
    public String id() {
        return primaryCommand.id();
    }
}
