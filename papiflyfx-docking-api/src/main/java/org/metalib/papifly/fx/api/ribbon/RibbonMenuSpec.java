package org.metalib.papifly.fx.api.ribbon;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor for a ribbon dropdown menu control.
 *
 * @param id stable control identifier
 * @param label localized menu label
 * @param tooltip localized descriptive tooltip
 * @param smallIcon small icon handle, typically for 16x16 assets
 * @param largeIcon large icon handle, typically for 32x32 assets
 * @param items commands presented by the dropdown menu
 */
public record RibbonMenuSpec(
    String id,
    String label,
    String tooltip,
    RibbonIconHandle smallIcon,
    RibbonIconHandle largeIcon,
    List<PapiflyCommand> items
) implements RibbonControlSpec {

    /**
     * Creates a menu descriptor.
     *
     * @param id stable control identifier
     * @param label localized menu label
     * @param tooltip localized descriptive tooltip
     * @param smallIcon small icon handle, typically for 16x16 assets
     * @param largeIcon large icon handle, typically for 32x32 assets
     * @param items commands presented by the dropdown menu
     */
    public RibbonMenuSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        tooltip = tooltip == null || tooltip.isBlank() ? label : tooltip;
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
    }
}
