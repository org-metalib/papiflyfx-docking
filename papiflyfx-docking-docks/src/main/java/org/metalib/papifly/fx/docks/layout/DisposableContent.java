package org.metalib.papifly.fx.docks.layout;

/**
 * Marker interface for content nodes that require explicit disposal
 * when their hosting {@link org.metalib.papifly.fx.docks.core.DockLeaf} is closed.
 * <p>
 * Implementations should stop background workers, unbind listeners,
 * and release caches in {@link #dispose()}.
 */
public interface DisposableContent {

    /**
     * Releases resources held by this content node.
     * Called automatically by the docking framework when the leaf is disposed.
     */
    void dispose();
}
