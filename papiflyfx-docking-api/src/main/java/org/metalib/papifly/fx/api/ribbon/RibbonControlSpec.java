package org.metalib.papifly.fx.api.ribbon;

/**
 * Marker interface for UI-agnostic ribbon control descriptors.
 *
 * <p>Hosts interpret these descriptors to render concrete JavaFX ribbon
 * controls without leaking view types into the shared API.</p>
 */
public interface RibbonControlSpec {

    /**
     * Returns the stable control identifier.
     *
     * @return control identifier
     */
    String id();

    /**
     * Returns the stable built-in control kind used by runtime strategies.
     *
     * @return control kind
     * @since Ribbon 6
     */
    RibbonControlKind kind();
}
