package org.metalib.papifly.fx.docks;

/**
 * Optional extension hook for enriching dock sessions with module-specific
 * payloads and restoring that payload after layout/session restore.
 *
 * <p>Each contributor owns exactly one stable namespace beneath
 * {@code extensions.<namespace>} in the serialized session payload. The
 * namespace is part of the persisted contract and should remain stable once
 * published so saved sessions continue to round-trip correctly.</p>
 *
 * <p>Contributors own only their namespaced payload. Core dock state remains
 * outside the extension map, and one contributor must not read or mutate
 * another contributor's namespace.</p>
 *
 * @param <T> typed extension state owned by the contributor namespace
 */
public interface DockSessionStateContributor<T> {

    /**
     * Returns the unique namespace used for persistence ownership.
     *
     * <p>The namespace becomes the key under {@code extensions.<namespace>} and
     * must therefore be stable, non-blank, and unique within the hosting
     * {@link DockManager}.</p>
     *
     * @return stable, non-blank namespace
     */
    String extensionNamespace();

    /**
     * Returns the codec used to encode/decode the contributor payload.
     *
     * <p>The codec is responsible only for the contributor-owned payload map.
     * Malformed payloads are isolated to this contributor during restore; they
     * must not abort unrelated core-session or extension restore paths.</p>
     *
     * @return typed extension codec
     */
    DockSessionExtensionCodec<T> codec();

    /**
     * Captures the current contributor-owned extension state.
     *
     * @return typed state to persist; {@code null} removes the namespace from
     *     the captured session so no {@code extensions.<namespace>} entry is
     *     written
     */
    default T captureSessionState() {
        return null;
    }

    /**
     * Restores contributor-owned extension state after the core dock session
     * restore finishes.
     *
     * <p>This callback only runs when the contributor's payload decoded
     * successfully. Decode failures are logged and isolated before this method
     * is invoked.</p>
     *
     * @param sessionState decoded extension state
     */
    default void restoreSessionState(T sessionState) {
    }
}
