package org.metalib.papifly.fx.docks.ribbon;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.MutableRibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonStateSubscription;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RibbonCommandBindingLifecycleTest {

    @Test
    void readOnlyBindingsCanBeDisposedAcrossRefreshCycles() {
        CountingBooleanState state = new CountingBooleanState(false);

        for (int cycle = 0; cycle < 25; cycle++) {
            JavaFxCommandBindings.ReadOnlyBinding binding = JavaFxCommandBindings.readOnly(state);
            assertEquals(1, state.listenersSize());

            binding.close();
            assertEquals(0, state.listenersSize());
        }
    }

    @Test
    void bidirectionalBindingsCanBeDisposedAcrossRefreshCycles() {
        CountingBooleanState state = new CountingBooleanState(false);

        for (int cycle = 0; cycle < 25; cycle++) {
            JavaFxCommandBindings.BidirectionalBinding binding = JavaFxCommandBindings.bidirectional(state);
            assertEquals(1, state.listenersSize());

            binding.property().set(true);
            assertTrue(state.get());

            binding.close();
            assertEquals(0, state.listenersSize());
            binding.property().set(false);
            assertTrue(state.get(), "closed binding must not write back into command state");
        }
    }

    private static final class CountingBooleanState implements MutableRibbonBooleanState {
        private final List<Listener> listeners = new ArrayList<>();
        private boolean value;

        private CountingBooleanState(boolean value) {
            this.value = value;
        }

        @Override
        public boolean get() {
            return value;
        }

        @Override
        public void set(boolean value) {
            boolean oldValue = this.value;
            if (oldValue == value) {
                return;
            }
            this.value = value;
            List.copyOf(listeners).forEach(listener -> listener.changed(oldValue, value));
        }

        @Override
        public RibbonStateSubscription subscribe(RibbonBooleanState.Listener listener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
            return () -> listeners.remove(listener);
        }

        int listenersSize() {
            return listeners.size();
        }
    }
}
