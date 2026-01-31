package org.metalib.papifly.fx.docks.core;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;
import org.metalib.papifly.fx.docks.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages a stack of DockLeaf objects with a custom tab header.
 */
public class DockTabGroup implements DockElement {

    private final String id;
    private final BorderPane container;
    private final HBox tabBar;
    private final StackPane contentArea;
    private final ObjectProperty<Theme> themeProperty;
    private final ObservableList<DockLeaf> tabs;
    private final ObservableList<DockLeaf> tabsView;
    private final IntegerProperty activeTabIndex;

    private DockElement parent;
    private Consumer<DockLeaf> onTabClose;
    private Consumer<DockTabGroup> onGroupEmpty;
    private ChangeListener<Theme> themeListener;
    private ListChangeListener<DockLeaf> tabsListener;

    // Window control buttons (apply to active tab)
    private final HBox tabsContainer;
    private final HBox buttonContainer;
    private final StackPane floatButton;
    private final StackPane minimizeButton;
    private final StackPane maximizeButton;
    private final SVGPath floatIcon;
    private final SVGPath maximizeIcon;

    private Runnable onFloat;
    private Runnable onMinimize;
    private Runnable onMaximize;
    private boolean isFloating = false;
    private boolean isMaximized = false;

    public DockTabGroup(ObjectProperty<Theme> themeProperty) {
        this(UUID.randomUUID().toString(), themeProperty);
    }

    public DockTabGroup(String id, ObjectProperty<Theme> themeProperty) {
        this.id = id;
        this.themeProperty = themeProperty;
        this.tabs = FXCollections.observableArrayList();
        this.tabsView = FXCollections.unmodifiableObservableList(tabs);
        this.activeTabIndex = new SimpleIntegerProperty(-1);

        // Container for tab labels
        tabsContainer = new HBox(2);
        tabsContainer.setAlignment(Pos.CENTER_LEFT);

        // Create window control buttons
        floatButton = createFloatButton();
        floatIcon = (SVGPath) floatButton.getChildren().get(0);

        minimizeButton = createMinimizeButton();

        maximizeButton = createMaximizeButton();
        maximizeIcon = (SVGPath) maximizeButton.getChildren().get(0);

        buttonContainer = new HBox(4, floatButton, minimizeButton, maximizeButton);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        tabBar = new HBox(2, tabsContainer, spacer, buttonContainer);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(0, 4, 0, 4));

        contentArea = new StackPane();
        contentArea.setMinSize(0, 0); // Allow shrinking

        container = new BorderPane();
        container.setTop(tabBar);
        container.setCenter(contentArea);
        container.setMinSize(0, 0); // Allow shrinking

        // Listen for tab list changes
        tabsListener = change -> {
            rebuildTabBar();
            // Auto-select first tab if none selected
            if (activeTabIndex.get() < 0 && !tabs.isEmpty()) {
                setActiveTab(0);
            }
            // Handle group becoming empty
            if (tabs.isEmpty() && onGroupEmpty != null) {
                onGroupEmpty.accept(this);
            }
        };
        tabs.addListener(tabsListener);

        // Listen for active tab changes
        activeTabIndex.addListener((obs, oldVal, newVal) -> updateContent());

