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
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonCommand}: stable
 *   action-command metadata, observable enabled state, and an execution
 *   callback. UI-neutral by design.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand}: a
 *   toggle-capable command contract that adds selected state only where a
 *   toggle control needs it.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonBooleanState} and
 *   {@link org.metalib.papifly.fx.api.ribbon.MutableRibbonBooleanState}:
 *   UI-neutral command state contracts that runtime hosts adapt to their
 *   toolkit through disposable subscriptions.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonTabSpec}: tab-level
 *   contribution with visibility predicate support.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec}: grouped
 *   controls plus a {@code collapseOrder} that drives deterministic adaptive
 *   layout (smaller values collapse first).</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonControlSpec}: control
 *   descriptors (button, toggle, split, menu) decoupled from JavaFX node
 *   classes.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonContext}: typed
 *   capability registry plus dock/content identity and metadata for provider
 *   routing.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonAttributeKey}: typed
 *   metadata keys layered over the legacy raw-string attribute map.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonCapabilityContributor}
 *   and {@link org.metalib.papifly.fx.api.ribbon.RibbonAttributeContributor}:
 *   optional active-content contracts for publishing explicit capabilities and
 *   contextual metadata.</li>
 * </ul>
 *
 * <p><b>Ribbon contract breaks:</b></p>
 * <ul>
 *   <li>Ribbon 2 removed JavaFX {@code BooleanProperty} from command state.</li>
 *   <li>{@code RibbonGroupSpec.reductionPriority} has been renamed to
 *   {@code collapseOrder} with an explicit Javadoc-defined ordering rule.</li>
 *   <li>{@code RibbonContext} adds a {@code capabilities} component and a
 *   {@code capability(Class)} accessor; providers should migrate off the
 *   deprecated
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes#ACTIVE_CONTENT_NODE}
 *   attribute.</li>
 *   <li>Ribbon 5 Phase 2 adds typed context metadata and explicit content-side
 *   contribution contracts without removing raw-string attribute access.</li>
 *   <li>Ribbon 6 removes the deprecated {@code PapiflyCommand},
 *   {@code BoolState}, and {@code MutableBoolState} bridge types. Providers use
 *   {@code RibbonCommand}, {@code RibbonToggleCommand}, and
 *   {@code RibbonBooleanState} directly.</li>
 * </ul>
 *
 * <p>Provider guidance:</p>
 * <ul>
 *   <li>Use {@link org.metalib.papifly.fx.api.ribbon.RibbonContext#capability(Class)}
 *   for executable integrations and reserve
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonContext#attribute(RibbonAttributeKey)}
 *   lookups for metadata such as dock titles, factory ids, content kind/domain,
 *   and visibility hints.</li>
 *   <li>Expose active-content actions either by making the root node implement
 *   the action interface or by implementing
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonCapabilityContributor} when
 *   the action object lives in a nested controller. Use
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonAttributeContributor} for
 *   explicit contextual metadata such as
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes#CONTENT_DOMAIN_KEY}
 *   and
 *   {@link org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes#CONTENT_KIND_KEY}.</li>
 *   <li>Keep tab, group, control, and command identifiers stable once
 *   published. Hosts persist selected-tab state and Quick Access Toolbar
 *   command identifiers by ID, so changing them breaks session continuity for
 *   that provider-owned surface. Use a dotted namespace such as
 *   {@code <module>.ribbon.<action>} for command ids.</li>
 *   <li>Duplicate tab ids are merged by the host with first tab label/order
 *   winning; duplicate command ids keep the first metadata surface while
 *   runtime state and action dispatch refresh from later emissions. Runtime
 *   hosts should warn or emit telemetry when first-wins metadata conflicts are
 *   detected.</li>
 *   <li>Provider command state may be recomputed on every
 *   {@code getTabs(context)} call. Hosts canonicalize command identity by id
 *   and project later {@code enabled}/{@code selected}/action snapshots onto
 *   the canonical command.</li>
 * </ul>
 */
package org.metalib.papifly.fx.api.ribbon;
