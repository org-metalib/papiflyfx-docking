package org.metalib.papifly.fx.api.ribbon;

/**
 * Disposable handle returned by ribbon state subscriptions.
 *
 * <p>The contract intentionally has a no-throws {@link #close()} method so
 * providers and runtime hosts can use it in UI cleanup paths without wrapping
 * checked exceptions.</p>
 *
 * @since Ribbon 6
 */
@FunctionalInterface
public interface RibbonStateSubscription extends AutoCloseable {

    /**
     * Stops receiving updates for the associated subscription. Calling this
     * method more than once must be safe.
     */
    @Override
    void close();

    /**
     * Returns a subscription that does nothing when closed.
     *
     * @return inert subscription
     */
    static RibbonStateSubscription noop() {
        return () -> {
            // no-op
        };
    }
}