        // Apply theme
        themeListener = (obs, oldTheme, newTheme) -> applyTheme(newTheme);
        applyTheme(themeProperty.get());
        themeProperty.addListener(themeListener);
    }

    private void rebuildTabBar() {
        tabsContainer.getChildren().clear();

        for (int i = 0; i < tabs.size(); i++) {
            DockLeaf leaf = tabs.get(i);
            HBox tab = createTab(leaf, i);
            tabsContainer.getChildren().add(tab);
        }
    }

    /**
     * Creates interactive tab with label and close button
     */
    private HBox createTab(DockLeaf leaf, int index) {
        Label label = new Label();
        label.textProperty().bind(leaf.metadataProperty().map(DockData::title));

        Region closeBtn = createTabCloseButton();
        closeBtn.setOnMouseClicked(e -> {
            e.consume();
            leaf.requestClose();
        });

        HBox tab = new HBox(4, label, closeBtn);
        tab.setAlignment(Pos.CENTER);
        tab.setPadding(new Insets(4, 8, 4, 8));
        tab.setCursor(Cursor.HAND);

        tab.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                Platform.runLater(() -> {
                    setActiveTab(tabs.indexOf(leaf));
                    Node content = leaf.getContent();
                    if (content != null) {
                        content.requestFocus();
                    }
                });
            }
        });

        // Store reference for drag handling
        tab.setUserData(leaf);

        applyTabStyle(tab, label, index == activeTabIndex.get());

        return tab;
    }

    private Region createTabCloseButton() {
        SVGPath path = new SVGPath();
        path.setContent("M0,0 L6,6 M6,0 L0,6");
        path.setStroke(Color.GRAY);
        path.setStrokeWidth(1.2);
        path.setFill(Color.TRANSPARENT);

        StackPane button = new StackPane(path);
        button.setPrefSize(12, 12);
        button.setMaxSize(12, 12);
        button.setCursor(Cursor.HAND);

        button.setOnMouseEntered(e -> path.setStroke(Color.WHITE));
        button.setOnMouseExited(e -> path.setStroke(Color.GRAY));

        return button;
    }

    private StackPane createFloatButton() {
        SVGPath path = new SVGPath();
        // Float icon: window with arrow pointing out
        path.setContent("M2,4 L2,10 L10,10 L10,4 Z M4,2 L4,4 M4,2 L8,2 L8,4");
        path.setStroke(Color.GRAY);
        path.setStrokeWidth(1.2);
        path.setFill(Color.TRANSPARENT);

        StackPane button = new StackPane(path);
        button.setPrefSize(16, 16);
        button.setMaxSize(16, 16);
        button.setCursor(Cursor.HAND);

        button.setOnMouseEntered(e -> path.setStroke(Color.WHITE));
        button.setOnMouseExited(e -> path.setStroke(Color.GRAY));
        button.setOnMouseClicked(e -> {
            e.consume();
            if (onFloat != null) {
                onFloat.run();
            }
        });

        return button;
    }

    private StackPane createMinimizeButton() {
        SVGPath path = new SVGPath();
        // Minimize icon: horizontal line at bottom
        path.setContent("M2,8 L10,8");
        path.setStroke(Color.GRAY);
        path.setStrokeWidth(1.5);
        path.setFill(Color.TRANSPARENT);

        StackPane button = new StackPane(path);
        button.setPrefSize(16, 16);
        button.setMaxSize(16, 16);
        button.setCursor(Cursor.HAND);

        button.setOnMouseEntered(e -> path.setStroke(Color.WHITE));
        button.setOnMouseExited(e -> path.setStroke(Color.GRAY));
        button.setOnMouseClicked(e -> {
            e.consume();
            if (onMinimize != null) {
                onMinimize.run();
            }
        });

        return button;
    }

    private StackPane createMaximizeButton() {
        SVGPath path = new SVGPath();
        // Maximize icon: square window
        path.setContent("M2,2 L10,2 L10,10 L2,10 Z");
        path.setStroke(Color.GRAY);
        path.setStrokeWidth(1.2);
        path.setFill(Color.TRANSPARENT);

        StackPane button = new StackPane(path);
        button.setPrefSize(16, 16);
        button.setMaxSize(16, 16);
        button.setCursor(Cursor.HAND);

        button.setOnMouseEntered(e -> path.setStroke(Color.WHITE));
        button.setOnMouseExited(e -> path.setStroke(Color.GRAY));
        button.setOnMouseClicked(e -> {
            e.consume();
            if (onMaximize != null) {
                onMaximize.run();
            }
        });

        return button;
    }

    private void applyTabStyle(HBox tab, Label label, boolean active) {
        Theme theme = themeProperty.get();
        if (theme == null) return;

        tab.setBackground(new Background(new BackgroundFill(
            active ? theme.headerBackgroundActive() : theme.headerBackground(),
            new CornerRadii(theme.cornerRadius(), theme.cornerRadius(), 0, 0, false),
            Insets.EMPTY
        )));

        label.setFont(theme.headerFont());
        label.setTextFill(active ? theme.textColorActive() : theme.textColor());
    }

    private void updateContent() {
        contentArea.getChildren().clear();

        int index = activeTabIndex.get();
        if (index >= 0 && index < tabs.size()) {
            DockLeaf leaf = tabs.get(index);
            if (leaf.getContent() != null) {
                Node content = leaf.getContent();
                if (content instanceof Region region) {
                    region.setMinSize(0, 0); // Allow shrinking
                }
                contentArea.getChildren().add(content);
            }
        }

        // Update tab visual states
        rebuildTabBar();
    }

    private void applyTheme(Theme theme) {
        if (theme == null) return;

        tabBar.setBackground(new Background(new BackgroundFill(
            theme.headerBackground(),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        tabBar.setPrefHeight(theme.tabHeight());
        tabBar.setMinHeight(theme.tabHeight());
        tabBar.setMaxHeight(theme.tabHeight());

        contentArea.setBackground(new Background(new BackgroundFill(
            theme.background(),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        contentArea.setPadding(theme.contentPadding());

        container.setBorder(new Border(new BorderStroke(
            theme.borderColor(),
            BorderStrokeStyle.SOLID,
            new CornerRadii(theme.cornerRadius()),
            new BorderWidths(theme.borderWidth())
        )));

        rebuildTabBar();
    }

    /**
     * Adds a leaf to this tab group.
     */
    public void addLeaf(DockLeaf leaf) {
        leaf.setParent(this);
        // Close handler is managed by DockManager
        tabs.add(leaf);

        // Activate the new tab
        setActiveTab(tabs.size() - 1);
    }

    /**
     * Inserts a leaf at a specific index.
     */
    public void addLeaf(int index, DockLeaf leaf) {
        leaf.setParent(this);
        // Close handler is managed by DockManager
        tabs.add(index, leaf);
        setActiveTab(index);
    }

    /**
     * Reorders an existing tab within this group.
     */
    public void moveLeaf(int fromIndex, int toIndex) {
        if (fromIndex == toIndex || fromIndex < 0 || fromIndex >= tabs.size() || toIndex < 0 || toIndex > tabs.size()) {
            return;
        }
        DockLeaf leaf = tabs.remove(fromIndex);
        tabs.add(toIndex, leaf);
        setActiveTab(toIndex);
    }

    /**
     * Removes a leaf from this tab group.
     */
    public void removeLeaf(DockLeaf leaf) {
        int index = tabs.indexOf(leaf);
        if (index >= 0) {
            int previousActiveIndex = activeTabIndex.get();
            tabs.remove(index);
            leaf.setParent(null);

            // Adjust active tab index
            if (tabs.isEmpty()) {
                activeTabIndex.set(-1);
            } else if (index <= activeTabIndex.get()) {
                int newActiveIndex = Math.max(0, activeTabIndex.get() - 1);
                setActiveTab(newActiveIndex);

                // If the active index value hasn't changed but the tab at that index has changed,
                // force a content refresh since the listener won't fire
                if (newActiveIndex == previousActiveIndex && !tabs.isEmpty()) {
                    refreshActiveTabContent();
                }
            }

            if (onTabClose != null) {
                onTabClose.accept(leaf);
            }
        }
    }

    /**
     * Sets the active tab by index.
     */
    public void setActiveTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeTabIndex.set(index);
        }
    }

    /**
     * Sets the active tab by leaf.
     */
    public void setActiveTab(DockLeaf leaf) {
        int index = tabs.indexOf(leaf);
        if (index >= 0) {
            setActiveTab(index);
        }
    }

    /**
     * Forces a content refresh for the active tab.
     * Use this when the content may have been moved to another container
     * (e.g., during maximize) and needs to be restored to this tab group.
     */
    public void refreshActiveTabContent() {
        updateContent();
    }

    /**
     * Gets the active tab.
     */
    public DockLeaf getActiveTab() {
        int index = activeTabIndex.get();
        return (index >= 0 && index < tabs.size()) ? tabs.get(index) : null;
    }

    /**
     * Gets the active tab index.
     */
    public int getActiveTabIndex() {
        return activeTabIndex.get();
    }

    /**
     * Gets the tab list (unmodifiable view).
     */
    public ObservableList<DockLeaf> getTabs() {
        return tabsView;
    }

    /**
     * Sets the handler for tab close events.
     */
    public void setOnTabClose(Consumer<DockLeaf> handler) {
        this.onTabClose = handler;
    }

    /**
     * Sets the handler for when the group becomes empty.
     */
    public void setOnGroupEmpty(Consumer<DockTabGroup> handler) {
        this.onGroupEmpty = handler;
    }

    /**
     * Gets the tab bar for drag handling.
     */
    public HBox getTabBar() {
        return tabBar;
    }

    /**
     * Gets the tabs container within the tab bar.
     */
    public HBox getTabsContainer() {
        return tabsContainer;
    }

    /**
     * Sets the handler called when the float/dock button is clicked.
     * The handler should operate on the active tab.
     */
    public void setOnFloat(Runnable handler) {
        this.onFloat = handler;
    }

    /**
     * Sets the handler called when the minimize button is clicked.
     * The handler should operate on the active tab.
     */
    public void setOnMinimize(Runnable handler) {
        this.onMinimize = handler;
    }

    /**
     * Sets the handler called when the maximize button is clicked.
     * The handler should operate on the active tab.
     */
    public void setOnMaximize(Runnable handler) {
        this.onMaximize = handler;
    }

    /**
     * Updates the visual state when floating status changes.
     * Changes the float button icon to show dock-back when floating.
     */
    public void setFloating(boolean floating) {
        this.isFloating = floating;
        if (floating) {
            // Dock icon: window with arrow pointing in
            floatIcon.setContent("M2,2 L2,10 L10,10 L10,2 Z M5,5 L8,5 M8,5 L8,8");
        } else {
            // Float icon: window with arrow pointing out
            floatIcon.setContent("M2,4 L2,10 L10,10 L10,4 Z M4,2 L4,4 M4,2 L8,2 L8,4");
        }
    }

    /**
     * Updates the visual state when maximized status changes.
     * Changes the maximize button icon to show restore when maximized.
     */
    public void setMaximized(boolean maximized) {
        this.isMaximized = maximized;
        if (maximized) {
            // Restore icon: two overlapping windows
            maximizeIcon.setContent("M4,2 L10,2 L10,8 L8,8 M2,4 L8,4 L8,10 L2,10 Z");
        } else {
            // Maximize icon: single window
            maximizeIcon.setContent("M2,2 L10,2 L10,10 L2,10 Z");
        }
    }

    /**
     * Checks if currently floating.
     */
    public boolean isFloating() {
        return isFloating;
    }

    /**
     * Checks if currently maximized.
     */
    public boolean isMaximized() {
        return isMaximized;
    }

    /**
     * Sets the visibility of the float button.
     */
    public void setFloatButtonVisible(boolean visible) {
        floatButton.setVisible(visible);
        floatButton.setManaged(visible);
    }

    /**
     * Sets the visibility of the minimize button.
     */
    public void setMinimizeButtonVisible(boolean visible) {
        minimizeButton.setVisible(visible);
        minimizeButton.setManaged(visible);
    }

    /**
     * Sets the visibility of the maximize button.
     */
    public void setMaximizeButtonVisible(boolean visible) {
        maximizeButton.setVisible(visible);
        maximizeButton.setManaged(visible);
    }

    @Override
    public Region getNode() {
        return container;
    }

    @Override
    public DockData getMetadata() {
        return DockData.of(id, "TabGroup");
    }

    @Override
    public LayoutNode serialize() {
        List<LeafData> tabData = new ArrayList<>();
        for (DockLeaf leaf : tabs) {
            tabData.add((LeafData) leaf.serialize());
        }
        return TabGroupData.of(id, tabData, activeTabIndex.get());
    }

    @Override
    public void dispose() {
        themeProperty.removeListener(themeListener);
        tabs.removeListener(tabsListener);

        for (DockLeaf leaf : tabs) {
            leaf.dispose();
        }
        tabs.clear();
    }

    @Override
    public DockElement getParent() {
        return parent;
    }

    @Override
    public void setParent(DockElement parent) {
        this.parent = parent;
    }
}
