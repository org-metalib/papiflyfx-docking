package org.metalib.papifly.fx.docks.ribbon;

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
        if (!this.ribbon.themeProperty().isBound()) {
            this.ribbon.themeProperty().bind(this.dockManager.themeProperty());
        }
        if (!this.ribbonManager.contextProperty().isBound()) {
            this.ribbonManager.contextProperty().bind(this.dockManager.ribbonContextProperty());
        }
        this.dockManager.registerSessionStateContributor(sessionStateContributor);

        setTop(this.ribbon);
        setCenter(this.dockManager.getRootPane());
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

    /**
     * Unregisters host-specific bindings and session contributors.
     */
    public void dispose() {
        dockManager.unregisterSessionStateContributor(sessionStateContributor);
    }
}
