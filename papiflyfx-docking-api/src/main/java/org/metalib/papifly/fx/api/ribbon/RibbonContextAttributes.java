package org.metalib.papifly.fx.api.ribbon;

/**
 * Standard attribute keys contributed by dock hosts through
 * {@link RibbonContext#attributes()}.
 *
 * <p>Providers should prefer these keys when they need host metadata to drive
 * tab visibility or command behavior while remaining decoupled from concrete
 * host implementation types.</p>
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
    public static final String ACTIVE_CONTENT_NODE = "activeContentNode";

    private RibbonContextAttributes() {
    }
}
