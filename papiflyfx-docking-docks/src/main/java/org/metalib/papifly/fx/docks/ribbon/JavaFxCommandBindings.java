package org.metalib.papifly.fx.docks.ribbon;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import org.metalib.papifly.fx.api.ribbon.BoolState;

import java.util.Objects;

/**
 * JavaFX adapters for {@link BoolState} that let UI-neutral command state
 * participate in JavaFX bindings without exposing JavaFX types in the shared
 * ribbon API.
 *
 * <p>Two flavors are provided:</p>
 * <ul>
 *   <li>{@link #readOnly(BoolState)} — a {@link ReadOnlyBooleanProperty} that
 *   tracks the underlying state and is the right fit for things like
 *   {@code disableProperty().bind(...)}.</li>
 *   <li>{@link #bidirectional(BoolState)} — a {@link BooleanProperty} that
 *   propagates UI-driven changes back into the {@link BoolState}, suited for
 *   toggle controls.</li>
 * </ul>
 *
 * <p>Listeners are dispatched on the JavaFX application thread so the
 * resulting properties are safe to bind to scene-graph nodes.</p>
 *
 * @since Ribbon 2
 */
final class JavaFxCommandBindings {

    private JavaFxCommandBindings() {
    }

    /**
     * Returns a JavaFX read-only property that mirrors the supplied state.
     *
     * @param state UI-neutral state
     * @return JavaFX property tracking the state
     */
    static ReadOnlyBooleanProperty readOnly(BoolState state) {
        Objects.requireNonNull(state, "state");
        ReadOnlyBooleanWrapper wrapper = new ReadOnlyBooleanWrapper(state.get());
        BoolState.Listener listener = (oldValue, newValue) -> runOnFx(() -> wrapper.set(newValue));
        state.addListener(listener);
        return wrapper.getReadOnlyProperty();
    }

    /**
     * Returns a JavaFX property that mirrors the supplied state and writes
     * back into it when the property changes. Re-entrant updates are
     * suppressed so the binding does not loop.
     *
     * @param state UI-neutral state
     * @return JavaFX property bidirectionally bound to the state
     */
    static BooleanProperty bidirectional(BoolState state) {
        Objects.requireNonNull(state, "state");
        SimpleBooleanProperty property = new SimpleBooleanProperty(state.get());
        boolean[] suppress = new boolean[1];
        BoolState.Listener listener = (oldValue, newValue) -> runOnFx(() -> {
            if (suppress[0] || property.get() == newValue) {
                return;
            }
            suppress[0] = true;
            try {
                property.set(newValue);
            } finally {
                suppress[0] = false;
            }
        });
        state.addListener(listener);
        property.addListener((obs, oldValue, newValue) -> {
            if (suppress[0] || state.get() == newValue) {
                return;
            }
            suppress[0] = true;
            try {
                state.set(newValue);
            } finally {
                suppress[0] = false;
            }
        });
        return property;
    }

    private static void runOnFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
