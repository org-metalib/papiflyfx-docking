package org.metalib.papifly.fx.docks;

import javafx.beans.value.ChangeListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.docking.api.ContentFactory;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.core.DockData;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockElementVisitor;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockState;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.drag.DragManager;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docks.layout.LayoutFactory;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.minimize.MinimizedStore;
import org.metalib.papifly.fx.docks.render.OverlayCanvas;
import org.metalib.papifly.fx.docks.serial.DockSessionPersistence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Central manager for the docking framework.
 * Acts as the main entry point and global event bus.
 * Supports floating windows, minimize bar, and maximize functionality.
 */
public class DockManager {

    public static final String ROOT_PANE_MANAGER_PROPERTY = DockManager.class.getName() + ".rootPaneManager";

    private final BorderPane mainContainer;
    private final StackPane rootPane;
    private final StackPane dockingLayer;
    private final OverlayCanvas overlayLayer;
    private final ObjectProperty<DockElement> rootElement;
    private final DockManagerContext serviceContext;
    private final DockThemeService themeService;
    private final DragManager dragManager;
    private final LayoutFactory layoutFactory;
    private final DockTreeService treeService;
    private final DockFloatingService floatingService;
    private final DockMinMaxService minMaxService;
    private final DockSessionService sessionService;
    private final ObjectProperty<RibbonContext> ribbonContext;
    private final Map<DockTabGroup, RibbonContextListenerHandle> ribbonContextListeners;
    private final LinkedHashSet<DockSessionStateContributor<?>> sessionStateContributors;

