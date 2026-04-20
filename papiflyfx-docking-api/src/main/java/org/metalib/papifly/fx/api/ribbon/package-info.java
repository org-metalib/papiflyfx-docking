/**
 * Ribbon contribution SPI for PapiflyFX hosts and feature modules.
 *
 * <p>The package defines a UI-agnostic command model plus tab/group/control
 * descriptors that runtime modules can discover with {@link java.util.ServiceLoader}.
 * Contributing modules register implementations of
 * {@link org.metalib.papifly.fx.api.ribbon.RibbonProvider} in their own
 * {@code META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider}
 * resource.</p>
 */
package org.metalib.papifly.fx.api.ribbon;
