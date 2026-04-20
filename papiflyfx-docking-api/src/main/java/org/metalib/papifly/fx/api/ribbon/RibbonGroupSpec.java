package org.metalib.papifly.fx.api.ribbon;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor for a ribbon group within a tab.
 *
 * <p>Groups cluster related controls and provide stable IDs that the host can
 * later use for layout, scaling, and persistence.</p>
 *
 * @param id stable group identifier
 * @param label localized group label
 * @param order group ordering within the tab; lower values sort first
 * @param reductionPriority lower-priority groups collapse earlier when space is constrained
 * @param controls controls contained in the group
 */
public record RibbonGroupSpec(
    String id,
    String label,
    int order,
    int reductionPriority,
    List<RibbonControlSpec> controls
) {

    /**
     * Creates a group descriptor.
     *
     * @param id stable group identifier
     * @param label localized group label
     * @param order group ordering within the tab; lower values sort first
     * @param reductionPriority lower-priority groups collapse earlier when space is constrained
     * @param controls controls contained in the group
     */
    public RibbonGroupSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        controls = List.copyOf(Objects.requireNonNull(controls, "controls"));
        if (controls.isEmpty()) {
            throw new IllegalArgumentException("controls must not be empty");
        }
    }
}