    private ContentFactory contentFactory;
    private DockTabGroup activeRibbonTabGroup;

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
        this(theme, DockManagerServices.defaults());
    }

    /**
     * Creates a new DockManager with explicit service wiring.
     *
     * @param theme initial dock theme
     * @param services service factories used to build the manager collaborators
     */
    public DockManager(Theme theme, DockManagerServices services) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(services, "services");

        this.rootElement = new SimpleObjectProperty<>();
        this.ribbonContext = new SimpleObjectProperty<>(RibbonContext.empty());
        this.ribbonContextListeners = new IdentityHashMap<>();
        this.sessionStateContributors = new LinkedHashSet<>();
        this.serviceContext = new ServiceContext();

        dockingLayer = new StackPane();
        dockingLayer.setMinSize(0, 0);
        overlayLayer = new OverlayCanvas();

        rootPane = new StackPane(dockingLayer, overlayLayer);
        rootPane.setMinSize(0, 0);

        mainContainer = new BorderPane();
        mainContainer.setCenter(rootPane);
        mainContainer.setMinSize(0, 0);
        mainContainer.getProperties().put(ROOT_PANE_MANAGER_PROPERTY, this);

        rootPane.widthProperty().addListener((obs, oldVal, newVal) ->
            overlayLayer.resize(newVal.doubleValue(), rootPane.getHeight()));
        rootPane.heightProperty().addListener((obs, oldVal, newVal) ->
            overlayLayer.resize(rootPane.getWidth(), newVal.doubleValue()));

        themeService = services.themeServiceFactory().create(theme, dockingLayer);
        layoutFactory = new LayoutFactory(themeProperty());
        treeService = new DockTreeService(this);
        floatingService = services.floatingServiceFactory().create(serviceContext);
        minMaxService = services.minMaxServiceFactory().create(serviceContext, floatingService);
        sessionService = services.sessionServiceFactory().create(serviceContext, treeService, floatingService, minMaxService);
        mainContainer.setBottom(minMaxService.getMinimizedBar());

        dragManager = new DragManager(this::getRoot, overlayLayer, this::setRoot, themeProperty(), this::createTabGroup);
        setupDragHandlers();
    }

    /**
     * Creates a new default manager for the given theme.
     *
     * @param theme initial dock theme
     * @return a manager with the default service wiring
     */
    public static DockManager createDefault(Theme theme) {
        return new DockManager(theme);
    }

    /**
     * Creates a new default manager using the dark theme.
     *
     * @return a manager with the default service wiring
     */
    public static DockManager createDefault() {
        return new DockManager();
    }

    private void setupDragHandlers() {
        rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, this::updateRibbonContextFromMouseTarget);
        rootPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (dragManager.hasDragContext()) {
                dragManager.onDrag(event);
            }
        });

        rootPane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (dragManager.hasDragContext()) {
                dragManager.endDrag(event);
            }
        });
    }

    private void updateRibbonContextFromMouseTarget(MouseEvent event) {
        Object target = event.getTarget();
        if (!(target instanceof Node node)) {
            return;
        }
        DockTabGroup targetGroup = resolveTabGroupForNode(node);
        if (targetGroup == null || targetGroup.getActiveTab() == null) {
            return;
        }
        activeRibbonTabGroup = targetGroup;
        syncRibbonContextFromTree();
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
        floatingService.setOwnerStage(stage);
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
            registerRibbonContextListeners(element);
        }
        syncRibbonContextFromTree();
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
        element.accept(new DockElementVisitor<>() {
            @Override
            public Void visitTabGroup(DockTabGroup tabGroup) {
                setupTabGroupDragHandlers(tabGroup);
                setupTabGroupButtonHandlers(tabGroup);
                trackRibbonContext(tabGroup);
                for (DockLeaf leaf : tabGroup.getTabs()) {
                    setupLeafCloseHandler(leaf);
                }
                return null;
            }

            @Override
            public Void visitSplitGroup(DockSplitGroup splitGroup) {
                wireHandlers(splitGroup.getFirst());
                wireHandlers(splitGroup.getSecond());
                return null;
            }
        });
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
        return themeService.themeProperty();
    }

    /**
     * Gets the current theme.
     *
     * @return current theme
     */
    public Theme getTheme() {
        return themeService.getTheme();
    }

    /**
     * Sets the theme.
     *
     * @param theme new theme
     */
    public void setTheme(Theme theme) {
        themeService.setTheme(theme);
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
    public org.metalib.papifly.fx.docks.floating.FloatingWindowManager getFloatingWindowManager() {
        return floatingService.getFloatingWindowManager();
    }

    /**
     * Gets the minimized store.
     *
     * @return minimized leaf store
     */
    public MinimizedStore getMinimizedStore() {
        return minMaxService.getMinimizedStore();
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
     * Gets the current ribbon context derived from the active dock/content.
     *
     * @return current ribbon context
     */
    public RibbonContext getRibbonContext() {
        return ribbonContext.get();
    }

    /**
     * Gets the observable ribbon context property derived from dock activity.
     *
     * @return ribbon context property
     */
    public ObjectProperty<RibbonContext> ribbonContextProperty() {
        return ribbonContext;
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
        restoreMaximizedIfNecessary(leaf);

        if (floatingService.isFloating(leaf)) {
            floatingService.unfloatLeaf(leaf);
        }

        minMaxService.removeMinimizedLeaf(leaf);

        DockTabGroup parent = leaf.getParent();
        if (parent != null) {
            parent.removeLeaf(leaf);
            if (parent.getTabs().isEmpty()) {
                treeService.removeElement(parent);
            }
        }

        leaf.dispose();
        floatingService.forgetRestoreHint(leaf.getMetadata().id());
        syncRibbonContextFromTree();
    }

    /**
     * Creates a new tab group.
     *
     * @return newly created tab group with handlers wired
     */
    public DockTabGroup createTabGroup() {
        DockTabGroup tabGroup = new DockTabGroup(themeProperty());
        setupTabGroupDragHandlers(tabGroup);
        setupTabGroupButtonHandlers(tabGroup);
        trackRibbonContext(tabGroup);
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
     * @throws DockSessionPersistence.SessionFileIOException if file I/O fails
     */
    public void saveSessionToFile(Path path) {
        sessionService.saveSessionToFile(path);
    }

    /**
     * Loads a session from a JSON file.
     *
     * @param path the file path to read from
     * @throws DockSessionPersistence.SessionFileIOException if file I/O fails
     * @throws DockSessionPersistence.SessionSerializationException if deserialization fails
     */
    public void loadSessionFromFile(Path path) {
        sessionService.loadSessionFromFile(path);
    }

    /**
     * Registers a session-state contributor used during capture and restore.
     *
     * @param contributor contributor implementation
     */
    public void registerSessionStateContributor(DockSessionStateContributor<?> contributor) {
        if (contributor == null) {
            return;
        }
        String namespace = requireSessionContributorNamespace(contributor);
        for (DockSessionStateContributor<?> existing : sessionStateContributors) {
            if (existing != contributor && namespace.equals(requireSessionContributorNamespace(existing))) {
                throw new IllegalArgumentException("Duplicate dock session contributor namespace: " + namespace);
            }
        }
        sessionStateContributors.add(contributor);
    }

    /**
     * Removes a previously registered session-state contributor.
     *
     * @param contributor contributor implementation
     */
    public void unregisterSessionStateContributor(DockSessionStateContributor<?> contributor) {
        if (contributor != null) {
            sessionStateContributors.remove(contributor);
        }
    }

    /**
     * Returns a snapshot of registered session-state contributors.
     *
     * @return registered contributors
     */
    public List<DockSessionStateContributor<?>> getSessionStateContributors() {
        return List.copyOf(sessionStateContributors);
    }

    private String requireSessionContributorNamespace(DockSessionStateContributor<?> contributor) {
        String namespace = Objects.requireNonNull(
            contributor.extensionNamespace(),
            "Session contributor namespace must not be null: " + contributor.getClass().getName()
        ).trim();
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException(
                "Session contributor namespace must not be blank: " + contributor.getClass().getName()
            );
        }
        return namespace;
    }

    /**
     * Sets up drag handlers for a tab group.
     *
     * @param tabGroup tab group to wire with drag handlers
     */
    public void setupTabGroupDragHandlers(DockTabGroup tabGroup) {
        tabGroup.getTabBar().addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            Node target = event.getPickResult().getIntersectedNode();
            while (target != null && target != tabGroup.getTabBar()) {
                if (target.getUserData() instanceof DockLeaf leaf) {
                    dragManager.startDrag(leaf, event);
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
            if (wasDragging) {
                event.consume();
            }
        });
    }

    /**
     * Floats a leaf from the dock tree into a floating window.
     *
     * @param leaf leaf to float
     */
    public void floatLeaf(DockLeaf leaf) {
        floatingService.floatLeaf(leaf);
        syncRibbonContextFromTree();
    }

    /**
     * Floats a leaf at a specific position.
     *
     * @param leaf leaf to float
     * @param x screen x position for the floating window
     * @param y screen y position for the floating window
     */
    public void floatLeaf(DockLeaf leaf, double x, double y) {
        floatingService.floatLeaf(leaf, x, y);
        syncRibbonContextFromTree();
    }

    /**
     * Docks a floating leaf back into the dock tree.
     *
     * @param leaf leaf to dock
     */
    public void dockLeaf(DockLeaf leaf) {
        floatingService.dockLeaf(leaf);
        syncRibbonContextFromTree();
    }

    /**
     * Minimizes a leaf, removing it from the dock tree and adding to minimized bar.
     *
     * @param leaf leaf to minimize
     */
    public void minimizeLeaf(DockLeaf leaf) {
        minMaxService.minimizeLeaf(leaf);
        syncRibbonContextFromTree();
    }

    /**
     * Restores a minimized leaf back into the dock tree.
     *
     * @param leaf leaf to restore
     */
    public void restoreLeaf(DockLeaf leaf) {
        minMaxService.restoreLeaf(leaf);
        syncRibbonContextFromTree();
    }

    /**
     * Restores a minimized leaf by ID.
     *
     * @param leafId identifier of the leaf to restore
     */
    public void restoreLeaf(String leafId) {
        minMaxService.restoreLeaf(leafId);
        syncRibbonContextFromTree();
    }

    /**
     * Maximizes a leaf to fill the entire dock area.
     * The previous layout is preserved and restored when unmaximizing.
     *
     * @param leaf leaf to maximize
     */
    public void maximizeLeaf(DockLeaf leaf) {
        minMaxService.maximizeLeaf(leaf);
        syncRibbonContextFromTree();
    }

    /**
     * Restores from maximized state to previous layout.
     */
    public void restoreMaximized() {
        minMaxService.restoreMaximized();
        syncRibbonContextFromTree();
    }

    /**
     * Checks if any leaf is currently maximized.
     *
     * @return {@code true} when a leaf is currently maximized
     */
    public boolean isMaximized() {
        return minMaxService.isMaximized();
    }

    /**
     * Gets the currently maximized leaf, if any.
     *
     * @return maximized leaf, or {@code null} when none
     */
    public DockLeaf getMaximizedLeaf() {
        return minMaxService.getMaximizedLeaf();
    }

    /**
     * Updates the state in leaf metadata.
     */
    void updateLeafState(DockLeaf leaf, DockState state) {
        DockData current = leaf.getMetadata();
        leaf.metadataProperty().set(current.withState(state));
    }

    private void restoreMaximizedIfNecessary(DockLeaf leaf) {
        if (minMaxService.getMaximizedLeaf() == leaf) {
            minMaxService.restoreMaximized();
        }
    }

    /**
     * Disposes of the dock manager and all elements.
     */
    public void dispose() {
        mainContainer.getProperties().remove(ROOT_PANE_MANAGER_PROPERTY);

        ribbonContextListeners.forEach(DockManager::detachRibbonContextListener);
        ribbonContextListeners.clear();
        activeRibbonTabGroup = null;
        floatingService.dispose();
        minMaxService.dispose();
        themeService.dispose();
        sessionStateContributors.clear();

        DockElement root = rootElement.get();
        if (root != null) {
            root.dispose();
        }
        rootElement.set(null);
        dockingLayer.getChildren().clear();
        contentFactory = null;
    }

    private void trackRibbonContext(DockTabGroup tabGroup) {
        if (tabGroup == null || ribbonContextListeners.containsKey(tabGroup)) {
            return;
        }
        ChangeListener<Number> activeTabListener = (obs, oldValue, newValue) -> {
            if (tabGroup.getActiveTab() != null) {
                activeRibbonTabGroup = tabGroup;
            } else if (activeRibbonTabGroup == tabGroup) {
                activeRibbonTabGroup = null;
            }
            syncRibbonContextFromTree();
        };
        EventHandler<MouseEvent> mousePressedListener = event -> {
            if (tabGroup.getActiveTab() != null) {
                activeRibbonTabGroup = tabGroup;
            }
            syncRibbonContextFromTree();
        };
        tabGroup.activeTabIndexProperty().addListener(activeTabListener);
        tabGroup.getNode().addEventFilter(MouseEvent.MOUSE_PRESSED, mousePressedListener);
        ribbonContextListeners.put(tabGroup, new RibbonContextListenerHandle(activeTabListener, mousePressedListener));
        if (activeRibbonTabGroup == null && tabGroup.getActiveTab() != null) {
            activeRibbonTabGroup = tabGroup;
        }
    }

    private static void detachRibbonContextListener(DockTabGroup tabGroup, RibbonContextListenerHandle handle) {
        if (tabGroup == null || handle == null) {
            return;
        }
        if (handle.activeTabListener() != null) {
            tabGroup.activeTabIndexProperty().removeListener(handle.activeTabListener());
        }
        if (handle.mousePressedListener() != null) {
            tabGroup.getNode().removeEventFilter(MouseEvent.MOUSE_PRESSED, handle.mousePressedListener());
        }
    }

    private void syncRibbonContextFromTree() {
        cleanupStaleRibbonContextListeners();
        ribbonContext.set(buildRibbonContext(resolveActiveRibbonLeaf()));
    }

    private DockLeaf resolveActiveRibbonLeaf() {
        if (!isActiveRibbonTabGroupValid()) {
            activeRibbonTabGroup = null;
        }

        DockLeaf activeLeaf = activeRibbonTabGroup == null ? null : activeRibbonTabGroup.getActiveTab();
        if (activeLeaf != null) {
            return activeLeaf;
        }
        DockTabGroup focusedGroup = resolveFocusedRibbonTabGroup();
        if (focusedGroup != null && focusedGroup.getActiveTab() != null) {
            activeRibbonTabGroup = focusedGroup;
            return focusedGroup.getActiveTab();
        }
        DockElement root = rootElement.get();
        if (root == null) {
            return null;
        }
        return root.accept(new DockElementVisitor<>() {
            @Override
            public DockLeaf visitTabGroup(DockTabGroup tabGroup) {
                DockLeaf groupActive = tabGroup.getActiveTab();
                if (groupActive != null) {
                    activeRibbonTabGroup = tabGroup;
                }
                return groupActive;
            }

            @Override
            public DockLeaf visitSplitGroup(DockSplitGroup splitGroup) {
                DockElement firstChild = splitGroup.getFirst();
                if (firstChild != null) {
                    DockLeaf first = firstChild.accept(this);
                    if (first != null) {
                        return first;
                    }
                }
                DockElement secondChild = splitGroup.getSecond();
                return secondChild == null ? null : secondChild.accept(this);
            }
        });
    }

    private RibbonContext buildRibbonContext(DockLeaf leaf) {
        if (leaf == null) {
            return RibbonContext.empty();
        }

        DockData metadata = leaf.getMetadata();
        LeafContentData contentData = leaf.getContentData();
        String contentId = contentData != null && contentData.contentId() != null
            ? contentData.contentId()
            : metadata.id();
        String contentTypeKey = contentData != null && contentData.typeKey() != null
            ? contentData.typeKey()
            : leaf.getContentFactoryId();

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put(RibbonContextAttributes.DOCK_TITLE, metadata.title());
        attributes.put(RibbonContextAttributes.DOCK_STATE, metadata.state().name());
        attributes.put(RibbonContextAttributes.FLOATING, floatingService.isFloating(leaf));
        attributes.put(RibbonContextAttributes.MAXIMIZED, minMaxService.getMaximizedLeaf() == leaf);
        if (leaf.getContentFactoryId() != null) {
            attributes.put(RibbonContextAttributes.CONTENT_FACTORY_ID, leaf.getContentFactoryId());
        }
        if (leaf.getParent() != null) {
            attributes.put(RibbonContextAttributes.DOCK_GROUP_ID, leaf.getParent().getMetadata().id());
        }
        if (contentData != null) {
            attributes.put(RibbonContextAttributes.CONTENT_VERSION, contentData.version());
            if (contentData.state() != null && !contentData.state().isEmpty()) {
                attributes.put(RibbonContextAttributes.CONTENT_STATE, new LinkedHashMap<>(contentData.state()));
            }
        }
        Map<Class<?>, Object> capabilities = new LinkedHashMap<>();
        Node contentNode = leaf.getContent();
        if (contentNode != null) {
            // Ribbon 2: still expose ACTIVE_CONTENT_NODE as a transitional bridge.
            // The deprecated attribute will be removed once provider migration completes.
            attributes.put(RibbonContextAttributes.ACTIVE_CONTENT_NODE, contentNode);
            // Register the content node under its concrete class so providers can
            // resolve typed capabilities through RibbonContext#capability(Class).
            capabilities.put(contentNode.getClass(), contentNode);
        }

        return new RibbonContext(metadata.id(), contentId, contentTypeKey, attributes, capabilities);
    }

    private void cleanupStaleRibbonContextListeners() {
        DockElement root = rootElement.get();
        ribbonContextListeners.entrySet().removeIf(entry -> {
            DockTabGroup group = entry.getKey();
            if (group == null) {
                return true;
            }
            boolean attachedToRoot = isTabGroupInRoot(root, group);
            boolean floating = isTabGroupFloating(group);
            if (attachedToRoot || floating) {
                return false;
            }
            detachRibbonContextListener(group, entry.getValue());
            if (activeRibbonTabGroup == group) {
                activeRibbonTabGroup = null;
            }
            return true;
        });
    }

    private DockTabGroup resolveFocusedRibbonTabGroup() {
        DockTabGroup focused = findGroupForFocusOwner(rootPane.getScene());
        if (focused != null) {
            return focused;
        }
        org.metalib.papifly.fx.docks.floating.FloatingWindowManager floatingWindowManager = floatingService.getFloatingWindowManager();
        if (floatingWindowManager == null) {
            return null;
        }
        return floatingWindowManager.getFloatingWindows().stream()
            .map(org.metalib.papifly.fx.docks.floating.FloatingDockWindow::getStage)
            .filter(Stage::isFocused)
            .map(Stage::getScene)
            .map(this::findGroupForFocusOwner)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private DockTabGroup findGroupForFocusOwner(Scene scene) {
        if (scene == null) {
            return null;
        }
        Node focusOwner = scene.getFocusOwner();
        if (focusOwner == null) {
            return null;
        }
        for (DockTabGroup tabGroup : ribbonContextListeners.keySet()) {
            if (tabGroup != null && isDescendantOf(focusOwner, tabGroup.getNode())) {
                return tabGroup;
            }
        }
        return null;
    }

    private DockTabGroup resolveTabGroupForNode(Node node) {
        if (node == null) {
            return null;
        }
        for (DockTabGroup tabGroup : ribbonContextListeners.keySet()) {
            if (tabGroup != null && isDescendantOf(node, tabGroup.getNode())) {
                return tabGroup;
            }
        }
        return null;
    }

    private boolean isActiveRibbonTabGroupValid() {
        if (activeRibbonTabGroup == null) {
            return false;
        }
        DockLeaf activeLeaf = activeRibbonTabGroup.getActiveTab();
        if (activeLeaf == null) {
            return false;
        }
        if (floatingService.isFloating(activeLeaf)) {
            return true;
        }
        return isLeafInRoot(rootElement.get(), activeLeaf);
    }

    private boolean isTabGroupFloating(DockTabGroup tabGroup) {
        return tabGroup.getTabs().stream().anyMatch(floatingService::isFloating);
    }

    private static boolean isDescendantOf(Node node, Node ancestor) {
        Node current = node;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean isTabGroupInRoot(DockElement root, DockTabGroup target) {
        if (root == null || target == null) {
            return false;
        }
        return root.accept(new DockElementVisitor<>() {
            @Override
            public Boolean visitTabGroup(DockTabGroup tabGroup) {
                return tabGroup == target;
            }

            @Override
            public Boolean visitSplitGroup(DockSplitGroup splitGroup) {
                DockElement first = splitGroup.getFirst();
                if (first != null && first.accept(this)) {
                    return true;
                }
                DockElement second = splitGroup.getSecond();
                return second != null && second.accept(this);
            }
        });
    }

    private void registerRibbonContextListeners(DockElement element) {
        if (element == null) {
            return;
        }
        element.accept(new DockElementVisitor<>() {
            @Override
            public Void visitTabGroup(DockTabGroup tabGroup) {
                trackRibbonContext(tabGroup);
                return null;
            }

            @Override
            public Void visitSplitGroup(DockSplitGroup splitGroup) {
                registerRibbonContextListeners(splitGroup.getFirst());
                registerRibbonContextListeners(splitGroup.getSecond());
                return null;
            }
        });
    }

    private static boolean isLeafInRoot(DockElement root, DockLeaf target) {
        if (root == null || target == null) {
            return false;
        }
        return root.accept(new DockElementVisitor<>() {
            @Override
            public Boolean visitTabGroup(DockTabGroup tabGroup) {
                return tabGroup.getTabs().contains(target);
            }

            @Override
            public Boolean visitSplitGroup(DockSplitGroup splitGroup) {
                DockElement first = splitGroup.getFirst();
                if (first != null && first.accept(this)) {
                    return true;
                }
                DockElement second = splitGroup.getSecond();
                return second != null && second.accept(this);
            }
        });
    }

    private record RibbonContextListenerHandle(
        ChangeListener<Number> activeTabListener,
        EventHandler<MouseEvent> mousePressedListener
    ) {
    }

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
        private DockManagerServices services = DockManagerServices.defaults();

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
         * Sets the service wiring to use for the created manager.
         *
         * @param services service factories
         * @return this builder
         */
        public Builder withServices(DockManagerServices services) {
            this.services = services;
            return this;
        }

        /**
         * Builds a configured {@link DockManager}.
         *
         * @return configured dock manager
         */
        public DockManager build() {
            DockManager manager = new DockManager(theme, services);
            if (contentFactory != null) {
                manager.setContentFactory(contentFactory);
            }
            if (layout != null) {
                manager.setRoot(layout);
            }
            return manager;
        }
    }

    private final class ServiceContext implements DockManagerContext {

        @Override
        public DockElement getRoot() {
            return DockManager.this.getRoot();
        }

        @Override
        public void setRoot(DockElement element) {
            DockManager.this.setRoot(element);
        }

        @Override
        public void restore(LayoutNode layout) {
            DockManager.this.restore(layout);
        }

        @Override
        public ObjectProperty<DockElement> rootProperty() {
            return DockManager.this.rootProperty();
        }

        @Override
        public ObjectProperty<Theme> themeProperty() {
            return DockManager.this.themeProperty();
        }

        @Override
        public LayoutFactory getLayoutFactory() {
            return DockManager.this.getLayoutFactory();
        }

        @Override
        public DockTreeService getTreeService() {
            return treeService;
        }

        @Override
        public DockTabGroup createTabGroup() {
            return DockManager.this.createTabGroup();
        }

        @Override
        public void setupLeafCloseHandler(DockLeaf leaf) {
            DockManager.this.setupLeafCloseHandler(leaf);
        }

        @Override
        public void updateLeafState(DockLeaf leaf, DockState state) {
            DockManager.this.updateLeafState(leaf, state);
        }

        @Override
        public void restoreMaximizedIfNecessary(DockLeaf leaf) {
            DockManager.this.restoreMaximizedIfNecessary(leaf);
        }

        @Override
        public void closeLeaf(DockLeaf leaf) {
            DockManager.this.closeLeaf(leaf);
        }

        @Override
        public StackPane getDockingLayer() {
            return dockingLayer;
        }

        @Override
        public StackPane getRootStack() {
            return rootPane;
        }

        @Override
        public List<DockSessionStateContributor<?>> getSessionStateContributors() {
            return new ArrayList<>(sessionStateContributors);
        }
    }
}
