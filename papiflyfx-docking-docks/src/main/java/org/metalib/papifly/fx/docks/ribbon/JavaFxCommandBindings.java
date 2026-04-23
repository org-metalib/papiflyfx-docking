package org.metalib.papifly.fx.docks.ribbon;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import org.metalib.papifly.fx.api.ribbon.BoolState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JavaFX adapters for {@link BoolState} that let UI-neutral command state
 * participate in JavaFX bindings without exposing JavaFX types in the shared
 * ribbon API.
 *
 * <p>Two flavors are provided:</p>
 * <ul>
 *   <li>{@link #readOnly(BoolState)} — a disposable wrapper around a
 *   {@link ReadOnlyBooleanProperty} that tracks the underlying state and is the
 *   right fit for things like {@code disableProperty().bind(...)}.</li>
 *   <li>{@link #bidirectional(BoolState)} — a disposable wrapper around a
 *   {@link BooleanProperty} that propagates UI-driven changes back into the
 *   {@link BoolState}, suited for toggle controls.</li>
 * </ul>
 *
 * <p>Listeners are dispatched on the JavaFX application thread so the
 * resulting properties are safe to bind to scene-graph nodes. Callers must
 * close the returned binding, or attach it to a node/menu item through the
 * helper methods in this class, when the rendered control is evicted.</p>
 *
 * @since Ribbon 2
 */
final class JavaFxCommandBindings {

    private static final String SUBSCRIPTIONS_KEY = JavaFxCommandBindings.class.getName() + ".subscriptions";

    private JavaFxCommandBindings() {
    }

    /**
     * Returns a JavaFX read-only property that mirrors the supplied state.
     *
     * @param state UI-neutral state
     * @return disposable JavaFX property tracking the state
     */
    static ReadOnlyBinding readOnly(BoolState state) {
        return new ReadOnlyBinding(state);
    }

    /**
     * Returns a JavaFX property that mirrors the supplied state and writes
     * back into it when the property changes. Re-entrant updates are
     * suppressed so the binding does not loop.
     *
     * @param state UI-neutral state
     * @return disposable JavaFX property bidirectionally bound to the state
     */
    static BidirectionalBinding bidirectional(BoolState state) {
        return new BidirectionalBinding(state);
    }

    static void bindDisabledToNot(ButtonBase buttonBase, BoolState state) {
        Objects.requireNonNull(buttonBase, "buttonBase");
        ReadOnlyBinding binding = readOnly(state);
        buttonBase.disableProperty().bind(binding.property().not());
        attach(buttonBase, () -> {
            buttonBase.disableProperty().unbind();
            binding.close();
        });
    }

    static void bindDisabledToNot(MenuItem menuItem, BoolState state) {
        Objects.requireNonNull(menuItem, "menuItem");
        ReadOnlyBinding binding = readOnly(state);
        menuItem.disableProperty().bind(binding.property().not());
        attach(menuItem, () -> {
            menuItem.disableProperty().unbind();
            binding.close();
        });
    }

    static void bindBidirectional(ToggleButton toggleButton, BoolState state) {
        Objects.requireNonNull(toggleButton, "toggleButton");
        BidirectionalBinding binding = bidirectional(state);
        toggleButton.selectedProperty().bindBidirectional(binding.property());
        attach(toggleButton, () -> {
            toggleButton.selectedProperty().unbindBidirectional(binding.property());
            binding.close();
        });
    }

    static void dispose(Node node) {
        if (node == null) {
            return;
        }
        closeSubscriptions(node.getProperties());
        if (node instanceof MenuButton menuButton) {
            menuButton.getItems().forEach(JavaFxCommandBindings::dispose);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(JavaFxCommandBindings::dispose);
        }
    }

    static void dispose(MenuItem menuItem) {
        if (menuItem == null) {
            return;
        }
        closeSubscriptions(menuItem.getProperties());
        dispose(menuItem.getGraphic());
    }

    static void disposeSubscriptions(Node node) {
        if (node != null) {
            closeSubscriptions(node.getProperties());
        }
    }

    private static void attach(Node node, Subscription subscription) {
        attach(node.getProperties(), subscription);
    }

    private static void attach(MenuItem menuItem, Subscription subscription) {
        attach(menuItem.getProperties(), subscription);
    }

    @SuppressWarnings("unchecked")
    private static void attach(Map<Object, Object> properties, Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        List<Subscription> subscriptions = (List<Subscription>) properties.computeIfAbsent(
            SUBSCRIPTIONS_KEY,
            ignored -> new ArrayList<Subscription>()
        );
        subscriptions.add(subscription);
    }

    @SuppressWarnings("unchecked")
    private static void closeSubscriptions(Map<Object, Object> properties) {
        List<Subscription> subscriptions = (List<Subscription>) properties.remove(SUBSCRIPTIONS_KEY);
        if (subscriptions == null) {
            return;
        }
        subscriptions.forEach(Subscription::close);
        subscriptions.clear();
    }

    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    static final class ReadOnlyBinding implements Subscription {
        private final BoolState state;
        private final ReadOnlyBooleanWrapper wrapper;
        private final BoolState.Listener listener;
        private boolean closed;

        private ReadOnlyBinding(BoolState state) {
            this.state = Objects.requireNonNull(state, "state");
            this.wrapper = new ReadOnlyBooleanWrapper(state.get());
            this.listener = (oldValue, newValue) -> runOnFx(() -> {
                if (!closed) {
                    wrapper.set(newValue);
                }
            });
            this.state.addListener(listener);
        }

        ReadOnlyBooleanProperty property() {
            return wrapper.getReadOnlyProperty();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            state.removeListener(listener);
        }
    }

    static final class BidirectionalBinding implements Subscription {
        private final BoolState state;
        private final SimpleBooleanProperty property;
        private final boolean[] suppress = new boolean[1];
        private final BoolState.Listener stateListener;
        private final ChangeListener<Boolean> propertyListener;
        private boolean closed;

        private BidirectionalBinding(BoolState state) {
            this.state = Objects.requireNonNull(state, "state");
            this.property = new SimpleBooleanProperty(state.get());
            this.stateListener = (oldValue, newValue) -> runOnFx(() -> {
                if (closed || suppress[0] || property.get() == newValue) {
                    return;
                }
                suppress[0] = true;
                try {
                    property.set(newValue);
                } finally {
                    suppress[0] = false;
                }
            });
            this.propertyListener = (obs, oldValue, newValue) -> {
                boolean resolvedNewValue = Boolean.TRUE.equals(newValue);
                if (closed || suppress[0] || state.get() == resolvedNewValue) {
                    return;
                }
                suppress[0] = true;
                try {
                    state.set(resolvedNewValue);
                } finally {
                    suppress[0] = false;
                }
            };
            this.state.addListener(stateListener);
            this.property.addListener(propertyListener);
        }

        BooleanProperty property() {
            return property;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            state.removeListener(stateListener);
            property.removeListener(propertyListener);
        }
    }

    private static void runOnFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
