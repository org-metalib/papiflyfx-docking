package org.metalib.papifly.fx.api.ribbon;

import java.util.Map;

/**
 * Standard attribute keys contributed by dock hosts through
 * {@link RibbonContext#attributes()}.
 *
 * <p>Providers should prefer these keys when they need host metadata to drive
 * tab visibility or presentation behavior while remaining decoupled from
 * concrete host implementation types. Attributes are the metadata side of the
 * contract; executable integrations belong in
 * {@link RibbonContext#capability(Class)}.</p>
 *
 * <p>The raw string constants are preserved for source and binary
 * compatibility. New providers should prefer the paired
 * {@link RibbonAttributeKey} constants such as {@link #DOCK_TITLE_KEY} because
 * they keep the key id and expected value type together. Provider-owned
 * attributes should use a stable dotted namespace, for example
 * {@code code.editor.language} or {@code github.ribbon.repository}; the
 * unqualified names in this class are reserved for host-defined standard
 * metadata.</p>
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
    public static final String CONTENT_KIND = "contentKind";
    public static final String CONTENT_DOMAIN = "contentDomain";

    public static final RibbonAttributeKey<String> DOCK_TITLE_KEY =
        RibbonAttributeKey.of(DOCK_TITLE, String.class);
    public static final RibbonAttributeKey<String> DOCK_STATE_KEY =
        RibbonAttributeKey.of(DOCK_STATE, String.class);
    public static final RibbonAttributeKey<Boolean> FLOATING_KEY =
        RibbonAttributeKey.of(FLOATING, Boolean.class);
    public static final RibbonAttributeKey<Boolean> MAXIMIZED_KEY =
        RibbonAttributeKey.of(MAXIMIZED, Boolean.class);
    public static final RibbonAttributeKey<String> DOCK_GROUP_ID_KEY =
        RibbonAttributeKey.of(DOCK_GROUP_ID, String.class);
    public static final RibbonAttributeKey<String> CONTENT_FACTORY_ID_KEY =
        RibbonAttributeKey.of(CONTENT_FACTORY_ID, String.class);
    public static final RibbonAttributeKey<Integer> CONTENT_VERSION_KEY =
        RibbonAttributeKey.of(CONTENT_VERSION, Integer.class);
    @SuppressWarnings("rawtypes")
    public static final RibbonAttributeKey<Map> CONTENT_STATE_KEY =
        RibbonAttributeKey.of(CONTENT_STATE, Map.class);
    /**
     * Explicit content kind used by contextual providers before falling back
     * to legacy type-key/title/path heuristics. Examples include
     * {@code markdown}, {@code java}, {@code image}, or a provider-owned value
     * such as {@code hugo.markdown}.
     */
    public static final RibbonAttributeKey<String> CONTENT_KIND_KEY =
        RibbonAttributeKey.of(CONTENT_KIND, String.class);
    /**
     * Explicit content domain used by contextual providers to avoid claiming
     * content owned by another feature module. Examples include {@code hugo},
     * {@code github}, {@code code}, {@code tree}, or {@code media}.
     */
    public static final RibbonAttributeKey<String> CONTENT_DOMAIN_KEY =
        RibbonAttributeKey.of(CONTENT_DOMAIN, String.class);

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
    /**
     * Typed key for {@link #ACTIVE_CONTENT_NODE}.
     *
     * @deprecated Ribbon 2 removes node casting from provider contracts.
     *     Providers should use {@link RibbonContext#capability(Class)} instead.
     */
    @Deprecated(since = "Ribbon 5 Phase 2", forRemoval = true)
    public static final RibbonAttributeKey<Object> ACTIVE_CONTENT_NODE_KEY =
        RibbonAttributeKey.of(ACTIVE_CONTENT_NODE, Object.class);

    private RibbonContextAttributes() {
    }
}
