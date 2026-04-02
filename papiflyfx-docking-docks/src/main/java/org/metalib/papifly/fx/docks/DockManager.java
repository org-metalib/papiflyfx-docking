package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import org.metalib.papifly.fx.docking.api.ContentFactory;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docks.layout.LayoutFactory;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.minimize.MinimizedBar;
import org.metalib.papifly.fx.docks.minimize.MinimizedStore;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;
import org.metalib.papifly.fx.docks.render.OverlayCanvas;

import javafx.geometry.Rectangle2D;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Central manager for the docking framework.
 * Acts as the main entry point and global event bus.
 * Supports floating windows, minimize bar, and maximize functionality.
 */
public class DockManager {

    private static final Logger LOG = Logger.getLogger(DockManager.class.getName());
    public static final String ROOT_PANE_MANAGER_PROPERTY = DockManager.class.getName() + ".rootPaneManager";

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
    private final DockTreeService treeService;
    private final DockSessionService sessionService;

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
     *
     * @param theme initial dock theme
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
        mainContainer.getProperties().put(ROOT_PANE_MANAGER_PROPERTY, this);

        // Bind overlay size to root
        rootPane.widthProperty().addListener((obs, oldVal, newVal) ->
            overlayLayer.resize(newVal.doubleValue(), rootPane.getHeight()));
        rootPane.heightProperty().addListener((obs, oldVal, newVal) ->
            overlayLayer.resize(rootPane.getWidth(), newVal.doubleValue()));

        // Initialize drag manager with tab group factory that sets up drag handlers
        dragManager = new DragManager(this::getRoot, overlayLayer, this::setRoot, themeProperty, this::createTabGroup);

        // Initialize layout factory
        layoutFactory = new LayoutFactory(themeProperty);
        treeService = new DockTreeService(this);
        sessionService = new DockSessionService(this, treeService);
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
     *
     * @return root container region
     */
    public Region getRootPane() {
        return mainContainer;
    }

    /**
     * Sets the owner stage for floating windows.
     * Recommended before using floating functionality.
     *
     * @param stage owner stage for floating windows
     */
    public void setOwnerStage(Stage stage) {
        this.ownerStage = stage;
        this.floatingWindowManager = new FloatingWindowManager(stage, themeProperty, this::createTabGroup);

        // Setup callbacks for floating window manager
        floatingWindowManager.setOnDockBack(this::dockLeaf);
        floatingWindowManager.setOnClose(this::closeLeaf);
    }

    boolean ensureFloatingWindowManager(String operation) {
        if (floatingWindowManager != null) {
            return true;
        }
        Stage stage = resolveOwnerStage();
        if (stage == null) {
            LOG.warning(() -> "Cannot " + operation
                + ": owner stage not set and no Stage is attached to the DockManager root. "
                + "Call setOwnerStage(stage) first.");
            return false;
        }
        setOwnerStage(stage);
        return true;
    }

    private Stage resolveOwnerStage() {
        if (ownerStage != null) {
            return ownerStage;
        }
        if (rootPane.getScene() == null) {
            return null;
        }
        if (rootPane.getScene().getWindow() instanceof Stage stage) {
            ownerStage = stage;
            return stage;
        }
        return null;
    }

    /**
     * Gets the current root dock element.
     *
     * @return current root dock element, or {@code null}
     */
    public DockElement getRoot() {
        return rootElement.get();
    }

    /**
     * Sets the root dock element.
     *
     * @param element new root dock element, or {@code null}
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
     *
     * @param layout layout definition to build and apply
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
     *
     * @return observable root element property
     */
    public ObjectProperty<DockElement> rootProperty() {
        return rootElement;
    }

    /**
     * Gets the theme property.
     *
     * @return observable theme property
     */
    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    /**
     * Gets the current theme.
     *
     * @return current theme
     */
    public Theme getTheme() {
        return themeProperty.get();
    }

    /**
     * Sets the theme.
     *
     * @param theme new theme
     */
    public void setTheme(Theme theme) {
        themeProperty.set(theme);
    }

    /**
     * Gets the layout factory for programmatic layout building.
     *
     * @return layout factory
     */
    public LayoutFactory getLayoutFactory() {
        return layoutFactory;
    }

    /**
     * Gets the floating window manager.
     *
     * @return floating window manager, or {@code null} until owner stage is set
     */
    public FloatingWindowManager getFloatingWindowManager() {
        return floatingWindowManager;
    }

    /**
     * Gets the minimized store.
     *
     * @return minimized leaf store
     */
    public MinimizedStore getMinimizedStore() {
        return minimizedStore;
    }

    /**
     * Sets the content factory for layout restoration.
     *
     * @param factory content factory used to rebuild leaf content
     */
    public void setContentFactory(ContentFactory factory) {
        this.contentFactory = factory;
        this.layoutFactory.setContentFactory(factory);
    }

