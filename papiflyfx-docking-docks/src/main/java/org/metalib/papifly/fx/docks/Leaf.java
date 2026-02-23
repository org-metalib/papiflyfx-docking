package org.metalib.papifly.fx.docks;

import javafx.scene.Node;
import org.metalib.papifly.fx.docks.core.DockLeaf;

import java.util.function.Consumer;

/**
 * Fluent builder for creating DockLeaf instances.
 */
public final class Leaf {

    private final DockLeaf leaf;

    private Leaf() {
        this.leaf = new DockLeaf();
    }

    /**
     * Creates a new Leaf builder.
     *
     * @return new leaf builder
     */
    public static Leaf create() {
        return new Leaf();
    }

    /**
     * Sets the title.
     *
     * @param title leaf title
     * @return this builder
     */
    public Leaf withTitle(String title) {
        leaf.withTitle(title);
        return this;
    }

    /**
     * Sets the icon.
     *
     * @param icon leaf icon
     * @return this builder
     */
    public Leaf withIcon(Node icon) {
        leaf.withIcon(icon);
        return this;
    }

    /**
     * Sets the content factory identifier used for serialization/restore.
     *
     * @param factoryId content factory identifier
     * @return this builder
     */
    public Leaf withContentFactoryId(String factoryId) {
        leaf.setContentFactoryId(factoryId);
        return this;
    }

    /**
     * Sets the content.
     *
     * @param content content node
     * @return this builder
     */
    public Leaf content(Node content) {
        leaf.content(content);
        return this;
    }

    /**
     * Sets the close handler.
     *
     * @param handler close callback
     * @return this builder
     */
    public Leaf onClose(Consumer<DockLeaf> handler) {
        leaf.onClose(handler);
        return this;
    }

    /**
     * Builds the DockLeaf.
     *
     * @return built dock leaf
     */
    public DockLeaf build() {
        return leaf;
    }

    /**
     * Convenience factory for creating a titled leaf.
     *
     * @param title leaf title
     * @return created dock leaf
     */
    public static DockLeaf createWithTitle(String title) {
        return new DockLeaf().withTitle(title);
    }

    /**
     * Convenience factory for creating a leaf with title and content.
     *
     * @param title leaf title
     * @param content leaf content node
     * @return created dock leaf
     */
    public static DockLeaf createWithContent(String title, Node content) {
        return new DockLeaf().withTitle(title).content(content);
    }
}
