package org.metalib.papifly.fx.docks;

/**
 * Optional extension hook for enriching dock sessions with module-specific
 * payloads and restoring that payload after layout/session restore.
 *
 * @param <T> typed extension state owned by the contributor namespace
 */
public interface DockSessionStateContributor<T> {

    /**
     * Returns the unique namespace used for persistence ownership.
     *
     * @return stable, non-blank namespace
     */
    String extensionNamespace();

    /**
     * Returns the codec used to encode/decode the contributor payload.
     *
     * @return typed extension codec
     */
    DockSessionExtensionCodec<T> codec();

    /**
     * Captures the current contributor-owned extension state.
     *
     * @return typed state to persist; {@code null} removes the namespace from
     *     the captured session
     */
    default T captureSessionState() {
        return null;
    }

    /**
     * Restores contributor-owned extension state after the core dock session
     * restore finishes.
     *
     * @param sessionState decoded extension state
     */
    default void restoreSessionState(T sessionState) {
    }
}
