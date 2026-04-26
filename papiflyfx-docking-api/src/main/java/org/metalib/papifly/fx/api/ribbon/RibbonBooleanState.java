package org.metalib.papifly.fx.api.ribbon;

/**
 * UI-neutral observable boolean state.
 *
 * <p>This contract is read-only from the consumer side. Runtime hosts observe
 * values through {@link #subscribe(Listener)} and dispose the returned
 * {@link RibbonStateSubscription} when the rendered control is removed. The
 * shared API does not expose JavaFX properties or scene-graph types.</p>
 *
 * @since Ribbon 6
 */
public interface RibbonBooleanState {

    /**
     * Listener invoked when the state value changes.
     */
    @FunctionalInterface
    interface Listener {
        /**
         * Notifies the listener of a value change.
         *
         * @param oldValue value before the change
         * @param newValue value after the change
         */
        void changed(boolean oldValue, boolean newValue);
    }

    /**
     * Returns the current value.
     *
     * @return current value
     */
    boolean get();

    /**
     * Subscribes to value changes.
     *
     * @param listener listener to notify
     * @return disposable subscription
     */
    RibbonStateSubscription subscribe(Listener listener);

    /**
     * Returns an immutable state that always reports the supplied value.
     *
     * @param value fixed value
     * @return immutable state
     */
    static RibbonBooleanState constant(boolean value) {
        return new RibbonBooleanState() {
            @Override
            public boolean get() {
                return value;
            }

            @Override
            public RibbonStateSubscription subscribe(Listener listener) {
                return RibbonStateSubscription.noop();
            }
        };
    }

    /**
     * Creates a mutable state initialised with the supplied value.
     *
     * @param initialValue initial value
     * @return mutable state
     */
    static MutableRibbonBooleanState mutable(boolean initialValue) {
        return new DefaultRibbonBooleanState(initialValue);
    }
}
