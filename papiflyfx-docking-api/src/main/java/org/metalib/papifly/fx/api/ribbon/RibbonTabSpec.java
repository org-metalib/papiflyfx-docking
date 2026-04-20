package org.metalib.papifly.fx.api.ribbon;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Descriptor for a ribbon tab contributed by a {@link RibbonProvider}.
 *
 * @param id stable tab identifier
 * @param label localized tab label
 * @param order tab ordering within the ribbon; lower values sort first
 * @param contextual whether the tab is contextual and may be styled differently by the host
 * @param visibleWhen predicate that decides whether the tab is visible for a context
 * @param groups groups contributed to the tab
 */
public record RibbonTabSpec(
    String id,
    String label,
    int order,
    boolean contextual,
    Predicate<RibbonContext> visibleWhen,
    List<RibbonGroupSpec> groups
) {

    /**
     * Creates a tab descriptor.
     *
     * @param id stable tab identifier
     * @param label localized tab label
     * @param order tab ordering within the ribbon; lower values sort first
     * @param contextual whether the tab is contextual and may be styled differently by the host
     * @param visibleWhen predicate that decides whether the tab is visible for a context
     * @param groups groups contributed to the tab
     */
    public RibbonTabSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        visibleWhen = visibleWhen == null ? context -> true : visibleWhen;
        groups = List.copyOf(Objects.requireNonNull(groups, "groups"));
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("groups must not be empty");
        }
    }

    /**
     * Returns whether the tab should be visible for the supplied context.
     *
     * @param context current ribbon context
     * @return {@code true} when the tab should be shown
     */
    public boolean isVisible(RibbonContext context) {
        return visibleWhen.test(context == null ? RibbonContext.empty() : context);
    }
}
