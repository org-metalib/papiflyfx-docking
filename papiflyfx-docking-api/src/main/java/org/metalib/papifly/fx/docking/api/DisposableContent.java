package org.metalib.papifly.fx.docking.api;

/**
 * Marker interface for content nodes that require explicit disposal when closed.
 * <p>
 * Implementations should stop background workers, unbind listeners,
 * and release caches in {@link #dispose()}.
 */
public interface DisposableContent {

    /**
     * Releases resources held by this content node.
     * Called automatically by the docking framework when the content is closed.
     */
    void dispose();
}
