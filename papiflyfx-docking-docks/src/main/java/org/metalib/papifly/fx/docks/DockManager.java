package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.core.DockData;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockState;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.drag.DragManager;
import org.metalib.papifly.fx.docks.drag.DropZone;
import org.metalib.papifly.fx.docks.floating.FloatingDockWindow;
import org.metalib.papifly.fx.docks.floating.FloatingWindowManager;
import org.metalib.papifly.fx.docks.layout.ContentStateAdapter;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docks.layout.ContentFactory;
import org.metalib.papifly.fx.docks.layout.LayoutFactory;
import org.metalib.papifly.fx.docks.layout.data.BoundsData;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.FloatingLeafData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.MaximizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.MinimizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.RestoreHintData;
import org.metalib.papifly.fx.docks.minimize.MinimizedBar;
import org.metalib.papifly.fx.docks.minimize.MinimizedStore;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;
import org.metalib.papifly.fx.docks.render.OverlayCanvas;
import org.metalib.papifly.fx.docks.serial.DockSessionPersistence;
import org.metalib.papifly.fx.docks.theme.Theme;

import javafx.geometry.Rectangle2D;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Central manager for the docking framework.
 * Acts as the main entry point and global event bus.
 * Supports floating windows, minimize bar, and maximize functionality.
 */
public class DockManager {

    private final BorderPane mainContainer;
    private final StackPane rootPane;
    private final StackPane dockingLayer;
    private final OverlayCanvas overlayLayer;
    private final MinimizedBar minimizedBar;
    private final ObjectProperty<Theme> themeProperty;
    private final ObjectProperty<DockElement> rootElement;
    private final DragManager dragManager;
    private final LayoutFactory layoutFactory;
    private final MinimizedStore minimizedStore;

    private FloatingWindowManager floatingWindowManager;
    private Stage ownerStage;
    private ContentFactory contentFactory;

    // Maximize state
    private DockLeaf maximizedLeaf;
    private DockTabGroup maximizedGroup;
    private DockElement savedRootBeforeMaximize;
    private RestoreHint maximizeRestoreHint;

    // Floating restore hints (leaf ID -> hint)
    private final java.util.Map<String, RestoreHint> floatingRestoreHints = new java.util.HashMap<>();

    /**
     * Creates a new DockManager with default dark theme.
     */
    public DockManager() {
        this(Theme.dark());
    }

    /**
     * Creates a new DockManager with the specified theme.
     */
    public DockManager(Theme theme) {
        this.themeProperty = new SimpleObjectProperty<>(theme);
        this.rootElement = new SimpleObjectProperty<>();

        // Create layered structure
        dockingLayer = new StackPane();
        dockingLayer.setMinSize(0, 0); // Allow shrinking
        overlayLayer = new OverlayCanvas();

        rootPane = new StackPane(dockingLayer, overlayLayer);
        rootPane.setMinSize(0, 0); // Allow shrinking

        // Create minimized bar at the bottom
        minimizedBar = new MinimizedBar(themeProperty);
        minimizedBar.setOnRestore(this::restoreLeaf);

        // Create minimized store
        minimizedStore = new MinimizedStore();
        minimizedStore.setOnLeafAdded(minimizedBar::addLeaf);
        minimizedStore.setOnLeafRemoved(minimizedBar::removeLeaf);

        // Create main container with docking area and minimized bar
        mainContainer = new BorderPane();
        mainContainer.setCenter(rootPane);
        mainContainer.setBottom(minimizedBar);
        mainContainer.setMinSize(0, 0);

        // Bind overlay size to root
        rootPane.widthProperty().addListener((obs, oldVal, newVal) ->
            overlayLayer.resize(newVal.doubleValue(), rootPane.getHeight()));
        rootPane.heightProperty().addListener((obs, oldVal, newVal) ->
            overlayLayer.resize(rootPane.getWidth(), newVal.doubleValue()));

        // Initialize drag manager with tab group factory that sets up drag handlers
        dragManager = new DragManager(this::getRoot, overlayLayer, this::setRoot, themeProperty, this::createTabGroup);

        // Initialize layout factory
        layoutFactory = new LayoutFactory(themeProperty);
        // Apply theme background
        applyTheme(theme);
        themeProperty.addListener((obs, oldTheme, newTheme) -> applyTheme(newTheme));

        // Setup global mouse handlers for drag operations
        setupDragHandlers();
    }

