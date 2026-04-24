package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

final class DefaultRibbonBooleanState implements MutableRibbonBooleanState {

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean value;

    DefaultRibbonBooleanState(boolean initialValue) {
        this.value = initialValue;
    }

    @Override
    public boolean get() {
        return value;
    }

    @Override
    public void set(boolean newValue) {
        boolean oldValue = value;
        if (oldValue == newValue) {
            return;
        }
        value = newValue;
        for (Listener listener : listeners) {
            listener.changed(oldValue, newValue);
        }
    }

    @Override
    public RibbonStateSubscription subscribe(Listener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.addIfAbsent(listener);
        return new RibbonStateSubscription() {
            private boolean closed;

            @Override
            public void close() {
                if (closed) {
                    return;
                }
                closed = true;
                listeners.remove(listener);
            }
        };
    }
}
