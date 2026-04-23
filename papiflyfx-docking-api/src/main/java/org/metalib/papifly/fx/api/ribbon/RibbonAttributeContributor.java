package org.metalib.papifly.fx.api.ribbon;

import java.util.Map;

/**
 * Optional contract for active content that publishes ribbon metadata.
 *
 * <p>Dock hosts may inspect the active content root for this interface while
 * building a {@link RibbonContext}. Contributed attributes are metadata for
 * visibility and presentation decisions, not executable integrations. Use
 * {@link RibbonCapabilityContributor} for action interfaces.</p>
 *
 * <p>Contributed keys should be standard keys from
 * {@link RibbonContextAttributes} or provider-owned keys following the
 * {@link RibbonAttributeKey} namespacing guidance.</p>
 *
 * @since Ribbon 5 Phase 2
 */
public interface RibbonAttributeContributor {

    /**
     * Returns metadata attributes exposed by the active content.
     *
     * <p>Hosts should ignore entries whose key is {@code null}, whose value is
     * {@code null}, or whose value is incompatible with the key's declared
     * type.</p>
     *
     * @return typed ribbon attributes, or an empty map when none are available
     */
    Map<? extends RibbonAttributeKey<?>, ?> ribbonAttributes();
}
