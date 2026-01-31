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
     */
    public static Leaf create() {
        return new Leaf();
    }

    /**
     * Sets the title.
     */
    public Leaf withTitle(String title) {
        leaf.withTitle(title);
        return this;
    }

    /**
     * Sets the icon.
     */
    public Leaf withIcon(Node icon) {
        leaf.withIcon(icon);
        return this;
    }

    /**
     * Sets the content factory identifier used for serialization/restore.
     */
    public Leaf withContentFactoryId(String factoryId) {
        leaf.setContentFactoryId(factoryId);
        return this;
    }

    /**
     * Sets the content.
     */
    public Leaf content(Node content) {
        leaf.content(content);
        return this;
    }

    /**
     * Sets the close handler.
     */
    public Leaf onClose(Consumer<DockLeaf> handler) {
        leaf.onClose(handler);
        return this;
    }

    /**
     * Builds the DockLeaf.
     */
    public DockLeaf build() {
        return leaf;
    }

    /**
     * Convenience factory for creating a titled leaf.
     */
    public static DockLeaf createWithTitle(String title) {
        return new DockLeaf().withTitle(title);
    }

    /**
     * Convenience factory for creating a leaf with title and content.
     */
    public static DockLeaf createWithContent(String title, Node content) {
        return new DockLeaf().withTitle(title).content(content);
    }
}
