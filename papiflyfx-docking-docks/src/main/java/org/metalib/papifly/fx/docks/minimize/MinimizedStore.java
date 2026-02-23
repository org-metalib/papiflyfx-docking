package org.metalib.papifly.fx.docks.minimize;

import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.drag.DropZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Stores minimized leaves and their restore hints.
 * Provides precise restore functionality to return leaves to their original positions.
 */
public class MinimizedStore {

    private final Map<String, DockLeaf> minimizedLeaves;
    private final Map<String, RestoreHint> restoreHints;
    private final List<DockLeaf> orderedLeaves;

    private Consumer<DockLeaf> onLeafAdded;
    private Consumer<DockLeaf> onLeafRemoved;

    /**
     * Creates an empty minimized-leaf store.
     */
    public MinimizedStore() {
        this.minimizedLeaves = new LinkedHashMap<>();
        this.restoreHints = new HashMap<>();
        this.orderedLeaves = new ArrayList<>();
    }

    /**
     * Adds a leaf to the minimized store with a restore hint.
     *
     * @param leaf The leaf to minimize
     * @param hint The restore hint for later restoration
     */
    public void addLeaf(DockLeaf leaf, RestoreHint hint) {
        String leafId = leaf.getMetadata().id();
        minimizedLeaves.put(leafId, leaf);
        restoreHints.put(leafId, hint);
        orderedLeaves.add(leaf);

        if (onLeafAdded != null) {
            onLeafAdded.accept(leaf);
        }
    }

    /**
     * Removes a leaf from the minimized store.
     *
     * @param leaf The leaf to remove
     * @return The restore hint, or null if not found
     */
    public RestoreHint removeLeaf(DockLeaf leaf) {
        String leafId = leaf.getMetadata().id();
        minimizedLeaves.remove(leafId);
        orderedLeaves.remove(leaf);
        RestoreHint hint = restoreHints.remove(leafId);

        if (onLeafRemoved != null) {
            onLeafRemoved.accept(leaf);
        }

        return hint;
    }

    /**
     * Gets a minimized leaf by ID.
     *
     * @param leafId The leaf ID
     * @return The leaf, or null if not found
     */
    public DockLeaf getLeaf(String leafId) {
        return minimizedLeaves.get(leafId);
    }

    /**
     * Gets the restore hint for a leaf.
     *
     * @param leaf The leaf
     * @return The restore hint, or null if not found
     */
    public RestoreHint getRestoreHint(DockLeaf leaf) {
        return restoreHints.get(leaf.getMetadata().id());
    }

    /**
     * Gets the restore hint for a leaf ID.
     *
     * @param leafId The leaf ID
     * @return The restore hint, or null if not found
     */
    public RestoreHint getRestoreHint(String leafId) {
        return restoreHints.get(leafId);
    }

    /**
     * Checks if a leaf is minimized.
     *
     * @param leaf The leaf to check
     * @return true if minimized
     */
    public boolean isMinimized(DockLeaf leaf) {
        return minimizedLeaves.containsKey(leaf.getMetadata().id());
    }

    /**
     * Checks if a leaf ID is minimized.
     *
     * @param leafId The leaf ID to check
     * @return true if minimized
     */
    public boolean isMinimized(String leafId) {
        return minimizedLeaves.containsKey(leafId);
    }

    /**
     * Gets all minimized leaves in order.
     *
     * @return List of minimized leaves
     */
    public List<DockLeaf> getMinimizedLeaves() {
        return Collections.unmodifiableList(orderedLeaves);
    }

    /**
     * Gets the count of minimized leaves.
     *
     * @return Number of minimized leaves
     */
    public int getMinimizedCount() {
        return minimizedLeaves.size();
    }

    /**
     * Checks if the store is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return minimizedLeaves.isEmpty();
    }

    /**
     * Creates a restore hint from the current parent of a leaf.
     *
     * @param leaf The leaf to capture location for
     * @return A restore hint, or default if parent is null
     */
    public static RestoreHint captureRestoreHint(DockLeaf leaf) {
        DockTabGroup parent = leaf.getParent();
        if (parent != null) {
            int index = parent.getTabs().indexOf(leaf);
            if (parent.getTabs().size() > 1) {
                return RestoreHint.forTab(parent.getMetadata().id(), index);
            }

            DockElement groupParent = parent.getParent();
            if (groupParent instanceof DockSplitGroup split) {
                boolean isFirst = split.getFirst() == parent;
                DropZone zone = split.getOrientation() == javafx.geometry.Orientation.HORIZONTAL
                    ? (isFirst ? DropZone.WEST : DropZone.EAST)
                    : (isFirst ? DropZone.NORTH : DropZone.SOUTH);
                DockElement sibling = isFirst ? split.getSecond() : split.getFirst();
                String siblingId = sibling != null ? sibling.getMetadata().id() : null;
                return RestoreHint.forSplit(split.getMetadata().id(), zone, split.getDividerPosition(), siblingId);
            }

            return RestoreHint.forTab(parent.getMetadata().id(), index);
        }

        return RestoreHint.defaultRestore();
    }

    /**
     * Sets the handler called when a leaf is added.
     *
     * @param handler callback invoked when a leaf is added
     */
    public void setOnLeafAdded(Consumer<DockLeaf> handler) {
        this.onLeafAdded = handler;
    }

    /**
     * Sets the handler called when a leaf is removed.
     *
     * @param handler callback invoked when a leaf is removed
     */
    public void setOnLeafRemoved(Consumer<DockLeaf> handler) {
        this.onLeafRemoved = handler;
    }

    /**
     * Clears all minimized leaves.
     */
    public void clear() {
        minimizedLeaves.clear();
        restoreHints.clear();
        orderedLeaves.clear();
    }
}
