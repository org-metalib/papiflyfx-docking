package org.metalib.papifly.fx.docks.floating;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docking.api.Theme;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages all floating windows in the docking framework.
 * Tracks open floating windows by leaf ID and handles window lifecycle.
 */
public class FloatingWindowManager {

    private final Map<String, FloatingDockWindow> floatingWindows;
    private final Map<String, Rectangle2D> positionMemory;
    private final ObjectProperty<Theme> themeProperty;
    private final Stage ownerStage;
    private final Supplier<DockTabGroup> tabGroupFactory;

    private Consumer<DockLeaf> onDockBack;
    private Consumer<DockLeaf> onClose;

    /**
     * Creates a new FloatingWindowManager.
     *
     * @param ownerStage    The main application stage
     * @param themeProperty The theme property for styling
     */
    public FloatingWindowManager(Stage ownerStage, ObjectProperty<Theme> themeProperty,
                                 Supplier<DockTabGroup> tabGroupFactory) {
        this.ownerStage = ownerStage;
        this.themeProperty = themeProperty;
        this.tabGroupFactory = tabGroupFactory;
        this.floatingWindows = new HashMap<>();
        this.positionMemory = new HashMap<>();
    }

    /**
     * Floats a leaf into a new window.
     *
     * @param leaf The leaf to float
     * @return The created floating window
     */
    public FloatingDockWindow floatLeaf(DockLeaf leaf) {
        String leafId = leaf.getMetadata().id();

        // Check if already floating
        if (floatingWindows.containsKey(leafId)) {
            FloatingDockWindow existing = floatingWindows.get(leafId);
            existing.toFront();
            return existing;
        }

        DockTabGroup tabGroup = tabGroupFactory.get();
        tabGroup.addLeaf(leaf);
        tabGroup.setFloating(true);

        // Create new floating window
        FloatingDockWindow window = new FloatingDockWindow(leaf, tabGroup, ownerStage, themeProperty);

        // Restore previous position if available
        Rectangle2D savedBounds = positionMemory.get(leafId);
        if (savedBounds != null) {
            window.setBounds(savedBounds);
        }

        // Setup callbacks
        window.setOnDockBack(floatLeaf -> {
            if (onDockBack != null) {
                onDockBack.accept(floatLeaf);
            }
        });

        window.setOnClose(floatLeaf -> {
            if (onClose != null) {
                onClose.accept(floatLeaf);
            }
        });

        floatingWindows.put(leafId, window);
        return window;
    }

    /**
     * Floats a leaf at a specific position.
     *
     * @param leaf The leaf to float
     * @param x    X position
     * @param y    Y position
     * @return The created floating window
     */
    public FloatingDockWindow floatLeaf(DockLeaf leaf, double x, double y) {
        FloatingDockWindow window = floatLeaf(leaf);
        window.show(x, y);
        return window;
    }

    /**
     * Unfloats a leaf, removing its window from management.
     *
     * @param leaf The leaf to unfloat
     */
    public void unfloatLeaf(DockLeaf leaf) {
        String leafId = leaf.getMetadata().id();
        FloatingDockWindow window = floatingWindows.remove(leafId);

        if (window != null) {
            // Remember position for future floating
            Rectangle2D bounds = window.getBounds();
            if (bounds != null) {
                positionMemory.put(leafId, bounds);
            }
            DockTabGroup tabGroup = window.getTabGroup();
            if (tabGroup != null) {
                tabGroup.removeLeaf(leaf);
                if (tabGroup.getTabs().isEmpty()) {
                    tabGroup.dispose();
                }
            }
            window.close();
        }
    }

    /**
     * Gets the floating window for a leaf.
     *
     * @param leaf The leaf to find
     * @return The floating window, or null if not floating
     */
    public FloatingDockWindow getWindow(DockLeaf leaf) {
        return floatingWindows.get(leaf.getMetadata().id());
    }

    /**
     * Gets the floating window for a leaf ID.
     *
     * @param leafId The leaf ID to find
     * @return The floating window, or null if not floating
     */
    public FloatingDockWindow getWindow(String leafId) {
        return floatingWindows.get(leafId);
    }

    /**
     * Checks if a leaf is currently floating.
     *
     * @param leaf The leaf to check
     * @return true if floating
     */
    public boolean isFloating(DockLeaf leaf) {
        return floatingWindows.containsKey(leaf.getMetadata().id());
    }

    /**
     * Checks if a leaf ID is currently floating.
     *
     * @param leafId The leaf ID to check
     * @return true if floating
     */
    public boolean isFloating(String leafId) {
        return floatingWindows.containsKey(leafId);
    }

    /**
     * Gets all currently floating windows.
     *
     * @return Collection of floating windows
     */
    public Collection<FloatingDockWindow> getFloatingWindows() {
        return floatingWindows.values();
    }

    /**
     * Gets the count of floating windows.
     *
     * @return Number of floating windows
     */
    public int getFloatingCount() {
        return floatingWindows.size();
    }

    /**
     * Gets the saved bounds for a leaf ID.
     *
     * @param leafId The leaf ID
     * @return The saved bounds, or null if none
     */
    public Rectangle2D getSavedBounds(String leafId) {
        return positionMemory.get(leafId);
    }

    /**
     * Saves bounds for a leaf ID.
     *
     * @param leafId The leaf ID
     * @param bounds The bounds to save
     */
    public void saveBounds(String leafId, Rectangle2D bounds) {
        positionMemory.put(leafId, bounds);
    }

    /**
     * Sets the handler called when a floating leaf requests to dock back.
     */
    public void setOnDockBack(Consumer<DockLeaf> handler) {
        this.onDockBack = handler;
    }

    /**
     * Sets the handler called when a floating window is closed.
     */
    public void setOnClose(Consumer<DockLeaf> handler) {
        this.onClose = handler;
    }

    /**
     * Closes all floating windows.
     */
    public void closeAll() {
        for (FloatingDockWindow window : floatingWindows.values()) {
            Rectangle2D bounds = window.getBounds();
            if (bounds != null) {
                positionMemory.put(window.getLeafId(), bounds);
            }
            DockTabGroup tabGroup = window.getTabGroup();
            DockLeaf leaf = window.getLeaf();
            if (tabGroup != null && leaf != null) {
                tabGroup.removeLeaf(leaf);
                if (tabGroup.getTabs().isEmpty()) {
                    tabGroup.dispose();
                }
            }
            window.close();
        }
        floatingWindows.clear();
    }

    /**
     * Disposes of the manager and closes all windows.
     */
    public void dispose() {
        closeAll();
        positionMemory.clear();
    }
}
