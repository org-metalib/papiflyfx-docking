package org.metalib.papifly.fx.docks.floating;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.theme.Theme;

import java.util.function.Consumer;

/**
 * Internal class that hosts a floating DockLeaf in its own Stage.
 * Manages the window lifecycle and provides callbacks for dock-back operations.
 */
public class FloatingDockWindow {

    private static final double MIN_WIDTH = 200;
    private static final double MIN_HEIGHT = 150;
    private static final double DEFAULT_WIDTH = 400;
    private static final double DEFAULT_HEIGHT = 300;

    private final Stage stage;
    private final StackPane rootContainer;
    private final DockLeaf leaf;
    private final DockTabGroup tabGroup;
    private final ObjectProperty<Theme> themeProperty;
    private ChangeListener<Theme> themeListener;

    private Consumer<DockLeaf> onDockBack;
    private Consumer<DockLeaf> onClose;
    private Rectangle2D lastBounds;

    /**
     * Creates a new floating window for the given leaf.
     *
     * @param leaf          The leaf to host
     * @param ownerStage    The owner stage (main application window)
     * @param themeProperty The theme property for styling
     */
    public FloatingDockWindow(DockLeaf leaf, DockTabGroup tabGroup, Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        this.leaf = leaf;
        this.tabGroup = tabGroup;
        this.themeProperty = themeProperty;

        // Create the stage
        stage = new Stage(StageStyle.DECORATED);
        // Conditionally set the owner to allow moving on macOS
        final String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) {
            // To allow the floating windows to be moved to other desktops, you must create them without an owner.
            // The trade-off is that without an owner, the floating window will no longer be guaranteed to stay on
            // top of the main application window, and it might not minimize or close automatically with the main window.
            // However, for a "floating" dock window, behaving like a separate application window is often the desired effect.
            //
            // the FloatingDockWindow will be a top-level, independent window on macOS, allowing users to move it freely
            // between virtual desktops, just like any other application window. On other operating systems like Windows
            // and Linux, it will retain its owner and stay on top of the main application window.
            stage.initOwner(ownerStage);
        }
        stage.setTitle(leaf.getMetadata().title());

        // Set minimum size
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        // Create root container
        rootContainer = new StackPane();
        rootContainer.setMinSize(0, 0);

        // Add the leaf's tab group to the container
        rootContainer.getChildren().add(tabGroup.getNode());

        // Apply theme
        applyTheme(themeProperty.get());
        themeListener = (obs, oldTheme, newTheme) -> applyTheme(newTheme);
        themeProperty.addListener(themeListener);

        // Bind title to leaf metadata
        leaf.metadataProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                stage.setTitle(newVal.title());
            }
        });

        // Setup close handler - default behavior is to dock back
        stage.setOnCloseRequest(event -> {
            event.consume();
            if (onDockBack != null) {
                onDockBack.accept(leaf);
            } else if (onClose != null) {
                onClose.accept(leaf);
            }
        });

        // Create scene
        Scene scene = new Scene(rootContainer, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        stage.setScene(scene);
    }

    private void applyTheme(Theme theme) {
        if (theme == null) return;

        String bgColor = toHexString(theme.background());
        rootContainer.setStyle("-fx-background-color: " + bgColor + ";");
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
     * Shows the floating window.
     */
    public void show() {
        // Restore last position if available
        if (lastBounds != null) {
            stage.setX(lastBounds.getMinX());
            stage.setY(lastBounds.getMinY());
            stage.setWidth(lastBounds.getWidth());
            stage.setHeight(lastBounds.getHeight());
        }
        stage.show();
        stage.toFront();
    }

    /**
     * Shows the floating window at a specific position.
     */
    public void show(double x, double y) {
        stage.setX(x);
        stage.setY(y);
        stage.show();
        stage.toFront();
    }

    /**
     * Hides the floating window.
     */
    public void hide() {
        // Store current bounds before hiding
        lastBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        stage.hide();
    }

    /**
     * Closes and disposes the floating window.
     */
    public void close() {
        lastBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        rootContainer.getChildren().clear();
        if (themeListener != null) {
            themeProperty.removeListener(themeListener);
            themeListener = null;
        }
        stage.close();
    }

    /**
     * Gets the hosted leaf.
     */
    public DockLeaf getLeaf() {
        return leaf;
    }

    /**
     * Gets the tab group hosting the leaf.
     */
    public DockTabGroup getTabGroup() {
        return tabGroup;
    }

    /**
     * Gets the leaf ID.
     */
    public String getLeafId() {
        return leaf.getMetadata().id();
    }

    /**
     * Gets the stage.
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Gets the current bounds of the floating window.
     */
    public Rectangle2D getBounds() {
        if (stage.isShowing()) {
            return new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        }
        return lastBounds;
    }

    /**
     * Sets the bounds of the floating window.
     */
    public void setBounds(Rectangle2D bounds) {
        this.lastBounds = bounds;
        if (stage.isShowing()) {
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }
    }

    /**
     * Sets the handler called when the user wants to dock the leaf back.
     */
    public void setOnDockBack(Consumer<DockLeaf> handler) {
        this.onDockBack = handler;
    }

    /**
     * Sets the handler called when the window is closed.
     */
    public void setOnClose(Consumer<DockLeaf> handler) {
        this.onClose = handler;
    }

    /**
     * Checks if the window is currently showing.
     */
    public boolean isShowing() {
        return stage.isShowing();
    }

    /**
     * Brings the window to front.
     */
    public void toFront() {
        stage.toFront();
    }
}
