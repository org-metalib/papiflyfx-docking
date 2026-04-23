package org.metalib.papifly.fx.docks;

import java.util.Map;

/**
 * Codec for a namespaced dock-session extension payload.
 *
 * <p>Implementations translate between contributor-owned typed state and the
 * serializable map written beneath {@code extensions.<namespace>}. Codecs must
 * not depend on unrelated session fields and should validate only their own
 * payload shape.</p>
 *
 * <p>Decode failures are isolated to the owning contributor namespace by the
 * session runtime. Implementations should throw an
 * {@link IllegalArgumentException} with a precise field path when the payload
 * is malformed.</p>
 *
 * @param <T> typed extension state
 */
public interface DockSessionExtensionCodec<T> {

    /**
     * Encodes typed extension state into a serializable payload map.
     *
     * @param state typed extension state
     * @return serializable payload map for the contributor-owned namespace;
     *     never {@code null}
     */
    Map<String, Object> encode(T state);

    /**
     * Decodes a serializable payload map back into typed extension state.
     *
     * @param payload serialized payload map
     * @return typed extension state, or {@code null} when no restore is needed
     * @throws IllegalArgumentException when the payload is malformed for the
     *     owning namespace
     */
    T decode(Map<String, Object> payload);
}
