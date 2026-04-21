package org.metalib.papifly.fx.api.ribbon;

/**
 * UI-agnostic mutable boolean state with change notification.
 *
 * <p>Ribbon 2 introduces this contract so that command state ({@code enabled},
 * {@code selected}, ...) can be expressed at the SPI boundary without leaking
 * JavaFX property types into shared modules. Runtime hosts adapt
 * {@link BoolState} to whichever UI toolkit they render with.</p>
 *
 * <p>Implementations must be safe to read from any thread; writes and
 * notifications are expected to occur on the host's UI thread.</p>
 *
 * @see MutableBoolState
 * @since Ribbon 2
 */
public interface BoolState {

    /**
     * Listener invoked when {@link BoolState#set(boolean)} produces a change.
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
     * Updates the current value, notifying listeners on change.
     *
     * @param value new value
     */
    void set(boolean value);

    /**
     * Registers a change listener. Re-registering the same listener has no
     * effect; ordering across listeners is not guaranteed.
     *
     * @param listener listener to register
     */
    void addListener(Listener listener);

    /**
     * Unregisters a previously registered change listener. Unknown listeners
     * are ignored.
     *
     * @param listener listener to remove
     */
    void removeListener(Listener listener);

    /**
     * Returns an immutable view that always reports the supplied value and
     * silently ignores writes.
     *
     * @param value fixed value reported by the state
     * @return immutable {@code BoolState}
     */
    static BoolState constant(boolean value) {
        return new BoolState() {
            @Override
            public boolean get() {
                return value;
            }

            @Override
            public void set(boolean ignored) {
                // immutable
            }

            @Override
            public void addListener(Listener listener) {
                // value never changes; nothing to notify
            }

            @Override
            public void removeListener(Listener listener) {
                // no-op
            }
        };
    }
}
