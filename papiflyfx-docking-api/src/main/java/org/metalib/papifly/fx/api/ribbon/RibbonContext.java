package org.metalib.papifly.fx.api.ribbon;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight runtime context passed to ribbon providers and visibility rules.
 *
 * <p>The context exposes only stable dock/content identity plus an optional
 * attribute bag so feature modules can react to the active surface without
 * depending on concrete dock implementation types.</p>
 *
 * @param activeDockId active dock identifier, or {@code null} when no dock is active
 * @param activeContentId active content identifier, or {@code null} when no content is active
 * @param activeContentTypeKey active content type key, or {@code null} when unknown
 * @param attributes additional host-defined contextual attributes
 */
public record RibbonContext(
    String activeDockId,
    String activeContentId,
    String activeContentTypeKey,
    Map<String, Object> attributes
) {
    private static final RibbonContext EMPTY = new RibbonContext(null, null, null, Map.of());

    /**
     * Creates a ribbon context.
     *
     * @param activeDockId active dock identifier, or {@code null} when no dock is active
     * @param activeContentId active content identifier, or {@code null} when no content is active
     * @param activeContentTypeKey active content type key, or {@code null} when unknown
     * @param attributes additional host-defined contextual attributes
     */
    public RibbonContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(attributes));
    }

    /**
     * Returns an empty ribbon context.
     *
     * @return a context without active dock/content state
     */
    public static RibbonContext empty() {
        return EMPTY;
    }

    /**
     * Returns the active dock identifier when available.
     *
     * @return active dock identifier when present
     */
    public Optional<String> activeDockIdOptional() {
        return Optional.ofNullable(activeDockId);
    }

    /**
     * Returns the active content identifier when available.
     *
     * @return active content identifier when present
     */
    public Optional<String> activeContentIdOptional() {
        return Optional.ofNullable(activeContentId);
    }

    /**
     * Returns the active content type key when available.
     *
     * @return active content type key when present
     */
    public Optional<String> activeContentTypeKeyOptional() {
        return Optional.ofNullable(activeContentTypeKey);
    }

    /**
     * Looks up a contextual attribute.
     *
     * @param key attribute key
     * @return attribute value when present
     */
    public Optional<Object> attribute(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(attributes.get(key));
    }

    /**
     * Looks up a contextual attribute and narrows it to the requested type.
     *
     * @param key attribute key
     * @param type requested value type
     * @param <T> requested value type
     * @return typed attribute value when present and compatible
     */
    public <T> Optional<T> attribute(String key, Class<T> type) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Object value = attributes.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }
}
