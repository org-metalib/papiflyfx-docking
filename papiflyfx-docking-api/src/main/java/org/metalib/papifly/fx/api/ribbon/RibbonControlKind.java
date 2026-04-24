package org.metalib.papifly.fx.api.ribbon;

/**
 * Stable built-in ribbon control kinds.
 *
 * <p>The value lets runtime hosts dispatch through strategy registries without
 * exposing JavaFX rendering types through the shared provider API.</p>
 *
 * @since Ribbon 6
 */
public enum RibbonControlKind {
    BUTTON,
    TOGGLE,
    SPLIT_BUTTON,
    MENU,
    UNKNOWN
}
