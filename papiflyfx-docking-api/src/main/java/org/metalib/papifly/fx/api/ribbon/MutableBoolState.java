package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default mutable implementation of {@link BoolState}.
 *
 * <p>Notifications use copy-on-write iteration so listeners can mutate the
 * registration list during dispatch without producing
 * {@link java.util.ConcurrentModificationException}.</p>
 *
 * @since Ribbon 2
 */
public final class MutableBoolState implements BoolState {

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean value;

    /**
     * Creates a state initialised to {@code false}.
     */
    public MutableBoolState() {
        this(false);
    }

    /**
     * Creates a state initialised to the supplied value.
     *
     * @param initialValue initial value
     */
    public MutableBoolState(boolean initialValue) {
        this.value = initialValue;
    }

    @Override
    public boolean get() {
        return value;
    }

    @Override
    public void set(boolean newValue) {
        boolean oldValue = this.value;
        if (oldValue == newValue) {
            return;
        }
        this.value = newValue;
        for (Listener listener : listeners) {
            listener.changed(oldValue, newValue);
        }
    }

    @Override
    public void addListener(Listener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.addIfAbsent(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
    }
}
