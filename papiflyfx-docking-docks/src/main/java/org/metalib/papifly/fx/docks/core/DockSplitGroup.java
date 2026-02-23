package org.metalib.papifly.fx.docks.core;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.SplitData;
import org.metalib.papifly.fx.docking.api.Theme;

import java.util.UUID;

/**
 * Manages two DockElements with a draggable divider.
 * Custom implementation replacing standard SplitPane for better control.
 */
public class DockSplitGroup implements DockElement {

    private static final double DIVIDER_SIZE = 6.0;
    private static final double MIN_SIZE = 50.0;

    private final String id;
    private final SplitContainer container;
    private final Region divider;
    private final ObjectProperty<Theme> themeProperty;
    private final DoubleProperty dividerPosition;
    private final Orientation orientation;
    private ChangeListener<Theme> themeListener;

    private DockElement first;
    private DockElement second;
    private DockElement parent;

    private double dragStart;
    private double dividerStartPosition;

    /**
     * Creates a split group with an auto-generated id and default divider position.
     *
     * @param orientation split orientation
     * @param themeProperty theme property used for styling
     */
    public DockSplitGroup(Orientation orientation, ObjectProperty<Theme> themeProperty) {
        this(UUID.randomUUID().toString(), orientation, 0.5, themeProperty);
    }

    /**
     * Creates a split group with explicit id and divider position.
     *
     * @param id split identifier
     * @param orientation split orientation
     * @param initialPosition initial divider position ratio
     * @param themeProperty theme property used for styling
     */
    public DockSplitGroup(String id, Orientation orientation, double initialPosition, ObjectProperty<Theme> themeProperty) {
        this.id = id;
        this.orientation = orientation;
        this.themeProperty = themeProperty;
        this.dividerPosition = new SimpleDoubleProperty(initialPosition);

        this.divider = createDivider();
        this.container = new SplitContainer();
        container.getChildren().add(divider);

        dividerPosition.addListener((obs, oldVal, newVal) -> container.requestLayout());

        applyTheme(themeProperty.get());
        themeListener = (obs, oldTheme, newTheme) -> applyTheme(newTheme);
        themeProperty.addListener(themeListener);
    }

    /**
     * Custom container that properly handles layout of split children.
     * Extends Pane to get public getChildren() access.
     */
    private class SplitContainer extends Pane {

        @Override
        protected void layoutChildren() {
            double width = getWidth();
            double height = getHeight();

            if (width <= 0 || height <= 0) return;

            double pos = dividerPosition.get();

            if (orientation == Orientation.HORIZONTAL) {
                double firstWidth = (width - DIVIDER_SIZE) * pos;
                double secondWidth = width - firstWidth - DIVIDER_SIZE;

                if (first != null) {
                    Region firstNode = first.getNode();
                    firstNode.resizeRelocate(0, 0, firstWidth, height);
                }

                divider.resizeRelocate(firstWidth, 0, DIVIDER_SIZE, height);

                if (second != null) {
                    Region secondNode = second.getNode();
                    secondNode.resizeRelocate(firstWidth + DIVIDER_SIZE, 0, secondWidth, height);
                }
            } else {
                double firstHeight = (height - DIVIDER_SIZE) * pos;
                double secondHeight = height - firstHeight - DIVIDER_SIZE;

                if (first != null) {
                    Region firstNode = first.getNode();
                    firstNode.resizeRelocate(0, 0, width, firstHeight);
                }

                divider.resizeRelocate(0, firstHeight, width, DIVIDER_SIZE);

                if (second != null) {
                    Region secondNode = second.getNode();
                    secondNode.resizeRelocate(0, firstHeight + DIVIDER_SIZE, width, secondHeight);
                }
            }
        }

        @Override
        protected double computeMinWidth(double height) {
            return 0; // Allow shrinking - layout handles constraints internally
        }

        @Override
        protected double computeMinHeight(double width) {
            return 0; // Allow shrinking - layout handles constraints internally
        }

        @Override
        protected double computePrefWidth(double height) {
            return 400;
        }

        @Override
        protected double computePrefHeight(double width) {
            return 300;
        }
    }

