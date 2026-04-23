package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;

/**
 * Typed key for metadata carried by a {@link RibbonContext}.
 *
 * <p>Standard host keys are exposed from {@link RibbonContextAttributes}.
 * Feature modules that define their own metadata should use a stable,
 * namespaced identifier such as {@code github.ribbon.repository} or
 * {@code code.editor.language}. The identifier is also the raw string key used
 * by legacy {@link RibbonContext#attribute(String)} lookups, so adding typed
 * keys does not change existing attribute compatibility.</p>
 *
 * @param id stable raw attribute identifier
 * @param type value type expected for the attribute
 * @param <T> value type
 * @since Ribbon 5 Phase 2
 */
public record RibbonAttributeKey<T>(String id, Class<T> type) {

    /**
     * Creates a typed attribute key.
     *
     * @param id stable raw attribute identifier
     * @param type value type expected for the attribute
     */
    public RibbonAttributeKey {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a typed attribute key.
     *
     * @param id stable raw attribute identifier
     * @param type value type expected for the attribute
     * @param <T> value type
     * @return typed attribute key
     */
    public static <T> RibbonAttributeKey<T> of(String id, Class<T> type) {
        return new RibbonAttributeKey<>(id, type);
    }
}
