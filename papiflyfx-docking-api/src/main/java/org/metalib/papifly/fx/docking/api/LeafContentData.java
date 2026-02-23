package org.metalib.papifly.fx.docking.api;

import java.util.Map;

/**
 * DTO representing persisted content identity and state for a dock leaf.
 *
 * @param typeKey stable content type key used to resolve a content adapter
 * @param contentId stable identifier of the content instance
 * @param version schema version for the serialized content state
 * @param state serialized content state map, or {@code null} when no state is stored
 */
public record LeafContentData(
    String typeKey,
    String contentId,
    int version,
    Map<String, Object> state
) {
    /**
     * Creates a LeafContentData without a state payload.
     *
     * @param typeKey stable content type key used to resolve a content adapter
     * @param contentId stable identifier of the content instance
     * @param version schema version for the serialized content state
     * @return a content data instance with a {@code null} state payload
     */
    public static LeafContentData of(String typeKey, String contentId, int version) {
        return new LeafContentData(typeKey, contentId, version, null);
    }

    /**
     * Creates a LeafContentData with a state payload.
     *
     * @param typeKey stable content type key used to resolve a content adapter
     * @param contentId stable identifier of the content instance
     * @param version schema version for the serialized content state
     * @param state serialized content state map, or {@code null} when no state is stored
     * @return a content data instance with the provided state payload
     */
    public static LeafContentData of(String typeKey, String contentId, int version, Map<String, Object> state) {
        return new LeafContentData(typeKey, contentId, version, state);
    }
}
