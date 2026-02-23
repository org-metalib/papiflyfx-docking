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
     *
     * @return registry initialized from available {@link ContentStateAdapter} services
     */
    public static ContentStateRegistry fromServiceLoader() {
        ContentStateRegistry registry = new ContentStateRegistry();
        ServiceLoader.load(ContentStateAdapter.class).forEach(registry::register);
        return registry;
    }

    /**
     * Registers an adapter using its type key.
     *
     * @param adapter adapter to register
     */
    public void register(ContentStateAdapter adapter) {
        if (adapter == null || adapter.getTypeKey() == null) {
            return;
        }
        adapters.put(adapter.getTypeKey(), adapter);
    }

    /**
     * Gets an adapter by type key.
     *
     * @param typeKey adapter type key
     * @return matching adapter, or {@code null} when none is registered
     */
    public ContentStateAdapter getAdapter(String typeKey) {
        if (typeKey == null) {
            return null;
        }
        return adapters.get(typeKey);
    }

    /**
     * Returns true when no adapters are registered.
     *
     * @return {@code true} when registry has no adapters
     */
    public boolean isEmpty() {
        return adapters.isEmpty();
    }
}