    private void setupDragHandlers() {
        // Mouse drag handler at root level - must call onDrag even before threshold is crossed
        rootPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (dragManager.hasDragContext()) {
                dragManager.onDrag(event);
            }
        });

        // Mouse release handler at root level
        rootPane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (dragManager.hasDragContext()) {
                dragManager.endDrag(event);
            }
        });
    }

    private void applyTheme(Theme theme) {
        if (theme != null) {
            dockingLayer.setStyle("-fx-background-color: " + toHexString(theme.background()) + ";");
        }
    }

    private String toHexString(javafx.scene.paint.Paint paint) {
        if (paint instanceof javafx.scene.paint.Color color) {
            return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        }
        return "#1E1E1E";
    }

    /**
     * Gets the root pane to add to your scene.
     * This includes the docking area and minimized bar.
     */
    public Region getRootPane() {
        return mainContainer;
    }

    /**
     * Sets the owner stage for floating windows.
     * Must be called before using floating functionality.
     */
    public void setOwnerStage(Stage stage) {
        this.ownerStage = stage;
        this.floatingWindowManager = new FloatingWindowManager(stage, themeProperty, this::createTabGroup);

        // Setup callbacks for floating window manager
        floatingWindowManager.setOnDockBack(this::dockLeaf);
        floatingWindowManager.setOnClose(this::closeLeaf);
    }

    /**
     * Gets the current root dock element.
     */
    public DockElement getRoot() {
        return rootElement.get();
    }

    /**
     * Sets the root dock element.
     */
    public void setRoot(DockElement element) {
        DockElement oldRoot = rootElement.get();
        if (oldRoot != null) {
            dockingLayer.getChildren().remove(oldRoot.getNode());
        }

        rootElement.set(element);

        if (element != null) {
            element.setParent(null);
            dockingLayer.getChildren().add(0, element.getNode());
        }
    }

    /**
     * Builds and sets the root from a layout definition.
     */
    public void setRoot(LayoutNode layout) {
        DockElement element = layoutFactory.build(layout);
        wireHandlers(element);
        setRoot(element);
    }

    private void wireHandlers(DockElement element) {
        if (element == null) {
            return;
        }
        if (element instanceof DockTabGroup tabGroup) {
            setupTabGroupDragHandlers(tabGroup);
            setupTabGroupButtonHandlers(tabGroup);
            for (DockLeaf leaf : tabGroup.getTabs()) {
                setupLeafCloseHandler(leaf);
            }
        } else if (element instanceof DockSplitGroup split) {
            wireHandlers(split.getFirst());
            wireHandlers(split.getSecond());
        }
    }

    /**
     * Gets the root element property.
     */
    public ObjectProperty<DockElement> rootProperty() {
        return rootElement;
    }

    /**
     * Gets the theme property.
     */
    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    /**
     * Gets the current theme.
     */
    public Theme getTheme() {
        return themeProperty.get();
    }

    /**
     * Sets the theme.
     */
    public void setTheme(Theme theme) {
        themeProperty.set(theme);
    }

    /**
     * Gets the layout factory for programmatic layout building.
     */
    public LayoutFactory getLayoutFactory() {
        return layoutFactory;
    }

    /**
     * Gets the floating window manager.
     */
    public FloatingWindowManager getFloatingWindowManager() {
        return floatingWindowManager;
    }

    /**
     * Gets the minimized store.
     */
    public MinimizedStore getMinimizedStore() {
        return minimizedStore;
    }

    /**
     * Sets the content factory for layout restoration.
     */
    public void setContentFactory(ContentFactory factory) {
        this.contentFactory = factory;
        this.layoutFactory.setContentFactory(factory);
    }

    /**
     * Sets the content state registry for capture and restore.
     */
    public void setContentStateRegistry(ContentStateRegistry registry) {
        this.layoutFactory.setContentStateRegistry(registry);
    }

    /**
     * Gets the content state registry used for capture and restore.
     */
    public ContentStateRegistry getContentStateRegistry() {
        return layoutFactory.getContentStateRegistry();
    }

    /**
     * Creates a new leaf with the given title and content.
     */
    public DockLeaf createLeaf(String title, Node content) {
        DockLeaf leaf = layoutFactory.createLeaf(title, content);
        setupLeafCloseHandler(leaf);
        return leaf;
    }

    /**
     * Sets up the close handler for a leaf.
     */
    private void setupLeafCloseHandler(DockLeaf leaf) {
        leaf.onClose(this::closeLeaf);
    }

    /**
     * Closes a leaf, removing it from its parent.
     */
    private void closeLeaf(DockLeaf leaf) {
        if (maximizedLeaf == leaf) {
            restoreMaximized();
        }

        if (floatingWindowManager != null && floatingWindowManager.isFloating(leaf)) {
            floatingWindowManager.unfloatLeaf(leaf);
        }

        DockTabGroup parent = leaf.getParent();
        if (parent != null) {
            parent.removeLeaf(leaf);
            if (parent.getTabs().isEmpty()) {
                removeElement(parent);
            }
        }

        leaf.dispose();
        floatingRestoreHints.remove(leaf.getMetadata().id());
    }

    /**
     * Removes an element from the hierarchy.
     */
    private void removeElement(DockElement element) {
        DockElement parent = element.getParent();

        if (parent instanceof DockSplitGroup split) {
            DockElement sibling = (split.getFirst() == element) ? split.getSecond() : split.getFirst();

            detachChild(split, element);
            detachChild(split, sibling);

            DockElement grandparent = split.getParent();
            if (grandparent instanceof DockSplitGroup grandSplit) {
                grandSplit.replaceChild(split, sibling);
            } else if (grandparent == null) {
                setRoot(sibling);
            }

            split.dispose();
        } else if (parent == null) {
            setRoot((DockElement) null);
        }

        element.dispose();
    }

    /**
     * Creates a new tab group.
     */
    public DockTabGroup createTabGroup() {
        DockTabGroup tabGroup = new DockTabGroup(themeProperty);
        setupTabGroupDragHandlers(tabGroup);
        setupTabGroupButtonHandlers(tabGroup);
        return tabGroup;
    }

    /**
     * Sets up the float/minimize/maximize button handlers for a tab group.
     * These buttons apply to the active tab.
     */
    private void setupTabGroupButtonHandlers(DockTabGroup tabGroup) {
        tabGroup.setOnFloat(() -> {
            DockLeaf activeTab = tabGroup.getActiveTab();
            if (activeTab != null) {
                if (tabGroup.isFloating()) {
                    dockLeaf(activeTab);
                } else {
                    floatLeaf(activeTab);
                }
            }
        });

        tabGroup.setOnMinimize(() -> {
            DockLeaf activeTab = tabGroup.getActiveTab();
            if (activeTab != null) {
                minimizeLeaf(activeTab);
            }
        });

        tabGroup.setOnMaximize(() -> {
            DockLeaf activeTab = tabGroup.getActiveTab();
            if (activeTab != null) {
                if (tabGroup.isMaximized()) {
                    restoreMaximized();
                } else {
                    maximizeLeaf(activeTab);
                }
            }
        });
    }

    /**
     * Creates a new horizontal split.
     */
    public DockSplitGroup createHorizontalSplit(DockElement first, DockElement second) {
        return layoutFactory.createHorizontalSplit(first, second);
    }

    /**
     * Creates a new horizontal split with custom divider position.
     */
    public DockSplitGroup createHorizontalSplit(DockElement first, DockElement second, double dividerPosition) {
        return layoutFactory.createHorizontalSplit(first, second, dividerPosition);
    }

    /**
     * Creates a new vertical split.
     */
    public DockSplitGroup createVerticalSplit(DockElement first, DockElement second) {
        return layoutFactory.createVerticalSplit(first, second);
    }

    /**
     * Creates a new vertical split with custom divider position.
     */
    public DockSplitGroup createVerticalSplit(DockElement first, DockElement second, double dividerPosition) {
        return layoutFactory.createVerticalSplit(first, second, dividerPosition);
    }

    /**
     * Captures the current layout as a LayoutNode for serialization.
     */
    public LayoutNode capture() {
        refreshContentStatesForLayout();
        DockElement root = rootElement.get();
        return root != null ? root.serialize() : null;
    }

    /**
     * Restores a layout from a LayoutNode.
     */
    public void restore(LayoutNode layout) {
        setRoot(layout);
    }

    // === Session Persistence API ===

    /**
     * Captures the current session including layout, floating windows, minimized and maximized state.
     *
     * @return DockSessionData representing the complete session state
     */
    public DockSessionData captureSession() {
        refreshContentStatesForSession();
        // Capture docked tree
        DockElement root = rootElement.get();
        LayoutNode layout = root != null ? root.serialize() : null;

        // Capture floating leaves
        List<FloatingLeafData> floatingList = new ArrayList<>();
        if (floatingWindowManager != null) {
            for (FloatingDockWindow window : floatingWindowManager.getFloatingWindows()) {
                DockLeaf leaf = window.getLeaf();
                String leafId = leaf.getMetadata().id();

                // Serialize leaf
                LeafData leafData = (LeafData) leaf.serialize();

                // Get bounds
                Rectangle2D bounds = window.getBounds();
                BoundsData boundsData = bounds != null
                    ? new BoundsData(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight())
                    : null;

                // Get restore hint
                RestoreHint hint = floatingRestoreHints.get(leafId);
                RestoreHintData hintData = hint != null
                    ? new RestoreHintData(hint.parentId(), hint.zone() != null ? hint.zone().name() : null,
                        hint.tabIndex(), hint.splitPosition(), hint.siblingId())
                    : null;

                floatingList.add(new FloatingLeafData(leafData, boundsData, hintData));
            }
        }

        // Capture minimized leaves
        List<MinimizedLeafData> minimizedList = new ArrayList<>();
        for (DockLeaf leaf : minimizedStore.getMinimizedLeaves()) {
            LeafData leafData = (LeafData) leaf.serialize();
            RestoreHint hint = minimizedStore.getRestoreHint(leaf);
            RestoreHintData hintData = hint != null
                ? new RestoreHintData(hint.parentId(), hint.zone() != null ? hint.zone().name() : null,
                    hint.tabIndex(), hint.splitPosition(), hint.siblingId())
                : null;
            minimizedList.add(new MinimizedLeafData(leafData, hintData));
        }

        // Capture maximized state
        MaximizedLeafData maximizedData = null;
        if (maximizedLeaf != null) {
            LeafData leafData = (LeafData) maximizedLeaf.serialize();
            RestoreHintData hintData = maximizeRestoreHint != null
                ? new RestoreHintData(maximizeRestoreHint.parentId(),
                    maximizeRestoreHint.zone() != null ? maximizeRestoreHint.zone().name() : null,
                    maximizeRestoreHint.tabIndex(), maximizeRestoreHint.splitPosition(),
                    maximizeRestoreHint.siblingId())
                : null;
            maximizedData = new MaximizedLeafData(leafData, hintData);
        }

        return DockSessionData.of(layout, floatingList, minimizedList, maximizedData);
    }

    private void refreshContentStatesForLayout() {
        ContentStateRegistry registry = layoutFactory.getContentStateRegistry();
        if (registry == null || registry.isEmpty()) {
            return;
        }
        DockElement root = rootElement.get();
        if (root == null) {
            return;
        }
        List<DockLeaf> leaves = new ArrayList<>();
        collectLeaves(root, leaves);
        refreshContentStates(leaves, registry);
    }

    private void refreshContentStatesForSession() {
        ContentStateRegistry registry = layoutFactory.getContentStateRegistry();
        if (registry == null || registry.isEmpty()) {
            return;
        }
        Collection<DockLeaf> leaves = new LinkedHashSet<>();
        collectLeaves(rootElement.get(), leaves);

        if (floatingWindowManager != null) {
            for (FloatingDockWindow window : floatingWindowManager.getFloatingWindows()) {
                leaves.add(window.getLeaf());
            }
        }

        leaves.addAll(minimizedStore.getMinimizedLeaves());

        if (maximizedLeaf != null) {
            leaves.add(maximizedLeaf);
        }

        refreshContentStates(leaves, registry);
    }

    private void refreshContentStates(Collection<DockLeaf> leaves, ContentStateRegistry registry) {
        for (DockLeaf leaf : leaves) {
            refreshContentState(leaf, registry);
        }
    }

    private void refreshContentState(DockLeaf leaf, ContentStateRegistry registry) {
        if (leaf == null || registry == null) {
            return;
        }

        LeafContentData existing = leaf.getContentData();
        String typeKey = existing != null && existing.typeKey() != null
            ? existing.typeKey()
            : leaf.getContentFactoryId();
        if (typeKey == null) {
            return;
        }

        ContentStateAdapter adapter = registry.getAdapter(typeKey);
        if (adapter == null) {
            return;
        }

        String contentId = existing != null && existing.contentId() != null
            ? existing.contentId()
            : leaf.getMetadata().id();
        if (contentId == null) {
            return;
        }

        Node content = leaf.getContent();
        if (content == null) {
            return;
        }

        Map<String, Object> state = adapter.saveState(contentId, content);
        leaf.setContentData(new LeafContentData(typeKey, contentId, adapter.getVersion(), state));
    }

    private void collectLeaves(DockElement element, Collection<DockLeaf> leaves) {
        if (element == null) {
            return;
        }
        if (element instanceof DockTabGroup tabGroup) {
            leaves.addAll(tabGroup.getTabs());
        } else if (element instanceof DockSplitGroup split) {
            collectLeaves(split.getFirst(), leaves);
            collectLeaves(split.getSecond(), leaves);
        }
    }

    /**
     * Restores a session from DockSessionData.
     *
     * @param session the session to restore
     */
    public void restoreSession(DockSessionData session) {
        if (session == null) {
            return;
        }

        // Clear current state
        if (maximizedLeaf != null) {
            restoreMaximized();
        }
        if (floatingWindowManager != null) {
            floatingWindowManager.closeAll();
        }
        floatingRestoreHints.clear();
        minimizedStore.clear();

        // Restore docked layout
        if (session.layout() != null) {
            restore(session.layout());
        }

        // Restore floating leaves
        if (session.floating() != null) {
            for (FloatingLeafData floatingData : session.floating()) {
                if (floatingData.leaf() != null) {
                    // Build leaf
                    DockLeaf leaf = layoutFactory.buildLeaf(floatingData.leaf());
                    setupLeafCloseHandler(leaf);

                    // Convert bounds
                    Rectangle2D bounds = null;
                    if (floatingData.bounds() != null) {
                        BoundsData bd = floatingData.bounds();
                        bounds = new Rectangle2D(bd.x(), bd.y(), bd.width(), bd.height());
                    }

                    // Convert restore hint
                    RestoreHint hint = null;
                    if (floatingData.restoreHint() != null) {
                        RestoreHintData hd = floatingData.restoreHint();
                        DropZone zone = hd.zone() != null ? DropZone.valueOf(hd.zone()) : null;
                        hint = new RestoreHint(hd.parentId(), zone, hd.tabIndex(), hd.splitPosition(), hd.siblingId());
                    }

                    // Restore floating state
                    restoreFloating(leaf, hint, bounds);
                }
            }
        }

        // Restore minimized leaves
        if (session.minimized() != null) {
            for (MinimizedLeafData minimizedData : session.minimized()) {
                if (minimizedData.leaf() != null) {
                    DockLeaf leaf = layoutFactory.buildLeaf(minimizedData.leaf());
                    setupLeafCloseHandler(leaf);

                    // Convert restore hint
                    RestoreHint hint = null;
                    if (minimizedData.restoreHint() != null) {
                        RestoreHintData hd = minimizedData.restoreHint();
                        DropZone zone = hd.zone() != null ? DropZone.valueOf(hd.zone()) : null;
                        hint = new RestoreHint(hd.parentId(), zone, hd.tabIndex(), hd.splitPosition(), hd.siblingId());
                    }

                    // Add to minimized store
                    updateLeafState(leaf, DockState.MINIMIZED);
                    minimizedStore.addLeaf(leaf, hint);
                }
            }
        }

        // Restore maximized state (we'll do this last)
        // Note: For now, we skip maximized restoration as it requires the leaf to be in the tree first
        // This is a known limitation that can be addressed in future enhancements
    }

    /**
     * Saves the current session to a JSON string.
     *
     * @return JSON representation of the current session, or empty string if no session
     * @throws DockSessionPersistence.SessionSerializationException if serialization fails
     */
    public String saveSessionToString() {
        DockSessionData session = captureSession();
        DockSessionPersistence persistence = new DockSessionPersistence();
        String json = persistence.toJsonString(session);
        return json != null ? json : "";
    }

    /**
     * Restores a session from a JSON string.
     *
     * @param json the JSON string containing the session
     * @throws DockSessionPersistence.SessionSerializationException if deserialization fails
     */
    public void restoreSessionFromString(String json) {
        DockSessionPersistence persistence = new DockSessionPersistence();
        DockSessionData session = persistence.fromJsonString(json);
        if (session != null) {
            restoreSession(session);
        }
    }

    /**
     * Saves the current session to a JSON file.
     *
     * @param path the file path to write to
     * @throws DockSessionPersistence.SessionSerializationException if serialization fails
     * @throws DockSessionPersistence.SessionFileIOException        if file I/O fails
     */
    public void saveSessionToFile(Path path) {
        DockSessionData session = captureSession();
        DockSessionPersistence persistence = new DockSessionPersistence();
        persistence.toJsonFile(session, path);
    }

    /**
     * Loads a session from a JSON file.
     *
     * @param path the file path to read from
     * @throws DockSessionPersistence.SessionFileIOException        if file I/O fails
     * @throws DockSessionPersistence.SessionSerializationException if deserialization fails
     */
    public void loadSessionFromFile(Path path) {
        DockSessionPersistence persistence = new DockSessionPersistence();
        DockSessionData session = persistence.fromJsonFile(path);
        if (session != null) {
            restoreSession(session);
        }
    }

    /**
     * Restores a leaf as floating without overwriting its restore hint.
     * Used internally during session restoration.
     *
     * @param leaf        the leaf to float
     * @param restoreHint the restore hint for docking back
     * @param bounds      the window bounds, or null for default
     */
    private void restoreFloating(DockLeaf leaf, RestoreHint restoreHint, Rectangle2D bounds) {
        if (floatingWindowManager == null) {
            throw new IllegalStateException("Owner stage not set. Call setOwnerStage() first.");
        }

        String leafId = leaf.getMetadata().id();

        // Store restore hint
        if (restoreHint != null) {
            floatingRestoreHints.put(leafId, restoreHint);
        }

        // Update state
        updateLeafState(leaf, DockState.FLOATING);

        // Create floating window
        FloatingDockWindow window = floatingWindowManager.floatLeaf(leaf);

        // Apply bounds if provided
        if (bounds != null) {
            window.setBounds(bounds);
        }

        // Show window
        window.show();
    }

    /**
     * Sets up drag handlers for a tab group.
     */
    public void setupTabGroupDragHandlers(DockTabGroup tabGroup) {
        // Tab-specific drag handling using event filters to capture events from child tabs
        tabGroup.getTabBar().addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!event.isPrimaryButtonDown()) return;

            // Find which tab was clicked
            Node target = event.getPickResult().getIntersectedNode();
            while (target != null && target != tabGroup.getTabBar()) {
                if (target.getUserData() instanceof DockLeaf leaf) {
                    dragManager.startDrag(leaf, event);
                    // Don't consume - let click handler work for tab selection
                    return;
                }
                target = target.getParent();
            }
        });

        tabGroup.getTabBar().addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (dragManager.hasDragContext()) {
                dragManager.onDrag(event);
                event.consume();
            }
        });

        tabGroup.getTabBar().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!dragManager.hasDragContext()) {
                return;
            }
            boolean wasDragging = dragManager.isDragging();
            dragManager.endDrag(event);
            // Only consume if we actually dragged (not just clicked)
            if (wasDragging) {
                event.consume();
            }
        });
    }

    // === Floating Window API ===

    /**
     * Floats a leaf from the dock tree into a floating window.
     */
    public void floatLeaf(DockLeaf leaf) {
        if (floatingWindowManager == null) {
            throw new IllegalStateException("Owner stage not set. Call setOwnerStage() first.");
        }

        if (maximizedLeaf == leaf) {
            restoreMaximized();
        }

        if (floatingWindowManager.isFloating(leaf)) {
            floatingWindowManager.getWindow(leaf).toFront();
            return;
        }

        // Capture restore hint BEFORE removing from tree
        String leafId = leaf.getMetadata().id();
        RestoreHint hint = MinimizedStore.captureRestoreHint(leaf);
        floatingRestoreHints.put(leafId, hint);

        // Remove from dock tree
        removeLeafFromDock(leaf);

        // Update state
        updateLeafState(leaf, DockState.FLOATING);

        // Create floating window
        FloatingDockWindow window = floatingWindowManager.floatLeaf(leaf);
        window.show();
    }

    /**
     * Floats a leaf at a specific position.
     */
    public void floatLeaf(DockLeaf leaf, double x, double y) {
        if (floatingWindowManager == null) {
            throw new IllegalStateException("Owner stage not set. Call setOwnerStage() first.");
        }

        if (maximizedLeaf == leaf) {
            restoreMaximized();
        }

        if (floatingWindowManager.isFloating(leaf)) {
            FloatingDockWindow window = floatingWindowManager.getWindow(leaf);
            if (window != null) {
                window.show(x, y);
            }
            return;
        }

        // Capture restore hint BEFORE removing from tree
        String leafId = leaf.getMetadata().id();
        RestoreHint hint = MinimizedStore.captureRestoreHint(leaf);
        floatingRestoreHints.put(leafId, hint);

        // Remove from dock tree
        removeLeafFromDock(leaf);

        // Update state
        updateLeafState(leaf, DockState.FLOATING);

        // Create floating window at position
        floatingWindowManager.floatLeaf(leaf, x, y);
    }

    /**
     * Docks a floating leaf back into the dock tree.
     */
    public void dockLeaf(DockLeaf leaf) {
        if (floatingWindowManager != null && floatingWindowManager.isFloating(leaf)) {
            floatingWindowManager.unfloatLeaf(leaf);
        }

        // Update state
        updateLeafState(leaf, DockState.DOCKED);

        // Try to restore to original position using saved hint
        String leafId = leaf.getMetadata().id();
        RestoreHint hint = floatingRestoreHints.remove(leafId);

        if (hint != null && tryRestoreWithHint(leaf, hint)) {
            return; // Successfully restored to original position
        }

        // Fallback: insert at default position
        insertLeafIntoDock(leaf);
    }

    // === Minimize API ===

    /**
     * Minimizes a leaf, removing it from the dock tree and adding to minimized bar.
     */
    public void minimizeLeaf(DockLeaf leaf) {
        // If maximized, restore first
        if (maximizedLeaf == leaf) {
            restoreMaximized();
        }

        // If floating, unfloat first
        if (floatingWindowManager != null && floatingWindowManager.isFloating(leaf)) {
            floatingWindowManager.unfloatLeaf(leaf);
        }

        // Capture restore hint
        RestoreHint hint = MinimizedStore.captureRestoreHint(leaf);

        // Remove from dock tree
        removeLeafFromDock(leaf);

        // Update state
        updateLeafState(leaf, DockState.MINIMIZED);

        // Add to minimized store
        minimizedStore.addLeaf(leaf, hint);
    }

    /**
     * Restores a minimized leaf back into the dock tree.
     */
    public void restoreLeaf(DockLeaf leaf) {
        if (!minimizedStore.isMinimized(leaf)) {
            return;
        }

        RestoreHint hint = minimizedStore.removeLeaf(leaf);

        // Update state
        updateLeafState(leaf, DockState.DOCKED);

        // Try to restore to original position, or fallback to default
        if (!tryRestoreWithHint(leaf, hint)) {
            insertLeafIntoDock(leaf);
        }
    }

    /**
     * Restores a minimized leaf by ID.
     */
    public void restoreLeaf(String leafId) {
        DockLeaf leaf = minimizedStore.getLeaf(leafId);
        if (leaf != null) {
            restoreLeaf(leaf);
        }
    }

    // === Maximize API ===

    /**
     * Maximizes a leaf to fill the entire dock area.
     * The previous layout is preserved and restored when unmaximizing.
     */
    public void maximizeLeaf(DockLeaf leaf) {
        if (maximizedLeaf != null) {
            restoreMaximized();
        }

        // If floating, dock first
        if (floatingWindowManager != null && floatingWindowManager.isFloating(leaf)) {
            floatingWindowManager.unfloatLeaf(leaf);
        }

        DockTabGroup originalParent = leaf.getParent();
        if (originalParent != null) {
            int index = originalParent.getTabs().indexOf(leaf);
            maximizeRestoreHint = RestoreHint.forTab(originalParent.getMetadata().id(), index);
        } else {
            maximizeRestoreHint = RestoreHint.defaultRestore();
        }
        maximizedLeaf = leaf;

        if (originalParent != null) {
            originalParent.removeLeaf(leaf);
        }

        // Save the current root
        savedRootBeforeMaximize = rootElement.get();

        // Remove the root from view
        if (savedRootBeforeMaximize != null) {
            dockingLayer.getChildren().remove(savedRootBeforeMaximize.getNode());
        }

        maximizedGroup = createTabGroup();
        maximizedGroup.addLeaf(leaf);
        maximizedGroup.setMaximized(true);

        // Add the maximized group's node directly to fill the dock area
        dockingLayer.getChildren().add(0, maximizedGroup.getNode());

        // Update state
        updateLeafState(leaf, DockState.MAXIMIZED);
    }

    /**
     * Restores from maximized state to previous layout.
     */
    public void restoreMaximized() {
        if (maximizedLeaf == null) {
            return;
        }

        DockLeaf leaf = maximizedLeaf;

        if (maximizedGroup != null) {
            dockingLayer.getChildren().remove(maximizedGroup.getNode());
            maximizedGroup.removeLeaf(leaf);
            maximizedGroup = null;
        }

        // Restore the original root to docking layer
        if (savedRootBeforeMaximize != null) {
            dockingLayer.getChildren().add(0, savedRootBeforeMaximize.getNode());
            rootElement.set(savedRootBeforeMaximize);
        }

        // Re-insert the leaf at its original position
        if (maximizeRestoreHint != null && maximizeRestoreHint.parentId() != null) {
            DockElement target = findElementById(savedRootBeforeMaximize, maximizeRestoreHint.parentId());
            if (target instanceof DockTabGroup tabGroup) {
                int index = Math.min(maximizeRestoreHint.tabIndex(), tabGroup.getTabs().size());
                tabGroup.addLeaf(Math.max(0, index), leaf);
                tabGroup.setActiveTab(leaf);
                tabGroup.refreshActiveTabContent();
            } else if (target instanceof DockSplitGroup split) {
                // Re-insert into split at original position
                DropZone zone = maximizeRestoreHint.zone();
                if (zone == DropZone.WEST || zone == DropZone.NORTH) {
                    DockTabGroup tabGroup = createTabGroup();
                    tabGroup.addLeaf(leaf);
                    split.setFirst(tabGroup);
                } else {
                    DockTabGroup tabGroup = createTabGroup();
                    tabGroup.addLeaf(leaf);
                    split.setSecond(tabGroup);
                }
            } else {
                // Fallback: insert into dock
                insertLeafIntoDock(leaf);
            }
        } else {
            // No hint - insert at default position
            insertLeafIntoDock(leaf);
        }

        // Update state
        updateLeafState(leaf, DockState.DOCKED);

        maximizedLeaf = null;
        maximizedGroup = null;
        savedRootBeforeMaximize = null;
        maximizeRestoreHint = null;
    }

    /**
     * Checks if any leaf is currently maximized.
     */
    public boolean isMaximized() {
        return maximizedLeaf != null;
    }

    /**
     * Gets the currently maximized leaf, if any.
     */
    public DockLeaf getMaximizedLeaf() {
        return maximizedLeaf;
    }

    // === Internal helpers ===

    /**
     * Removes a leaf from the dock tree without disposing it.
     */
    private void removeLeafFromDock(DockLeaf leaf) {
        DockTabGroup parent = leaf.getParent();
        if (parent != null) {
            parent.removeLeaf(leaf);
            if (parent.getTabs().isEmpty()) {
                removeElementWithoutDispose(parent);
            }
        }
    }

    /**
     * Removes an element without disposing it.
     */
    private void removeElementWithoutDispose(DockElement element) {
        DockElement parent = element.getParent();

        if (parent instanceof DockSplitGroup split) {
            DockElement sibling = (split.getFirst() == element) ? split.getSecond() : split.getFirst();

            detachChild(split, element);
            detachChild(split, sibling);

            DockElement grandparent = split.getParent();
            if (grandparent instanceof DockSplitGroup grandSplit) {
                grandSplit.replaceChild(split, sibling);
            } else if (grandparent == null) {
                setRoot(sibling);
            }

            split.dispose();
        } else if (parent == null) {
            setRoot((DockElement) null);
        }
    }

    /**
     * Inserts a leaf into the dock tree at a default position.
     */
    private void insertLeafIntoDock(DockLeaf leaf) {
        DockElement currentRoot = rootElement.get();

        if (currentRoot == null) {
            // No existing content, make a single-tab group the root
            DockTabGroup tabGroup = createTabGroup();
            tabGroup.addLeaf(leaf);
            setRoot(tabGroup);
        } else if (currentRoot instanceof DockTabGroup tabGroup) {
            // Add as tab to existing tab group
            tabGroup.addLeaf(leaf);
        } else {
            // Add as horizontal split on the right
            DockTabGroup tabGroup = createTabGroup();
            tabGroup.addLeaf(leaf);
            DockSplitGroup newSplit = createHorizontalSplit(currentRoot, tabGroup, 0.75);
            setRoot(newSplit);
        }
    }

    /**
     * Detaches a child from a split so it can be safely reparented.
     */
    private void detachChild(DockSplitGroup split, DockElement child) {
        if (child == null) {
            return;
        }
        if (split.getFirst() == child) {
            split.setFirst(null);
        } else if (split.getSecond() == child) {
            split.setSecond(null);
        }
    }

    /**
     * Tries to restore a leaf using the provided hint.
     */
    private boolean tryRestoreWithHint(DockLeaf leaf, RestoreHint hint) {
        if (hint == null) {
            return false;
        }

        // First, try to find the original parent by ID
        if (hint.parentId() != null) {
            DockElement target = findElementById(rootElement.get(), hint.parentId());

            if (target instanceof DockTabGroup tabGroup && hint.zone() == DropZone.TAB_BAR) {
                int index = Math.min(hint.tabIndex(), tabGroup.getTabs().size());
                tabGroup.addLeaf(index >= 0 ? index : tabGroup.getTabs().size(), leaf);
                return true;
            }

            if (target instanceof DockSplitGroup split) {
                // Re-insert into split at original position
                DropZone zone = hint.zone();
                if (zone == DropZone.WEST || zone == DropZone.NORTH) {
                    if (split.getFirst() == null) {
                        DockTabGroup tabGroup = createTabGroup();
                        tabGroup.addLeaf(leaf);
                        split.setFirst(tabGroup);
                        return true;
                    }
                } else if (zone == DropZone.EAST || zone == DropZone.SOUTH) {
                    if (split.getSecond() == null) {
                        DockTabGroup tabGroup = createTabGroup();
                        tabGroup.addLeaf(leaf);
                        split.setSecond(tabGroup);
                        return true;
                    }
                }
            }
        }

        // Fallback: try to restore next to the sibling (for splits that were collapsed)
        if (hint.siblingId() != null) {
            DockElement sibling = findElementById(rootElement.get(), hint.siblingId());
            if (sibling != null) {
                DockElement siblingParent = sibling.getParent();
                DropZone zone = hint.zone();
                Orientation orientation = (zone == DropZone.WEST || zone == DropZone.EAST)
                    ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                boolean leafFirst = (zone == DropZone.WEST || zone == DropZone.NORTH);

                // Create a new split
                DockSplitGroup newSplit = new DockSplitGroup(orientation, themeProperty);
                newSplit.setDividerPosition(hint.splitPosition());

                // First, replace sibling with newSplit in its parent (this detaches sibling)
                if (siblingParent instanceof DockSplitGroup parentSplit) {
                    parentSplit.replaceChild(sibling, newSplit);
                } else if (siblingParent == null) {
                    // Sibling was root - replace root with newSplit
                    dockingLayer.getChildren().remove(sibling.getNode());
                    rootElement.set(newSplit);
                    dockingLayer.getChildren().add(0, newSplit.getNode());
                }

                // Now add children to the new split
                if (leafFirst) {
                    DockTabGroup tabGroup = createTabGroup();
                    tabGroup.addLeaf(leaf);
                    newSplit.setFirst(tabGroup);
                    newSplit.setSecond(sibling);
                } else {
                    DockTabGroup tabGroup = createTabGroup();
                    tabGroup.addLeaf(leaf);
                    newSplit.setFirst(sibling);
                    newSplit.setSecond(tabGroup);
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Finds an element by ID in the tree.
     */
    private DockElement findElementById(DockElement element, String id) {
        if (element == null) {
            return null;
        }
        if (element.getMetadata().id().equals(id)) {
            return element;
        }
        if (element instanceof DockSplitGroup split) {
            DockElement found = findElementById(split.getFirst(), id);
            if (found != null) return found;
            return findElementById(split.getSecond(), id);
        }
        if (element instanceof DockTabGroup tabGroup) {
            for (DockLeaf tab : tabGroup.getTabs()) {
                if (tab.getMetadata().id().equals(id)) {
                    return tabGroup;
                }
            }
        }
        return null;
    }

    /**
     * Updates the state in leaf metadata.
     */
    private void updateLeafState(DockLeaf leaf, DockState state) {
        DockData current = leaf.getMetadata();
        leaf.metadataProperty().set(current.withState(state));
    }

    /**
     * Disposes of the dock manager and all elements.
     */
    public void dispose() {
        // Dispose floating windows
        if (floatingWindowManager != null) {
            floatingWindowManager.dispose();
        }
        floatingRestoreHints.clear();

        // Clear minimized store
        minimizedStore.clear();
        minimizedBar.clear();

        DockElement root = rootElement.get();
        if (root != null) {
            root.dispose();
        }
        rootElement.set(null);
        dockingLayer.getChildren().clear();
    }

    // === Fluent Builder API ===

    /**
     * Creates a new DockManager builder.
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Fluent builder for DockManager.
     */
    public static class Builder {
        private Theme theme = Theme.dark();
        private LayoutNode layout;
        private ContentFactory contentFactory;

        public Builder withTheme(Theme theme) {
            this.theme = theme;
            return this;
        }

        public Builder withLayout(LayoutNode layout) {
            this.layout = layout;
            return this;
        }

        public Builder withContentFactory(ContentFactory factory) {
            this.contentFactory = factory;
            return this;
        }

        public DockManager build() {
            DockManager manager = new DockManager(theme);
            if (contentFactory != null) {
                manager.setContentFactory(contentFactory);
            }
            if (layout != null) {
                manager.setRoot(layout);
            }
            return manager;
        }
    }
}
