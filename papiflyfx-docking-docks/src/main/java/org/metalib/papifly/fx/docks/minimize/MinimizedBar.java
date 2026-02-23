package org.metalib.papifly.fx.docks.minimize;

import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import org.metalib.papifly.fx.docks.core.DockData;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docking.api.Theme;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * UI component that displays minimized leaves as a horizontal bar.
 * Auto-hides when empty and provides click-to-restore functionality.
 */
public class MinimizedBar extends StackPane {

    private static final double MAX_ITEM_WIDTH = 120;
    private static final double ANIMATION_DURATION_MS = 200;

    private final HBox itemsContainer;
    private final ScrollPane scrollPane;
    private final ObjectProperty<Theme> themeProperty;
    private final Map<String, HBox> leafItems;

    private Consumer<DockLeaf> onRestore;
    private boolean visible;

    /**
     * Creates a new MinimizedBar.
     *
     * @param themeProperty The theme property for styling
     */
    public MinimizedBar(ObjectProperty<Theme> themeProperty) {
        this.themeProperty = themeProperty;
        this.leafItems = new HashMap<>();
        this.visible = false;

        // Create items container
        itemsContainer = new HBox(4);
        itemsContainer.setAlignment(Pos.CENTER_LEFT);
        itemsContainer.setPadding(new Insets(2, 8, 2, 8));

        // Wrap in scroll pane for overflow
        scrollPane = new ScrollPane(itemsContainer);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        getChildren().add(scrollPane);

        // Apply theme
        applyTheme(themeProperty.get());
        themeProperty.addListener((obs, oldTheme, newTheme) -> applyTheme(newTheme));

        // Start hidden
        setVisible(false);
        setManaged(false);
    }

    private void applyTheme(Theme theme) {
        if (theme == null) return;

        double height = theme.minimizedBarHeight();
        setPrefHeight(height);
        setMinHeight(height);
        setMaxHeight(height);

        String bgColor = toHexString(theme.minimizedBarBackground());
        setStyle("-fx-background-color: " + bgColor + ";");
        scrollPane.setStyle("-fx-background: " + bgColor + "; -fx-background-color: " + bgColor + ";");

        // Update existing items
        for (HBox item : leafItems.values()) {
            applyItemStyle(item, theme);
        }
    }

    private String toHexString(javafx.scene.paint.Paint paint) {
        if (paint instanceof Color color) {
            return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        }
        return "#282828";
    }

    /**
     * Adds a minimized leaf to the bar.
     *
     * @param leaf The leaf to add
     */
    public void addLeaf(DockLeaf leaf) {
        String leafId = leaf.getMetadata().id();
        if (leafItems.containsKey(leafId)) {
            return;
        }

        HBox item = createItem(leaf);
        leafItems.put(leafId, item);
        itemsContainer.getChildren().add(item);

        updateVisibility();
    }

    /**
     * Removes a minimized leaf from the bar.
     *
     * @param leaf The leaf to remove
     */
    public void removeLeaf(DockLeaf leaf) {
        String leafId = leaf.getMetadata().id();
        HBox item = leafItems.remove(leafId);
        if (item != null) {
            itemsContainer.getChildren().remove(item);
        }

        updateVisibility();
    }

    /**
     * Removes a minimized leaf by ID.
     *
     * @param leafId The leaf ID to remove
     */
    public void removeLeaf(String leafId) {
        HBox item = leafItems.remove(leafId);
        if (item != null) {
            itemsContainer.getChildren().remove(item);
        }

        updateVisibility();
    }

    private HBox createItem(DockLeaf leaf) {
        DockData metadata = leaf.getMetadata();

        // Icon
        HBox iconContainer = new HBox();
        iconContainer.setAlignment(Pos.CENTER);
        if (metadata.icon() != null) {
            // Clone icon for minimized bar (can't reuse Node)
            iconContainer.getChildren().add(createMinimizedIcon());
        } else {
            iconContainer.getChildren().add(createMinimizedIcon());
        }

        // Label
        Label label = new Label(metadata.title());
        label.setMaxWidth(MAX_ITEM_WIDTH - 24);
        label.textProperty().bind(leaf.metadataProperty().map(DockData::title));

        // Tooltip for full title
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(leaf.metadataProperty().map(DockData::title));
        Tooltip.install(label, tooltip);

        // Container
        HBox item = new HBox(4, iconContainer, label);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(2, 8, 2, 8));
        item.setCursor(Cursor.HAND);
        item.setMaxWidth(MAX_ITEM_WIDTH);
        item.setUserData(leaf);

        // Click handler
        item.setOnMouseClicked(e -> {
            e.consume();
            if (onRestore != null) {
                onRestore.accept(leaf);
            }
        });

        // Apply theme
        applyItemStyle(item, themeProperty.get());

        // Hover effects
        item.setOnMouseEntered(e -> {
            Theme theme = themeProperty.get();
            if (theme != null) {
                item.setBackground(new Background(new BackgroundFill(
                    theme.buttonHoverBackground(),
                    new CornerRadii(theme.cornerRadius()),
                    Insets.EMPTY
                )));
            }
        });

        item.setOnMouseExited(e -> applyItemStyle(item, themeProperty.get()));

        return item;
    }

    private Node createMinimizedIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M2,2 L10,2 L10,10 L2,10 Z"); // Simple window icon
        path.setStroke(Color.GRAY);
        path.setStrokeWidth(1.0);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private void applyItemStyle(HBox item, Theme theme) {
        if (theme == null) return;

        item.setBackground(new Background(new BackgroundFill(
            theme.headerBackground(),
            new CornerRadii(theme.cornerRadius()),
            Insets.EMPTY
        )));

        // Style label
        for (Node child : item.getChildren()) {
            if (child instanceof Label label) {
                label.setFont(theme.headerFont());
                label.setTextFill(theme.textColor());
            }
        }
    }

    private void updateVisibility() {
        boolean shouldShow = !leafItems.isEmpty();

        if (shouldShow && !visible) {
            showBar();
        } else if (!shouldShow && visible) {
            hideBar();
        }
    }

    private void showBar() {
        visible = true;
        setVisible(true);
        setManaged(true);

        // Slide-in animation
        TranslateTransition transition = new TranslateTransition(Duration.millis(ANIMATION_DURATION_MS), this);
        double height = themeProperty.get() != null ? themeProperty.get().minimizedBarHeight() : 24.0;
        setTranslateY(height);
        transition.setFromY(height);
        transition.setToY(0);
        transition.play();
    }

    private void hideBar() {
        visible = false;

        // Slide-out animation
        TranslateTransition transition = new TranslateTransition(Duration.millis(ANIMATION_DURATION_MS), this);
        double height = themeProperty.get() != null ? themeProperty.get().minimizedBarHeight() : 24.0;
        transition.setFromY(0);
        transition.setToY(height);
        transition.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
        });
        transition.play();
    }

    /**
     * Sets the handler called when a minimized item is clicked to restore.
     *
     * @param handler restore callback
     */
    public void setOnRestore(Consumer<DockLeaf> handler) {
        this.onRestore = handler;
    }

    /**
     * Gets the number of items in the bar.
     *
     * @return number of minimized items
     */
    public int getItemCount() {
        return leafItems.size();
    }

    /**
     * Checks if the bar is currently showing.
     *
     * @return {@code true} when the minimized bar is visible
     */
    public boolean isBarVisible() {
        return visible;
    }

    /**
     * Clears all items from the bar.
     */
    public void clear() {
        leafItems.clear();
        itemsContainer.getChildren().clear();
        updateVisibility();
    }
}
