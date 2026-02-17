package org.metalib.papifly.fx.docks.layout;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.metalib.papifly.fx.docks.core.DockData;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.SplitData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;
import org.metalib.papifly.fx.docks.theme.Theme;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for building DockElement trees from layout DTOs.
 * Recursively traverses the data model to instantiate concrete structural nodes.
 */
public class LayoutFactory {

    private static final Logger LOG = Logger.getLogger(LayoutFactory.class.getName());

    private final ObjectProperty<Theme> themeProperty;
    private ContentFactory contentFactory;
    private ContentStateRegistry contentStateRegistry;

    /**
     * Creates a LayoutFactory with the given theme property and content factory.
     */
    public LayoutFactory(ObjectProperty<Theme> themeProperty, ContentFactory contentFactory) {
        this.themeProperty = themeProperty;
        this.contentFactory = contentFactory;
        this.contentStateRegistry = ContentStateRegistry.fromServiceLoader();
    }

    /**
     * Creates a LayoutFactory with no content factory (content must be set manually).
     */
    public LayoutFactory(ObjectProperty<Theme> themeProperty) {
        this(themeProperty, null);
    }

    /**
     * Updates the content factory used to recreate leaf contents during restore.
     */
    public void setContentFactory(ContentFactory contentFactory) {
        this.contentFactory = contentFactory;
    }

    /**
     * Updates the content state registry used to restore content.
     */
    public void setContentStateRegistry(ContentStateRegistry contentStateRegistry) {
        this.contentStateRegistry = contentStateRegistry;
    }

    /**
     * Gets the content state registry used to restore content.
     */
    public ContentStateRegistry getContentStateRegistry() {
        return contentStateRegistry;
    }

    /**
     * Builds a DockElement tree from the given layout node.
     */
    public DockElement build(LayoutNode node) {
        if (node == null) {
            return null;
        }

        return switch (node) {
            case LeafData leaf -> buildSingleTabGroup(leaf);
            case SplitData split -> buildSplit(split);
            case TabGroupData tabGroup -> buildTabGroup(tabGroup);
        };
    }

    /**
     * Builds a DockLeaf from LeafData.
     * Public access for session restoration.
     */
    public DockLeaf buildLeaf(LeafData data) {
        DockData metadata = DockData.of(data.id(), data.title());
        DockLeaf leaf = new DockLeaf(metadata);
        leaf.setContentFactoryId(data.contentFactoryId());
        LeafContentData contentData = normalizeContentData(data);
        leaf.setContentData(contentData);

        Node content = null;
        if (contentData != null && contentStateRegistry != null) {
            ContentStateAdapter adapter = contentStateRegistry.getAdapter(contentData.typeKey());
            if (adapter != null) {
                try {
                    content = adapter.restore(contentData);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Adapter restore failed for typeKey=" + contentData.typeKey()
                        + ", falling through to factory/placeholder", e);
                    // content remains null -> factory fallback below
                }
            }
            // When adapter is absent, fall through to factory attempt below
        }

        // Create content if factory is available
        if (content == null && contentFactory != null && data.contentFactoryId() != null) {
            content = contentFactory.create(data.contentFactoryId());
        }

        // Placeholder only after both adapter and factory attempts fail
        if (content == null) {
            content = contentData != null
                ? createMissingContentPlaceholder(contentData)
                : createMissingContentPlaceholder(data.id(), data.title());
        }

        if (content != null) {
            leaf.content(content);
        }

        return leaf;
    }

    private LeafContentData normalizeContentData(LeafData data) {
        LeafContentData contentData = data.content();
        if (contentData == null) {
            return null;
        }
        if (contentData.typeKey() == null && data.contentFactoryId() != null) {
            return new LeafContentData(
                data.contentFactoryId(),
                contentData.contentId(),
                contentData.version(),
                contentData.state()
            );
        }
        return contentData;
    }

    private Node createMissingContentPlaceholder(LeafContentData contentData) {
        String typeKey = contentData.typeKey() != null ? contentData.typeKey() : "unknown";
        String contentId = contentData.contentId();
        String labelText = contentId != null
            ? "Missing content: " + typeKey + " (" + contentId + ")"
            : "Missing content: " + typeKey;
        return new Label(labelText);
    }

    private Node createMissingContentPlaceholder(String leafId, String title) {
        String labelText = title != null
            ? "Missing content: " + title + " (" + leafId + ")"
            : "Missing content: " + leafId;
        return new Label(labelText);
    }

    private DockTabGroup buildSingleTabGroup(LeafData data) {
        DockTabGroup tabGroup = new DockTabGroup(themeProperty);
        tabGroup.addLeaf(buildLeaf(data));
        return tabGroup;
    }

    private DockSplitGroup buildSplit(SplitData data) {
        DockSplitGroup split = new DockSplitGroup(
            data.id(),
            data.orientation(),
            data.dividerPosition(),
            themeProperty
        );

        if (data.first() != null) {
            split.setFirst(build(data.first()));
        }
        if (data.second() != null) {
            split.setSecond(build(data.second()));
        }

        return split;
    }

    private DockTabGroup buildTabGroup(TabGroupData data) {
        DockTabGroup tabGroup = new DockTabGroup(data.id(), themeProperty);

        for (LeafData leafData : data.tabs()) {
            DockLeaf leaf = buildLeaf(leafData);
            tabGroup.addLeaf(leaf);
        }

        if (data.activeTabIndex() >= 0 && data.activeTabIndex() < data.tabs().size()) {
            tabGroup.setActiveTab(data.activeTabIndex());
        }

        return tabGroup;
    }

    /**
     * Creates a simple leaf with title and content.
     */
    public DockLeaf createLeaf(String title, Node content) {
        return new DockLeaf()
            .withTitle(title)
            .content(content);
    }

    /**
     * Creates a horizontal split group.
     */
    public DockSplitGroup createHorizontalSplit(DockElement first, DockElement second) {
        return createHorizontalSplit(first, second, 0.5);
    }

    /**
     * Creates a horizontal split group with custom divider position.
     */
    public DockSplitGroup createHorizontalSplit(DockElement first, DockElement second, double dividerPosition) {
        DockSplitGroup split = new DockSplitGroup(Orientation.HORIZONTAL, themeProperty);
        split.setDividerPosition(dividerPosition);
        split.setFirst(first);
        split.setSecond(second);
        return split;
    }

    /**
     * Creates a vertical split group.
     */
    public DockSplitGroup createVerticalSplit(DockElement first, DockElement second) {
        return createVerticalSplit(first, second, 0.5);
    }

    /**
     * Creates a vertical split group with custom divider position.
     */
    public DockSplitGroup createVerticalSplit(DockElement first, DockElement second, double dividerPosition) {
        DockSplitGroup split = new DockSplitGroup(Orientation.VERTICAL, themeProperty);
        split.setDividerPosition(dividerPosition);
        split.setFirst(first);
        split.setSecond(second);
        return split;
    }

    /**
     * Creates a tab group with the given leaves.
     */
    public DockTabGroup createTabGroup(DockLeaf... leaves) {
        DockTabGroup tabGroup = new DockTabGroup(themeProperty);
        for (DockLeaf leaf : leaves) {
            tabGroup.addLeaf(leaf);
        }
        return tabGroup;
    }
}
