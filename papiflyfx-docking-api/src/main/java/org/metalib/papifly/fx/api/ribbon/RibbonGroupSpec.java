package org.metalib.papifly.fx.api.ribbon;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor for a ribbon group within a tab.
 *
 * <p>Groups cluster related controls and provide stable IDs that the host can
 * later use for layout, scaling, and persistence.</p>
 *
 * <p><b>Collapse-order semantics (Ribbon 2):</b> the {@code collapseOrder}
 * component is a small integer that defines the order in which the adaptive
 * layout shrinks groups when horizontal space becomes constrained. The
 * runtime sorts groups by ascending {@code collapseOrder} and reduces them in
 * that order from {@code LARGE} towards {@code COLLAPSED}.</p>
 *
 * <ul>
 *   <li><b>Smaller</b> values collapse <b>earlier</b>; use them for groups
 *       that are least essential to keep at full size.</li>
 *   <li><b>Larger</b> values collapse <b>later</b>; use them for groups whose
 *       commands the user is most likely to need at a glance.</li>
 * </ul>
 *
 * <p>Recommended convention: use {@code 10} for "collapse first",
 * {@code 20} for "intermediate", and {@code 30} for "keep visible". Groups
 * with equal {@code collapseOrder} are then disambiguated by {@link #order()}
 * and {@link #id()} so behavior is deterministic across providers.</p>
 *
 * <p><b>Ribbon 2 contract break:</b> the previous {@code reductionPriority}
 * component has been renamed to {@code collapseOrder}. The numeric semantics
 * are preserved (smaller value = collapses earlier) but the new name removes
 * ambiguity around what "priority" means in context. Existing call sites and
 * tests must be updated.</p>
 *
 * @param id stable group identifier
 * @param label localized group label
 * @param order group ordering within the tab; lower values sort first
 * @param collapseOrder adaptive-layout collapse order; lower values collapse first
 * @param dialogLauncher optional command rendered as a group launcher affordance
 * @param controls controls contained in the group
 * @since Ribbon 2
 */
public record RibbonGroupSpec(
    String id,
    String label,
    int order,
    int collapseOrder,
    RibbonCommand dialogLauncher,
    List<RibbonControlSpec> controls
) {

    /**
     * Creates a group descriptor.
     *
     * @param id stable group identifier
     * @param label localized group label
     * @param order group ordering within the tab; lower values sort first
     * @param collapseOrder adaptive-layout collapse order; lower values collapse first
     * @param dialogLauncher optional command rendered as a group launcher affordance
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
