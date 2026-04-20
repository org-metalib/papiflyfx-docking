package org.metalib.papifly.fx.api.ribbon;

import java.util.List;

/**
 * ServiceLoader SPI for modules that contribute ribbon tabs.
 *
 * <p>Hosts discover providers with {@link java.util.ServiceLoader} and merge
 * the returned tab descriptors into the active ribbon model. Contributing
 * modules register provider implementations in
 * {@code META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider}.</p>
 */
public interface RibbonProvider {

    /**
     * Returns a stable provider identifier.
     *
     * @return provider identifier, defaulting to the implementation class name
     */
    default String id() {
        return getClass().getName();
    }

    /**
     * Returns provider ordering relative to other providers.
     *
     * @return provider order; lower values sort first
     */
    default int order() {
        return 0;
    }

    /**
     * Returns the tabs contributed by this provider for the supplied runtime
     * context.
     *
     * @param context current ribbon context
     * @return contributed tab descriptors, never {@code null}
     */
    List<RibbonTabSpec> getTabs(RibbonContext context);
}
