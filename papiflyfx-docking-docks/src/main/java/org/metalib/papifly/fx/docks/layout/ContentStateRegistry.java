package org.metalib.papifly.fx.docks.layout;

import org.metalib.papifly.fx.docking.api.ContentStateAdapter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry for content state adapters keyed by type.
 */
public class ContentStateRegistry {

    private final Map<String, ContentStateAdapter> adapters = new LinkedHashMap<>();

    /**
     * Creates an empty registry.
     */
    public ContentStateRegistry() {
    }

    /**
     * Loads adapters using {@link ServiceLoader}.
     */
    public static ContentStateRegistry fromServiceLoader() {
        ContentStateRegistry registry = new ContentStateRegistry();
        ServiceLoader.load(ContentStateAdapter.class).forEach(registry::register);
        return registry;
    }

    /**
     * Registers an adapter using its type key.
     */
    public void register(ContentStateAdapter adapter) {
        if (adapter == null || adapter.getTypeKey() == null) {
            return;
        }
        adapters.put(adapter.getTypeKey(), adapter);
    }

    /**
     * Gets an adapter by type key.
     */
    public ContentStateAdapter getAdapter(String typeKey) {
        if (typeKey == null) {
            return null;
        }
        return adapters.get(typeKey);
    }

    /**
     * Returns true when no adapters are registered.
     */
    public boolean isEmpty() {
        return adapters.isEmpty();
    }
}
