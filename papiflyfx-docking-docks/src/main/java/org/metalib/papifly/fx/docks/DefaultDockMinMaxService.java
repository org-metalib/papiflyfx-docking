package org.metalib.papifly.fx.docks;

import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockElementVisitor;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockState;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.drag.DropZone;
import org.metalib.papifly.fx.docks.minimize.MinimizedBar;
import org.metalib.papifly.fx.docks.minimize.MinimizedStore;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;

import java.util.ArrayList;

final class DefaultDockMinMaxService implements DockMinMaxService {

    private final DockManagerContext context;
    private final DockFloatingService floatingService;
    private final MinimizedBar minimizedBar;
    private final MinimizedStore minimizedStore;

    private DockLeaf maximizedLeaf;
    private DockTabGroup maximizedGroup;
    private DockElement savedRootBeforeMaximize;
    private RestoreHint maximizeRestoreHint;

    DefaultDockMinMaxService(DockManagerContext context, DockFloatingService floatingService) {
        this.context = context;
        this.floatingService = floatingService;
        this.minimizedBar = new MinimizedBar(context.themeProperty());
        this.minimizedStore = new MinimizedStore();
        minimizedBar.setOnRestore(this::restoreLeaf);
        minimizedStore.setOnLeafAdded(minimizedBar::addLeaf);
        minimizedStore.setOnLeafRemoved(minimizedBar::removeLeaf);
    }

    @Override
    public MinimizedBar getMinimizedBar() {
        return minimizedBar;
    }

    @Override
    public MinimizedStore getMinimizedStore() {
        return minimizedStore;
    }

    @Override
    public void addMinimizedLeaf(DockLeaf leaf, RestoreHint restoreHint) {
        context.updateLeafState(leaf, DockState.MINIMIZED);
        minimizedStore.addLeaf(leaf, restoreHint);
    }

    @Override
    public void removeMinimizedLeaf(DockLeaf leaf) {
        if (minimizedStore.isMinimized(leaf)) {
            minimizedStore.removeLeaf(leaf);
        }
    }

    @Override
    public void clearMinimized() {
        minimizedStore.clear();
        minimizedBar.clear();
    }

    @Override
    public void minimizeLeaf(DockLeaf leaf) {
        context.restoreMaximizedIfNecessary(leaf);

        if (floatingService.isFloating(leaf)) {
            floatingService.unfloatLeaf(leaf);
        }

        RestoreHint hint = MinimizedStore.captureRestoreHint(leaf);
        context.getTreeService().removeLeafFromDock(leaf);
        addMinimizedLeaf(leaf, hint);
    }

    @Override
    public void restoreLeaf(DockLeaf leaf) {
        if (!minimizedStore.isMinimized(leaf)) {
            return;
        }

        RestoreHint hint = minimizedStore.removeLeaf(leaf);
        context.updateLeafState(leaf, DockState.DOCKED);

        if (!context.getTreeService().tryRestoreWithHint(leaf, hint)) {
            context.getTreeService().insertLeafIntoDock(leaf);
        }
    }

    @Override
    public void restoreLeaf(String leafId) {
        DockLeaf leaf = minimizedStore.getLeaf(leafId);
        if (leaf != null) {
            restoreLeaf(leaf);
        }
    }

    @Override
    public void maximizeLeaf(DockLeaf leaf) {
        if (maximizedLeaf != null) {
            restoreMaximized();
        }

        if (floatingService.isFloating(leaf)) {
            floatingService.unfloatLeaf(leaf);
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

        savedRootBeforeMaximize = context.getRoot();
        if (savedRootBeforeMaximize != null) {
            context.getDockingLayer().getChildren().remove(savedRootBeforeMaximize.getNode());
        }

        maximizedGroup = context.createTabGroup();
        maximizedGroup.addLeaf(leaf);
        maximizedGroup.setMaximized(true);
        context.getDockingLayer().getChildren().add(0, maximizedGroup.getNode());
        context.updateLeafState(leaf, DockState.MAXIMIZED);
    }

    @Override
    public void restoreMaximized() {
        if (maximizedLeaf == null) {
            return;
        }

        DockLeaf leaf = maximizedLeaf;

        if (maximizedGroup != null) {
            context.getDockingLayer().getChildren().remove(maximizedGroup.getNode());
            maximizedGroup.removeLeaf(leaf);
            maximizedGroup = null;
        }

        if (savedRootBeforeMaximize != null) {
            context.getDockingLayer().getChildren().add(0, savedRootBeforeMaximize.getNode());
            context.rootProperty().set(savedRootBeforeMaximize);
        }

        if (maximizeRestoreHint != null && maximizeRestoreHint.parentId() != null) {
            DockElement target = context.getTreeService().findElementById(savedRootBeforeMaximize, maximizeRestoreHint.parentId());
            if (target == null) {
                context.getTreeService().insertLeafIntoDock(leaf);
            } else {
                target.accept(new DockElementVisitor<>() {
                    @Override
                    public Void visitTabGroup(DockTabGroup tabGroup) {
                        int index = Math.min(maximizeRestoreHint.tabIndex(), tabGroup.getTabs().size());
                        tabGroup.addLeaf(Math.max(0, index), leaf);
                        tabGroup.setActiveTab(leaf);
                        tabGroup.refreshActiveTabContent();
                        return null;
                    }

                    @Override
                    public Void visitSplitGroup(DockSplitGroup splitGroup) {
                        DockTabGroup tabGroup = context.createTabGroup();
                        tabGroup.addLeaf(leaf);
                        DropZone zone = maximizeRestoreHint.zone();
                        if (zone == DropZone.WEST || zone == DropZone.NORTH) {
                            splitGroup.setFirst(tabGroup);
                        } else {
                            splitGroup.setSecond(tabGroup);
                        }
                        return null;
                    }
                });
            }
        } else {
            context.getTreeService().insertLeafIntoDock(leaf);
        }

        context.updateLeafState(leaf, DockState.DOCKED);
        maximizedLeaf = null;
        maximizedGroup = null;
        savedRootBeforeMaximize = null;
        maximizeRestoreHint = null;
    }

    @Override
    public boolean isMaximized() {
        return maximizedLeaf != null;
    }

    @Override
    public DockLeaf getMaximizedLeaf() {
        return maximizedLeaf;
    }

    @Override
    public RestoreHint getMaximizeRestoreHint() {
        return maximizeRestoreHint;
    }

    @Override
    public void dispose() {
        for (DockLeaf leaf : new ArrayList<>(minimizedStore.getMinimizedLeaves())) {
            leaf.dispose();
        }
        clearMinimized();

        if (maximizedGroup != null) {
            maximizedGroup.dispose();
        } else if (maximizedLeaf != null) {
            maximizedLeaf.dispose();
        }

        maximizedLeaf = null;
        maximizedGroup = null;
        savedRootBeforeMaximize = null;
        maximizeRestoreHint = null;
    }
}
