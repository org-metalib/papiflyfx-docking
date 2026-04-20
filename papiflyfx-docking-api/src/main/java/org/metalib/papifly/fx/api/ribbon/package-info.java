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
 *   command metadata and execution callback.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonTabSpec}: tab-level
 *   contribution with visibility predicate support.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec}: grouped
 *   controls and adaptive reduction priority metadata.</li>
 *   <li>{@link org.metalib.papifly.fx.api.ribbon.RibbonControlSpec}: control
 *   descriptors (`button`, `toggle`, `split`, `menu`) that stay decoupled from
 *   JavaFX node classes.</li>
 * </ul>
 *
 * <p>Persistence guidance: contributors must keep command and tab identifiers
 * stable once published because hosts can persist selected tabs and Quick
 * Access Toolbar command IDs across application restarts.</p>
 */
package org.metalib.papifly.fx.api.ribbon;
