package org.metalib.papifly.fx.api.ribbon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight runtime context passed to ribbon providers and visibility rules.
 *
 * <p>The context exposes stable dock/content identity, an attribute bag for
 * string-keyed metadata, and a typed capability registry for action
 * interfaces that the active content exposes. Providers should prefer
 * {@link #capability(Class)} for action dispatch and rely on attributes for
 * presentation metadata only. Typical attribute uses include tab visibility,
 * dock-title heuristics, and content-factory metadata; executable integrations
 * such as {@code GitHubRibbonActions} or {@code HugoRibbonActions} belong in
 * the capability map.</p>
 *
 * <p><b>Ribbon 2 contract break:</b> a new {@code capabilities} component has
 * been added. Constructors that accept the legacy 4-argument form are kept
 * for call-site ergonomics and default {@code capabilities} to an empty map.
 * Provider code that casts the legacy
 * {@link RibbonContextAttributes#ACTIVE_CONTENT_NODE} attribute should migrate
 * to {@link #capability(Class)} so the active content can expose multiple
 * capability interfaces without leaking concrete node types.</p>
 *
 * @param activeDockId active dock identifier, or {@code null} when no dock is active
 * @param activeContentId active content identifier, or {@code null} when no content is active
 * @param activeContentTypeKey active content type key, or {@code null} when unknown
 * @param attributes additional host-defined contextual attributes used for
 *     metadata and visibility decisions
 * @param capabilities typed capability instances contributed by the host for
 *     command routing and other executable integrations
 * @since Ribbon 2
 */
public record RibbonContext(
    String activeDockId,
    String activeContentId,
    String activeContentTypeKey,
    Map<String, Object> attributes,
    Map<Class<?>, Object> capabilities
) {
    private static final RibbonContext EMPTY = new RibbonContext(null, null, null, Map.of(), Map.of());

    /**
     * Creates a ribbon context with typed capabilities.
     *
     * @param activeDockId active dock identifier, or {@code null} when no dock is active
     * @param activeContentId active content identifier, or {@code null} when no content is active
     * @param activeContentTypeKey active content type key, or {@code null} when unknown
     * @param attributes additional host-defined contextual attributes
     * @param capabilities typed capability instances contributed by the host
     */
    public RibbonContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(attributes));
        capabilities = capabilities == null
            ? Collections.emptyMap()
            : Map.copyOf(new LinkedHashMap<>(capabilities));
    }

    /**
     * Creates a ribbon context without explicit capabilities.
     *
     * <p>Equivalent to the canonical constructor with an empty capabilities
     * map. Kept for call-site ergonomics during Ribbon 2 migration.</p>
     *
     * @param activeDockId active dock identifier, or {@code null} when no dock is active
     * @param activeContentId active content identifier, or {@code null} when no content is active
     * @param activeContentTypeKey active content type key, or {@code null} when unknown
     * @param attributes additional host-defined contextual attributes
     */
    public RibbonContext(
        String activeDockId,
        String activeContentId,
        String activeContentTypeKey,
        Map<String, Object> attributes
    ) {
        this(activeDockId, activeContentId, activeContentTypeKey, attributes, Map.of());
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
     * <p>Attributes are intended for metadata rather than executable behavior.
     * Providers should prefer {@link #capability(Class)} when they need to
     * invoke host or content actions.</p>
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
     * <p>This is appropriate for visibility metadata such as dock titles,
     * content-factory identifiers, or persisted content-state hints. It should
     * not be used as a substitute for typed action capabilities.</p>
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

    /**
     * Returns the capability registered under the supplied type, falling back
     * to a linear scan so hosts may register a single active value under a
     * more specific key.
     *
     * <p>Ribbon 2 providers should use this method to obtain typed action
     * interfaces (for example, {@code context.capability(GitHubRibbonActions.class)})
     * instead of casting raw nodes retrieved from
     * {@link RibbonContextAttributes#ACTIVE_CONTENT_NODE}. Hosts may register a
     * concrete active content node under its implementation class and still
     * satisfy provider lookups for the exported action interface.</p>
     *
     * @param type requested capability type
     * @param <T> requested capability type
     * @return capability instance when the host has contributed one
     */
    public <T> Optional<T> capability(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object direct = capabilities.get(type);
        if (type.isInstance(direct)) {
            return Optional.of(type.cast(direct));
        }
        for (Object value : capabilities.values()) {
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a copy of this context with the supplied capability registered.
     *
     * @param type capability type key
     * @param instance capability instance; {@code null} removes the binding
     * @param <T> capability type
     * @return copy of the context with the capability applied
     */
    public <T> RibbonContext withCapability(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        LinkedHashMap<Class<?>, Object> next = new LinkedHashMap<>(capabilities);
        if (instance == null) {
            next.remove(type);
        } else {
            next.put(type, instance);
        }
        return new RibbonContext(activeDockId, activeContentId, activeContentTypeKey, attributes, next);
    }
}
