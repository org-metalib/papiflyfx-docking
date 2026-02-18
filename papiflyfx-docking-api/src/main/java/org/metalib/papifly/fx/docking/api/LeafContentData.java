package org.metalib.papifly.fx.docking.api;

import java.util.Map;

/**
 * DTO representing persisted content identity and state for a dock leaf.
 */
public record LeafContentData(
    String typeKey,
    String contentId,
    int version,
    Map<String, Object> state
) {
    /**
     * Creates a LeafContentData without a state payload.
     */
    public static LeafContentData of(String typeKey, String contentId, int version) {
        return new LeafContentData(typeKey, contentId, version, null);
    }

    /**
     * Creates a LeafContentData with a state payload.
     */
    public static LeafContentData of(String typeKey, String contentId, int version, Map<String, Object> state) {
        return new LeafContentData(typeKey, contentId, version, state);
    }
}
