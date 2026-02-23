package org.metalib.papifly.fx.docks.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafData;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Terminal node containing user content.
 * The DockLeaf is the content model and is rendered by a DockTabGroup.
 */
public class DockLeaf {

    private final ObjectProperty<DockData> metadata;
    private final ObjectProperty<Node> content;
    private DockTabGroup parent;
    private Consumer<DockLeaf> onCloseRequest;
    private String contentFactoryId;
    private LeafContentData contentData;

    /**
     * Creates a new DockLeaf with default metadata.
     */
    public DockLeaf() {
        this.metadata = new SimpleObjectProperty<>(DockData.of(UUID.randomUUID().toString(), "Untitled"));
        this.content = new SimpleObjectProperty<>();
    }

    /**
     * Creates a new DockLeaf with metadata.
     *
     * @param data metadata for the new leaf
     */
    public DockLeaf(DockData data) {
        this.metadata = new SimpleObjectProperty<>(data);
        this.content = new SimpleObjectProperty<>();
    }

    /**
     * Builder method to set the title.
     *
     * @param title new leaf title
     * @return this leaf instance
     */
    public DockLeaf withTitle(String title) {
        metadata.set(metadata.get().withTitle(title));
        return this;
    }

    /**
     * Builder method to set the icon.
     *
     * @param icon new leaf icon
     * @return this leaf instance
     */
    public DockLeaf withIcon(Node icon) {
        metadata.set(metadata.get().withIcon(icon));
        return this;
    }

    /**
     * Builder method to set the content factory identifier used for rebuild/serialization.
     *
     * @param factoryId content factory identifier
     * @return this leaf instance
     */
    public DockLeaf withContentFactoryId(String factoryId) {
        this.contentFactoryId = factoryId;
        return this;
    }

    /**
     * Sets the content factory identifier used to recreate this leaf's content.
     *
     * @param factoryId content factory identifier
     */
    public void setContentFactoryId(String factoryId) {
        this.contentFactoryId = factoryId;
    }

    /**
     * Gets the content factory identifier used to recreate this leaf's content.
     *
     * @return content factory identifier
     */
    public String getContentFactoryId() {
        return contentFactoryId;
    }

    /**
     * Sets the persisted content data for this leaf.
     *
     * @param contentData persisted content identity and state
     */
    public void setContentData(LeafContentData contentData) {
        this.contentData = contentData;
    }

    /**
     * Gets the persisted content data for this leaf.
     *
     * @return persisted content identity and state
     */
    public LeafContentData getContentData() {
        return contentData;
    }

    /**
     * Builder method to set the content.
     *
     * @param node content node
     * @return this leaf instance
     */
    public DockLeaf content(Node node) {
        content.set(node);
        return this;
    }

    /**
     * Sets the close request handler.
     *
     * @param handler close callback invoked on {@link #requestClose()}
     * @return this leaf instance
     */
    public DockLeaf onClose(Consumer<DockLeaf> handler) {
        this.onCloseRequest = handler;
        return this;
    }

    /**
     * Requests to close this leaf.
     */
    public void requestClose() {
        if (onCloseRequest != null) {
            onCloseRequest.accept(this);
        }
    }

    /**
     * Gets the content property.
     *
     * @return observable content property
     */
    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    /**
     * Gets the content node.
     *
     * @return current content node
     */
    public Node getContent() {
        return content.get();
    }

    /**
     * Gets the metadata property.
     *
     * @return observable metadata property
     */
    public ObjectProperty<DockData> metadataProperty() {
        return metadata;
    }

    /**
     * Gets the current metadata value.
     *
     * @return current leaf metadata
     */
    public DockData getMetadata() {
        return metadata.get();
    }

    /**
     * Serializes this leaf into layout data.
     *
     * @return serialized leaf layout node
     */
    public LayoutNode serialize() {
        DockData data = metadata.get();
        return LeafData.of(data.id(), data.title(), contentFactoryId, contentData);
    }

    /**
     * Disposes this leaf and releases content resources.
     */
    public void dispose() {
        Node node = content.get();
        if (node instanceof DisposableContent disposable) {
            disposable.dispose();
        }
        content.unbind();
        content.set(null);
    }

    /**
     * Gets the parent tab group.
     *
     * @return parent tab group, or {@code null} when detached
     */
    public DockTabGroup getParent() {
        return parent;
    }

    /**
     * Sets the parent tab group.
     *
     * @param parent parent tab group, or {@code null} when detached
     */
    public void setParent(DockTabGroup parent) {
        this.parent = parent;
    }
}
