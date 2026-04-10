package org.metalib.papifly.fx.docks;

import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockState;
import org.metalib.papifly.fx.docks.floating.FloatingDockWindow;
import org.metalib.papifly.fx.docks.floating.FloatingWindowManager;
import org.metalib.papifly.fx.docks.minimize.MinimizedStore;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

final class DefaultDockFloatingService implements DockFloatingService {

    private static final Logger LOG = Logger.getLogger(DefaultDockFloatingService.class.getName());

    private final DockManagerContext context;
    private final Map<String, RestoreHint> floatingRestoreHints = new HashMap<>();

    private FloatingWindowManager floatingWindowManager;
    private Stage ownerStage;

    DefaultDockFloatingService(DockManagerContext context) {
        this.context = context;
    }

    @Override
    public void setOwnerStage(Stage stage) {
        this.ownerStage = stage;
        this.floatingWindowManager = new FloatingWindowManager(stage, context.themeProperty(), context::createTabGroup);
        floatingWindowManager.setOnDockBack(this::dockLeaf);
        floatingWindowManager.setOnClose(context::closeLeaf);
    }

    @Override
    public boolean ensureFloatingWindowManager(String operation) {
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

    @Override
    public FloatingWindowManager getFloatingWindowManager() {
        return floatingWindowManager;
    }

    @Override
    public boolean isFloating(DockLeaf leaf) {
        return floatingWindowManager != null && floatingWindowManager.isFloating(leaf);
    }

    @Override
    public void unfloatLeaf(DockLeaf leaf) {
        if (floatingWindowManager != null && floatingWindowManager.isFloating(leaf)) {
            floatingWindowManager.unfloatLeaf(leaf);
        }
    }

    @Override
    public void floatLeaf(DockLeaf leaf) {
        if (!ensureFloatingWindowManager("float leaf")) {
            return;
        }

        context.restoreMaximizedIfNecessary(leaf);

        if (floatingWindowManager.isFloating(leaf)) {
            FloatingDockWindow window = floatingWindowManager.getWindow(leaf);
            if (window != null) {
                window.toFront();
            }
            return;
        }

        floatingRestoreHints.put(leaf.getMetadata().id(), MinimizedStore.captureRestoreHint(leaf));
        context.getTreeService().removeLeafFromDock(leaf);
        context.updateLeafState(leaf, DockState.FLOATING);
        floatingWindowManager.floatLeaf(leaf).show();
    }

    @Override
    public void floatLeaf(DockLeaf leaf, double x, double y) {
        if (!ensureFloatingWindowManager("float leaf")) {
            return;
        }

        context.restoreMaximizedIfNecessary(leaf);

        if (floatingWindowManager.isFloating(leaf)) {
            FloatingDockWindow window = floatingWindowManager.getWindow(leaf);
            if (window != null) {
                window.show(x, y);
            }
            return;
        }

        floatingRestoreHints.put(leaf.getMetadata().id(), MinimizedStore.captureRestoreHint(leaf));
        context.getTreeService().removeLeafFromDock(leaf);
        context.updateLeafState(leaf, DockState.FLOATING);
        floatingWindowManager.floatLeaf(leaf, x, y);
    }

    @Override
    public void dockLeaf(DockLeaf leaf) {
        if (isFloating(leaf)) {
            floatingWindowManager.unfloatLeaf(leaf);
        }

        context.updateLeafState(leaf, DockState.DOCKED);

        RestoreHint hint = floatingRestoreHints.remove(leaf.getMetadata().id());
        if (hint != null && context.getTreeService().tryRestoreWithHint(leaf, hint)) {
            return;
        }

        context.getTreeService().insertLeafIntoDock(leaf);
    }

    @Override
    public boolean restoreFloating(DockLeaf leaf, RestoreHint restoreHint, Rectangle2D bounds) {
        if (!ensureFloatingWindowManager("restore floating leaf")) {
            return false;
        }

        if (restoreHint != null) {
            floatingRestoreHints.put(leaf.getMetadata().id(), restoreHint);
        }

        context.updateLeafState(leaf, DockState.FLOATING);
        FloatingDockWindow window = floatingWindowManager.floatLeaf(leaf);
        if (bounds != null) {
            window.setBounds(bounds);
        }
        window.show();
        return true;
    }

    @Override
    public RestoreHint getRestoreHint(String leafId) {
        return floatingRestoreHints.get(leafId);
    }

    @Override
    public void forgetRestoreHint(String leafId) {
        floatingRestoreHints.remove(leafId);
    }

    @Override
    public void clearRestoreHints() {
        floatingRestoreHints.clear();
    }

    @Override
    public void closeAll() {
        if (floatingWindowManager != null) {
            floatingWindowManager.closeAll();
        }
    }

    @Override
    public void dispose() {
        if (floatingWindowManager != null) {
            List<DockLeaf> floatingLeaves = new ArrayList<>();
            for (FloatingDockWindow window : floatingWindowManager.getFloatingWindows()) {
                DockLeaf leaf = window.getLeaf();
                if (leaf != null) {
                    floatingLeaves.add(leaf);
                }
            }
            floatingWindowManager.dispose();
            floatingWindowManager = null;
            for (DockLeaf leaf : floatingLeaves) {
                leaf.dispose();
            }
        }
        floatingRestoreHints.clear();
        ownerStage = null;
    }

    private Stage resolveOwnerStage() {
        if (ownerStage != null) {
            return ownerStage;
        }
        if (context.getRootStack().getScene() == null) {
            return null;
        }
        if (context.getRootStack().getScene().getWindow() instanceof Stage stage) {
            ownerStage = stage;
            return stage;
        }
        return null;
    }
}
