package org.metalib.papifly.fx.api.ribbon;

/**
 * Mutable ribbon boolean state.
 *
 * <p>Provider-owned command state should expose this type only where mutation
 * is intentionally part of the provider implementation. Command consumers
 * should depend on {@link RibbonBooleanState}.</p>
 *
 * @since Ribbon 6
 */
public interface MutableRibbonBooleanState extends RibbonBooleanState {

    /**
     * Updates the current value, notifying subscribers on change.
     *
     * @param value new value
     */
    void set(boolean value);
}
