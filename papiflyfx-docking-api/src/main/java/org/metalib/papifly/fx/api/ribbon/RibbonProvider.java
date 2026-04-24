package org.metalib.papifly.fx.api.ribbon;

import java.util.List;

/**
 * ServiceLoader SPI for modules that contribute ribbon tabs.
 *
 * <p>Hosts discover providers with {@link java.util.ServiceLoader} and merge
 * the returned tab descriptors into the active ribbon model. Contributing
 * modules register provider implementations in
 * {@code META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider}.
 * Keep {@link RibbonTabSpec#id()} and {@link RibbonCommand#id()} values
 * stable because hosts may persist selected tabs and Quick Access Toolbar
 * command sets by identifier.</p>
 *
 * <p>When multiple providers contribute the same tab identifier, hosts merge
 * the groups into the first provider's tab metadata: the first label/order
 * wins, contextual styling is ORed across contributions, and runtimes should
 * emit diagnostics for conflicting label/order metadata. Command identifiers
 * follow the same first-metadata-wins rule for labels, icons, and tooltips
 * while runtime {@link RibbonBooleanState} values and action dispatch are refreshed
 * from the latest provider emission. Prefer a dotted namespace such as
 * {@code <module>.ribbon.<action>} for command ids.</p>
 *
 * <p>Providers should fail closed. Runtime hosts are expected to isolate
 * {@link #getTabs(RibbonContext)} failures so one broken provider does not
 * remove healthy provider tabs, and to surface the failure through logging or
 * telemetry.</p>
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
     * @return contributed tab descriptors, never {@code null}; throw a runtime
     *     exception only for unrecoverable provider defects
     */
    List<RibbonTabSpec> getTabs(RibbonContext context);
}