    private Region createDivider() {
        Region div = new Region();
        div.setCursor(orientation == Orientation.HORIZONTAL ? Cursor.H_RESIZE : Cursor.V_RESIZE);
        div.setPickOnBounds(true); // Ensure mouse events are captured on full bounds

        div.setOnMousePressed(this::onDividerPressed);
        div.setOnMouseDragged(this::onDividerDragged);

        return div;
    }

    private void onDividerPressed(MouseEvent e) {
        dragStart = orientation == Orientation.HORIZONTAL ? e.getScreenX() : e.getScreenY();
        dividerStartPosition = dividerPosition.get();
        e.consume();
    }

    private void onDividerDragged(MouseEvent e) {
        double current = orientation == Orientation.HORIZONTAL ? e.getScreenX() : e.getScreenY();
        double delta = current - dragStart;
        double totalSize = orientation == Orientation.HORIZONTAL ? container.getWidth() : container.getHeight();

        if (totalSize > 0) {
            double newPosition = dividerStartPosition + (delta / totalSize);
            // Constrain to valid range
            double minPos = MIN_SIZE / totalSize;
            double maxPos = 1.0 - (MIN_SIZE / totalSize);
            newPosition = Math.max(minPos, Math.min(maxPos, newPosition));
            dividerPosition.set(newPosition);
        }
        e.consume();
    }

    private void applyTheme(Theme theme) {
        if (theme == null) return;

        divider.setBackground(new Background(new BackgroundFill(theme.dividerColor(), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    /**
     * Sets the first (left or top) element.
     *
     * @param element first child element
     */
    public void setFirst(DockElement element) {
        if (first != null) {
            container.getChildren().remove(first.getNode());
            first.setParent(null);
        }
        first = element;
        if (first != null) {
            first.setParent(this);
            Region node = first.getNode();
            node.setMinSize(0, 0); // Allow shrinking
            container.getChildren().add(0, node);
            // Ensure divider stays on top for mouse events
            divider.toFront();
            container.requestLayout();
        }
    }

    /**
     * Sets the second (right or bottom) element.
     *
     * @param element second child element
     */
    public void setSecond(DockElement element) {
        if (second != null) {
            container.getChildren().remove(second.getNode());
            second.setParent(null);
        }
        second = element;
        if (second != null) {
            second.setParent(this);
            Region node = second.getNode();
            node.setMinSize(0, 0); // Allow shrinking
            container.getChildren().add(node);
            // Ensure divider stays on top for mouse events
            divider.toFront();
            container.requestLayout();
        }
    }

    /**
     * Replaces a child element with a new element.
     *
     * @param oldChild child element to replace
     * @param newChild replacement child element
     */
    public void replaceChild(DockElement oldChild, DockElement newChild) {
        if (first == oldChild) {
            setFirst(newChild);
        } else if (second == oldChild) {
            setSecond(newChild);
        }
        // Ensure divider stays on top after any child replacement
        divider.toFront();
    }

    /**
     * Gets the first element.
     *
     * @return first child element
     */
    public DockElement getFirst() {
        return first;
    }

    /**
     * Gets the second element.
     *
     * @return second child element
     */
    public DockElement getSecond() {
        return second;
    }

    /**
     * Gets the divider position property.
     *
     * @return divider position property
     */
    public DoubleProperty dividerPositionProperty() {
        return dividerPosition;
    }

    /**
     * Gets the current divider position (0.0 to 1.0).
     *
     * @return divider position ratio
     */
    public double getDividerPosition() {
        return dividerPosition.get();
    }

    /**
     * Sets the divider position.
     *
     * @param position divider position ratio
     */
    public void setDividerPosition(double position) {
        dividerPosition.set(position);
    }

    /**
     * Gets the orientation.
     *
     * @return split orientation
     */
    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public Region getNode() {
        return container;
    }

    @Override
    public DockData getMetadata() {
        return DockData.of(id, "Split");
    }

    @Override
    public LayoutNode serialize() {
        LayoutNode firstData = first != null ? first.serialize() : null;
        LayoutNode secondData = second != null ? second.serialize() : null;
        return new SplitData(id, orientation, dividerPosition.get(), firstData, secondData);
    }

    @Override
    public void dispose() {
        if (themeListener != null) {
            themeProperty.removeListener(themeListener);
            themeListener = null;
        }
        if (first != null) {
            first.dispose();
            first = null;
        }
        if (second != null) {
            second.dispose();
            second = null;
        }
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
