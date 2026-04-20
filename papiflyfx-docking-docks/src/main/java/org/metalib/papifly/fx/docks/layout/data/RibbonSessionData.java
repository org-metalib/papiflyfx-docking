package org.metalib.papifly.fx.docks.layout.data;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Optional ribbon-specific session payload.
 *
 * @param minimized whether the ribbon shell is minimized
 * @param selectedTabId selected ribbon tab identifier
 * @param quickAccessCommandIds command identifiers rendered in the Quick Access Toolbar
 */
public record RibbonSessionData(
    boolean minimized,
    String selectedTabId,
    List<String> quickAccessCommandIds
) {

    /**
     * Creates a ribbon session payload.
     *
     * @param minimized whether the ribbon shell is minimized
     * @param selectedTabId selected ribbon tab identifier
     * @param quickAccessCommandIds command identifiers rendered in the Quick Access Toolbar
     */
    public RibbonSessionData {
        selectedTabId = selectedTabId == null || selectedTabId.isBlank() ? null : selectedTabId;
        quickAccessCommandIds = List.copyOf(new LinkedHashSet<>(
            Objects.requireNonNullElse(quickAccessCommandIds, List.<String>of()).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList()
        ));
    }
}
