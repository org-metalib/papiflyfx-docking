package org.metalib.papifly.fx.api.ribbon;

import java.util.Map;

/**
 * Optional contract for active content that publishes ribbon capabilities.
 *
 * <p>Dock hosts may inspect the active content root for this interface while
 * building a {@link RibbonContext}. This lets content expose one or more
 * explicit action interfaces even when the root node itself is only a view and
 * delegates behavior to nested controllers.</p>
 *
 * <p>The older convention where the active root node directly implements an
 * action interface remains supported by runtime hosts as a compatibility
 * fallback.</p>
 *
 * @since Ribbon 5 Phase 2
 */
public interface RibbonCapabilityContributor {

    /**
     * Returns capabilities exposed by the active content.
     *
     * <p>Keys are capability interfaces or classes and values are the
     * instances returned by {@link RibbonContext#capability(Class)}. Hosts
     * should ignore entries whose key is {@code null}, whose value is
     * {@code null}, or whose value is incompatible with the key.</p>
     *
     * @return typed ribbon capabilities, or an empty map when none are available
     */
    Map<? extends Class<?>, ?> ribbonCapabilities();
}
