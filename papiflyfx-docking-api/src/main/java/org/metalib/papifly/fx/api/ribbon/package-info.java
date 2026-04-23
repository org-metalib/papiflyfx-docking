/**
 * Ribbon contribution SPI for PapiflyFX hosts and feature modules.
 *
 * <p>The package defines a UI-agnostic command model plus tab/group/control
 * descriptors that runtime modules can discover with {@link java.util.ServiceLoader}.
 * Contributing modules register implementations of
 * {@link org.metalib.papifly.fx.api.ribbon.RibbonProvider} in their own
 * {@code META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider}
 * resource.</p>
 *
 * <p>Core contracts:</p>
 * <ul>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.PapiflyCommand}: stable
 *   command metadata, observable {@link org.metalib.papifly.fx.api.ribbon.BoolState}
 *   for enabled/selected, and an execution callback. UI-neutral by design.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.BoolState} and
 *   {@link org.metalib.papifly.fx.api.ribbon.MutableBoolState}: UI-neutral
 *   command state contract that runtime hosts adapt to their toolkit.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonTabSpec}: tab-level
 *   contribution with visibility predicate support.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec}: grouped
 *   controls plus a {@code collapseOrder} that drives deterministic adaptive
 *   layout (smaller values collapse first).</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonControlSpec}: control
 *   descriptors (button, toggle, split, menu) decoupled from JavaFX node
 *   classes.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonContext}: typed
 *   capability registry plus dock/content identity for provider routing.</li>
 * </ul>
 *
 * <p><b>Ribbon 2 contract breaks:</b></p>
 * <ul>
 *   <li>{@code PapiflyCommand} no longer exposes JavaFX
 *   {@code BooleanProperty}; it exposes {@code BoolState} fields named
 *   {@code enabled} and {@code selected}.</li>
 *   <li>{@code RibbonGroupSpec.reductionPriority} has been renamed to
 *   {@code collapseOrder} with an explicit Javadoc-defined ordering rule.</li>
 *   <li>{@code RibbonContext} adds a {@code capabilities} component and a
 *   {@code capability(Class)} accessor; providers should migrate off the
 *   deprecated
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes#ACTIVE_CONTENT_NODE}
 *   attribute.</li>
 * </ul>
 *
 * <p>Provider guidance:</p>
 * <ul>
 *   <li>Use {@link org.metalib.papifly.fx.api.ribbon.RibbonContext#capability(Class)}
 *   for executable integrations and reserve
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonContext#attribute(String)}
 *   lookups for metadata such as dock titles, factory ids, and visibility
 *   hints.</li>
 *   <li>Keep tab, group, control, and command identifiers stable once
 *   published. Hosts persist selected-tab state and Quick Access Toolbar
 *   command identifiers by ID, so changing them breaks session continuity for
 *   that provider-owned surface.</li>
 *   <li>Provider command state should be updated on canonical command
 *   instances returned by the runtime rather than by inventing replacement
 *   identifiers for the same semantic action.</li>
 * </ul>
 */
package org.metalib.papifly.fx.api.ribbon;