    /**
     * Sets the content state registry for capture and restore.
     *
     * @param registry content state registry
     */
    public void setContentStateRegistry(ContentStateRegistry registry) {
        this.layoutFactory.setContentStateRegistry(registry);
    }

    /**
     * Gets the content state registry used for capture and restore.
     *
     * @return content state registry
     */
    public ContentStateRegistry getContentStateRegistry() {
        return layoutFactory.getContentStateRegistry();
    }

    /**
     * Creates a new leaf with the given title and content.
     *
     * @param title leaf title
     * @param content leaf content node
     * @return newly created leaf
     */
    public DockLeaf createLeaf(String title, Node content) {
        DockLeaf leaf = layoutFactory.createLeaf(title, content);
        setupLeafCloseHandler(leaf);
        return leaf;
    }

    /**
     * Sets up the close handler for a leaf.
     */
    void setupLeafCloseHandler(DockLeaf leaf) {
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
                treeService.removeElement(parent);
            }
        }

        leaf.dispose();
        floatingRestoreHints.remove(leaf.getMetadata().id());
    }

    /**
     * Creates a new tab group.
     *
     * @return newly created tab group with handlers wired
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
     *
     * @param first first child element
     * @param second second child element
     * @return horizontal split group
     */
    public DockSplitGroup createHorizontalSplit(DockElement first, DockElement second) {
        return layoutFactory.createHorizontalSplit(first, second);
    }

    /**
     * Creates a new horizontal split with custom divider position.
     *
     * @param first first child element
     * @param second second child element
     * @param dividerPosition divider position ratio
     * @return horizontal split group
     */
    public DockSplitGroup createHorizontalSplit(DockElement first, DockElement second, double dividerPosition) {
        return layoutFactory.createHorizontalSplit(first, second, dividerPosition);
    }

    /**
     * Creates a new vertical split.
     *
     * @param first first child element
     * @param second second child element
     * @return vertical split group
     */
    public DockSplitGroup createVerticalSplit(DockElement first, DockElement second) {
        return layoutFactory.createVerticalSplit(first, second);
    }

    /**
     * Creates a new vertical split with custom divider position.
     *
     * @param first first child element
     * @param second second child element
     * @param dividerPosition divider position ratio
     * @return vertical split group
     */
    public DockSplitGroup createVerticalSplit(DockElement first, DockElement second, double dividerPosition) {
        return layoutFactory.createVerticalSplit(first, second, dividerPosition);
    }

    /**
     * Captures the current layout as a LayoutNode for serialization.
     *
     * @return serialized layout tree, or {@code null} when no root is set
     */
    public LayoutNode capture() {
        return sessionService.captureLayout();
    }

    /**
     * Restores a layout from a LayoutNode.
     *
     * @param layout serialized layout to restore
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
        return sessionService.captureSession();
    }

    /**
     * Restores a session from DockSessionData.
     *
     * @param session the session to restore
     */
    public void restoreSession(DockSessionData session) {
        sessionService.restoreSession(session);
    }

    /**
     * Saves the current session to a JSON string.
     *
     * @return JSON representation of the current session, or empty string if no session
     * @throws DockSessionPersistence.SessionSerializationException if serialization fails
     */
    public String saveSessionToString() {
        return sessionService.saveSessionToString();
    }

    /**
     * Restores a session from a JSON string.
     *
     * @param json the JSON string containing the session
     * @throws DockSessionPersistence.SessionSerializationException if deserialization fails
     */
    public void restoreSessionFromString(String json) {
        sessionService.restoreSessionFromString(json);
    }

    /**
     * Saves the current session to a JSON file.
     *
     * @param path the file path to write to
     * @throws DockSessionPersistence.SessionSerializationException if serialization fails
     * @throws DockSessionPersistence.SessionFileIOException        if file I/O fails
     */
    public void saveSessionToFile(Path path) {
        sessionService.saveSessionToFile(path);
    }

    /**
     * Loads a session from a JSON file.
     *
     * @param path the file path to read from
     * @throws DockSessionPersistence.SessionFileIOException        if file I/O fails
     * @throws DockSessionPersistence.SessionSerializationException if deserialization fails
     */
    public void loadSessionFromFile(Path path) {
        sessionService.loadSessionFromFile(path);
    }

    /**
     * Restores a leaf as floating without overwriting its restore hint.
     * Used internally during session restoration.
     *
     * @param leaf        the leaf to float
     * @param restoreHint the restore hint for docking back
     * @param bounds      the window bounds, or null for default
     */
    boolean restoreFloating(DockLeaf leaf, RestoreHint restoreHint, Rectangle2D bounds) {
        if (!ensureFloatingWindowManager("restore floating leaf")) {
            return false;
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
        return true;
    }

    /**
     * Sets up drag handlers for a tab group.
     *
     * @param tabGroup tab group to wire with drag handlers
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
     *
     * @param leaf leaf to float
     */
    public void floatLeaf(DockLeaf leaf) {
        if (!ensureFloatingWindowManager("float leaf")) {
            return;
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
        treeService.removeLeafFromDock(leaf);

        // Update state
        updateLeafState(leaf, DockState.FLOATING);

        // Create floating window
        FloatingDockWindow window = floatingWindowManager.floatLeaf(leaf);
        window.show();
    }

    /**
     * Floats a leaf at a specific position.
     *
     * @param leaf leaf to float
     * @param x screen x position for the floating window
     * @param y screen y position for the floating window
     */
    public void floatLeaf(DockLeaf leaf, double x, double y) {
        if (!ensureFloatingWindowManager("float leaf")) {
            return;
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
        treeService.removeLeafFromDock(leaf);

        // Update state
        updateLeafState(leaf, DockState.FLOATING);

        // Create floating window at position
        floatingWindowManager.floatLeaf(leaf, x, y);
    }

    /**
     * Docks a floating leaf back into the dock tree.
     *
     * @param leaf leaf to dock
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

        if (hint != null && treeService.tryRestoreWithHint(leaf, hint)) {
            return; // Successfully restored to original position
        }

        // Fallback: insert at default position
        treeService.insertLeafIntoDock(leaf);
    }

    // === Minimize API ===

    /**
     * Minimizes a leaf, removing it from the dock tree and adding to minimized bar.
     *
     * @param leaf leaf to minimize
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
        treeService.removeLeafFromDock(leaf);

        // Update state
        updateLeafState(leaf, DockState.MINIMIZED);

        // Add to minimized store
        minimizedStore.addLeaf(leaf, hint);
    }

    /**
     * Restores a minimized leaf back into the dock tree.
     *
     * @param leaf leaf to restore
     */
    public void restoreLeaf(DockLeaf leaf) {
        if (!minimizedStore.isMinimized(leaf)) {
            return;
        }

        RestoreHint hint = minimizedStore.removeLeaf(leaf);

        // Update state
        updateLeafState(leaf, DockState.DOCKED);

        // Try to restore to original position, or fallback to default
        if (!treeService.tryRestoreWithHint(leaf, hint)) {
            treeService.insertLeafIntoDock(leaf);
        }
    }

    /**
     * Restores a minimized leaf by ID.
     *
     * @param leafId identifier of the leaf to restore
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
     *
     * @param leaf leaf to maximize
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
            DockElement target = treeService.findElementById(savedRootBeforeMaximize, maximizeRestoreHint.parentId());
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
                treeService.insertLeafIntoDock(leaf);
            }
        } else {
            // No hint - insert at default position
            treeService.insertLeafIntoDock(leaf);
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
     *
     * @return {@code true} when a leaf is currently maximized
     */
    public boolean isMaximized() {
        return maximizedLeaf != null;
    }

    /**
     * Gets the currently maximized leaf, if any.
     *
     * @return maximized leaf, or {@code null} when none
     */
    public DockLeaf getMaximizedLeaf() {
        return maximizedLeaf;
    }

    java.util.Map<String, RestoreHint> floatingRestoreHints() {
        return floatingRestoreHints;
    }

    RestoreHint maximizeRestoreHint() {
        return maximizeRestoreHint;
    }

    /**
     * Updates the state in leaf metadata.
     */
    void updateLeafState(DockLeaf leaf, DockState state) {
        DockData current = leaf.getMetadata();
        leaf.metadataProperty().set(current.withState(state));
    }

    /**
     * Disposes of the dock manager and all elements.
     */
    public void dispose() {
        if (themeProperty.isBound()) {
            themeProperty.unbind();
        }
        mainContainer.getProperties().remove(ROOT_PANE_MANAGER_PROPERTY);
        ownerStage = null;

        // Dispose floating windows
        if (floatingWindowManager != null) {
            floatingWindowManager.dispose();
            floatingWindowManager = null;
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
        contentFactory = null;
    }

    // === Fluent Builder API ===

    /**
     * Creates a new DockManager builder.
     *
     * @return new builder instance
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

        /**
         * Creates a builder with default configuration.
         */
        public Builder() {
        }

        /**
         * Sets the theme to use for the created manager.
         *
         * @param theme theme to apply
         * @return this builder
         */
        public Builder withTheme(Theme theme) {
            this.theme = theme;
            return this;
        }

        /**
         * Sets the initial layout to apply.
         *
         * @param layout initial layout
         * @return this builder
         */
        public Builder withLayout(LayoutNode layout) {
            this.layout = layout;
            return this;
        }

        /**
         * Sets the content factory for leaf restoration.
         *
         * @param factory content factory
         * @return this builder
         */
        public Builder withContentFactory(ContentFactory factory) {
            this.contentFactory = factory;
            return this;
        }

        /**
         * Builds a configured {@link DockManager}.
         *
         * @return configured dock manager
         */
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
