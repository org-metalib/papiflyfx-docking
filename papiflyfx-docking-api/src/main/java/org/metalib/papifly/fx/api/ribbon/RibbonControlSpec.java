package org.metalib.papifly.fx.api.ribbon;

/**
 * Marker interface for UI-agnostic ribbon control descriptors.
 *
 * <p>Hosts interpret these descriptors to render concrete JavaFX ribbon
 * controls without leaking view types into the shared API.</p>
 */
public sealed interface RibbonControlSpec
    permits RibbonButtonSpec, RibbonToggleSpec, RibbonSplitButtonSpec, RibbonMenuSpec {

    /**
     * Returns the stable control identifier.
     *
     * @return control identifier
     */
    String id();
}
