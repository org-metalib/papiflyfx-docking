package org.metalib.papifly.fx.api.ribbon;

/**
 * Standard attribute keys contributed by dock hosts through
 * {@link RibbonContext#attributes()}.
 *
 * <p>Providers should prefer these keys when they need host metadata to drive
 * tab visibility or command behavior while remaining decoupled from concrete
 * host implementation types.</p>
 *
 * <p><b>Ribbon 2 note:</b> {@link #ACTIVE_CONTENT_NODE} is preserved as a
 * presentation-time bridge only. Providers that previously cast this value
 * to a concrete content node or action interface must migrate to
 * {@link RibbonContext#capability(Class)} which returns a typed capability
 * without leaking UI node types.</p>
 */
public final class RibbonContextAttributes {

    public static final String DOCK_TITLE = "dockTitle";
    public static final String DOCK_STATE = "dockState";
    public static final String FLOATING = "floating";
    public static final String MAXIMIZED = "maximized";
    public static final String DOCK_GROUP_ID = "dockGroupId";
    public static final String CONTENT_FACTORY_ID = "contentFactoryId";
    public static final String CONTENT_VERSION = "contentVersion";
    public static final String CONTENT_STATE = "contentState";

    /**
     * Attribute key previously used to smuggle the active content node into
     * provider code so that providers could cast it to an action interface.
     *
     * @deprecated Ribbon 2 removes node casting from provider contracts.
     *     Providers should use {@link RibbonContext#capability(Class)} instead.
     *     This key remains populated by the docks runtime for a transitional
     *     window and will be removed in a future iteration.
     */
    @Deprecated(since = "Ribbon 2", forRemoval = true)
    public static final String ACTIVE_CONTENT_NODE = "activeContentNode";

    private RibbonContextAttributes() {
    }
}
