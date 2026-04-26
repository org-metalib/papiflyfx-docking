package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.BorderPane;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.DockSessionStateContributor;
import org.metalib.papifly.fx.docks.layout.data.RibbonSessionData;

import java.util.Objects;

/**
 * Convenience host that mounts a {@link Ribbon} above a {@link DockManager}.
 */
public class RibbonDockHost extends BorderPane {

    private final DockManager dockManager;
    private final RibbonManager ribbonManager;
    private final Ribbon ribbon;
    private final DockSessionStateContributor<RibbonSessionData> sessionStateContributor;
    private final boolean ribbonThemeBoundByHost;
    private final boolean ribbonContextBoundByHost;
    private final ChangeListener<RibbonPlacement> placementChangeListener =
        (obs, oldPlacement, newPlacement) -> applyPlacement(newPlacement);
    private final ObjectProperty<RibbonPlacement> placement = new SimpleObjectProperty<>(this, "placement", RibbonPlacement.TOP);
    private boolean disposed;

    /**
     * Creates a host with default ribbon shell/runtime instances.
     *
     * @param dockManager dock manager rendered below the ribbon
     */
    public RibbonDockHost(DockManager dockManager) {
        this(dockManager, new RibbonManager(), new Ribbon());
    }

    /**
     * Creates a host with explicit ribbon runtime and shell instances.
     *
     * @param dockManager dock manager rendered below the ribbon
     * @param ribbonManager ribbon runtime manager
     * @param ribbon ribbon shell
     */
    public RibbonDockHost(DockManager dockManager, RibbonManager ribbonManager, Ribbon ribbon) {
        this.dockManager = Objects.requireNonNull(dockManager, "dockManager");
        this.ribbonManager = Objects.requireNonNull(ribbonManager, "ribbonManager");
        this.ribbon = Objects.requireNonNull(ribbon, "ribbon");
        this.sessionStateContributor = new RibbonSessionStateContributor(this.ribbon);

        getStyleClass().add("pf-ribbon-dock-host");
        setMinSize(0, 0);

        this.ribbon.setManager(this.ribbonManager);
        boolean boundTheme = false;
        if (!this.ribbon.themeProperty().isBound()) {
            this.ribbon.themeProperty().bind(this.dockManager.themeProperty());
            boundTheme = true;
        }
        this.ribbonThemeBoundByHost = boundTheme;
        boolean boundContext = false;
        if (!this.ribbonManager.contextProperty().isBound()) {
            this.ribbonManager.contextProperty().bind(this.dockManager.ribbonContextProperty());
            boundContext = true;
        }
        this.ribbonContextBoundByHost = boundContext;
        this.dockManager.registerSessionStateContributor(sessionStateContributor);

        this.ribbon.placementProperty().bindBidirectional(this.placement);
        this.placement.addListener(placementChangeListener);
        applyPlacement(this.placement.get());
        setCenter(this.dockManager.getRootPane());
    }

    /**
     * Returns the ribbon placement property.
     *
     * @return ribbon placement property
     */
    public ObjectProperty<RibbonPlacement> placementProperty() {
        return placement;
    }

    /**
     * Returns the side where the ribbon is hosted.
     *
     * @return current placement, defaulting to {@link RibbonPlacement#TOP}
     */
    public RibbonPlacement getPlacement() {
        return RibbonPlacement.normalize(placement.get());
    }

    /**
     * Updates the side where the ribbon is hosted.
     *
     * @param placement requested placement, or {@code null} for {@link RibbonPlacement#TOP}
     */
    public void setPlacement(RibbonPlacement placement) {
        this.placement.set(RibbonPlacement.normalize(placement));
    }

    /**
     * Returns the hosted dock manager.
     *
     * @return hosted dock manager
     */
    public DockManager getDockManager() {
        return dockManager;
    }

    /**
     * Returns the hosted ribbon manager.
     *
     * @return hosted ribbon manager
     */
    public RibbonManager getRibbonManager() {
        return ribbonManager;
    }

    /**
     * Returns the hosted ribbon shell.
     *
     * @return hosted ribbon shell
     */
    public Ribbon getRibbon() {
        return ribbon;
    }

    private void applyPlacement(RibbonPlacement requestedPlacement) {
        RibbonPlacement resolvedPlacement = RibbonPlacement.normalize(requestedPlacement);
        clearRibbonRegions();
        switch (resolvedPlacement) {
            case TOP -> setTop(ribbon);
            case BOTTOM -> setBottom(ribbon);
            case LEFT -> setLeft(ribbon);
            case RIGHT -> setRight(ribbon);
        }
    }

    private void clearRibbonRegions() {
        setTop(null);
        setBottom(null);
        setLeft(null);
        setRight(null);
    }

    /**
     * Unregisters host-specific bindings and session contributors.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        placement.removeListener(placementChangeListener);
        ribbon.placementProperty().unbindBidirectional(placement);
        if (ribbonThemeBoundByHost && ribbon.themeProperty().isBound()) {
            ribbon.themeProperty().unbind();
        }
        if (ribbonContextBoundByHost && ribbonManager.contextProperty().isBound()) {
            ribbonManager.contextProperty().unbind();
        }
        dockManager.unregisterSessionStateContributor(sessionStateContributor);
        clearRibbonRegions();
        setCenter(null);
    }
}
