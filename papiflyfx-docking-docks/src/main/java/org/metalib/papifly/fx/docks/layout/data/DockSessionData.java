package org.metalib.papifly.fx.docks.layout.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * DTO representing a complete dock session including the layout tree,
 * floating windows, minimized leaves, maximized state, and namespaced
 * extension payloads.
 *
 * @param version session schema version
 * @param layout serialized docked layout tree
 * @param floating serialized floating leaves
 * @param minimized serialized minimized leaves
 * @param maximized serialized maximized leaf state
 * @param extensions namespaced extension payloads keyed by owner namespace
 */
public record DockSessionData(
    int version,
    LayoutNode layout,
    List<FloatingLeafData> floating,
    List<MinimizedLeafData> minimized,
    MaximizedLeafData maximized,
    Map<String, Map<String, Object>> extensions
) {
    public static final int CURRENT_VERSION = 3;

    public DockSessionData {
        floating = List.copyOf(Objects.requireNonNullElse(floating, List.<FloatingLeafData>of()));
        minimized = List.copyOf(Objects.requireNonNullElse(minimized, List.<MinimizedLeafData>of()));
        extensions = copyExtensions(extensions);
    }

    /**
     * Creates a DockSessionData with current version.
     *
     * @param layout serialized docked layout tree
     * @param floating serialized floating leaves
     * @param minimized serialized minimized leaves
     * @param maximized serialized maximized leaf state
     * @return session data using {@link #CURRENT_VERSION}
     */
    public static DockSessionData of(
        LayoutNode layout,
        List<FloatingLeafData> floating,
        List<MinimizedLeafData> minimized,
        MaximizedLeafData maximized
    ) {
        return of(layout, floating, minimized, maximized, Map.of());
    }

    /**
     * Creates a DockSessionData with current version and optional extension
     * payloads.
     *
     * @param layout serialized docked layout tree
     * @param floating serialized floating leaves
     * @param minimized serialized minimized leaves
     * @param maximized serialized maximized leaf state
     * @param extensions namespaced extension payloads
     * @return session data using {@link #CURRENT_VERSION}
     */
    public static DockSessionData of(
        LayoutNode layout,
        List<FloatingLeafData> floating,
        List<MinimizedLeafData> minimized,
        MaximizedLeafData maximized,
        Map<String, Map<String, Object>> extensions
    ) {
        return new DockSessionData(CURRENT_VERSION, layout, floating, minimized, maximized, extensions);
    }

    /**
     * Creates an empty session.
     *
     * @return empty session data using {@link #CURRENT_VERSION}
     */
    public static DockSessionData empty() {
        return new DockSessionData(CURRENT_VERSION, null, List.of(), List.of(), null, Map.of());
    }

    /**
     * Returns the payload for the given namespace.
     *
     * @param namespace extension namespace
     * @return copied extension payload, or {@code null} when absent
     */
    public Map<String, Object> extension(String namespace) {
        return extensions.get(normalizeNamespace(namespace));
    }

    /**
     * Returns a copy with the supplied extension payload.
     *
     * @param namespace extension namespace
     * @param payload extension payload; {@code null} removes the namespace
     * @return copied session state
     */
    public DockSessionData withExtension(String namespace, Map<String, Object> payload) {
        String normalizedNamespace = normalizeNamespace(namespace);
        LinkedHashMap<String, Map<String, Object>> updated = new LinkedHashMap<>(extensions);
        if (payload == null) {
            updated.remove(normalizedNamespace);
        } else {
            updated.put(normalizedNamespace, copyExtensionPayload(payload, normalizedNamespace));
        }
        return new DockSessionData(version, layout, floating, minimized, maximized, updated);
    }

    /**
     * Returns a copy without the supplied extension payload.
     *
     * @param namespace extension namespace
     * @return copied session state
     */
    public DockSessionData withoutExtension(String namespace) {
        return withExtension(namespace, null);
    }

    private static Map<String, Map<String, Object>> copyExtensions(Map<String, Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, Map<String, Object>> sorted = new TreeMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : source.entrySet()) {
            String namespace = normalizeNamespace(entry.getKey());
            Map<String, Object> previous = sorted.put(namespace, copyExtensionPayload(entry.getValue(), namespace));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate extension namespace: " + namespace);
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static Map<String, Object> copyExtensionPayload(Map<String, Object> payload, String namespace) {
        Objects.requireNonNull(payload, "Extension payload must not be null for namespace " + namespace);
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                    "Extension payload keys must be non-blank for namespace " + namespace
                );
            }
            copy.put(key, entry.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String normalizeNamespace(String namespace) {
        if (namespace == null) {
            throw new IllegalArgumentException("Extension namespace must not be null");
        }
        String normalized = namespace.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Extension namespace must not be blank");
        }
        return normalized;
    }
}
