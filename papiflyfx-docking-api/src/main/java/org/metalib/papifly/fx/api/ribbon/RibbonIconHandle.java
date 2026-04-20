package org.metalib.papifly.fx.api.ribbon;

import java.util.Objects;

/**
 * UI-agnostic handle for an icon resource used by ribbon commands and controls.
 *
 * <p>The handle intentionally stores only a stable resource path. Runtime
 * modules resolve the path into concrete JavaFX images when rendering the
 * ribbon.</p>
 *
 * @param resourcePath classpath-relative or host-defined resource path
 */
public record RibbonIconHandle(String resourcePath) {

    /**
     * Creates an icon handle.
     *
     * @param resourcePath classpath-relative or host-defined resource path
     */
    public RibbonIconHandle {
        Objects.requireNonNull(resourcePath, "resourcePath");
    }

    /**
     * Creates an icon handle for the supplied resource path.
     *
     * @param resourcePath classpath-relative or host-defined resource path
     * @return a new icon handle
     */
    public static RibbonIconHandle of(String resourcePath) {
        return new RibbonIconHandle(resourcePath);
    }
}
