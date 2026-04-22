package org.metalib.papifly.fx.docks;

import java.util.Map;

/**
 * Codec for a namespaced dock-session extension payload.
 *
 * @param <T> typed extension state
 */
public interface DockSessionExtensionCodec<T> {

    /**
     * Encodes typed extension state into a serializable payload map.
     *
     * @param state typed extension state
     * @return serializable payload map
     */
    Map<String, Object> encode(T state);

    /**
     * Decodes a serializable payload map back into typed extension state.
     *
     * @param payload serialized payload map
     * @return typed extension state, or {@code null} when no restore is needed
     */
    T decode(Map<String, Object> payload);
}
