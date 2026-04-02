package org.metalib.papifly.fx.docks;

import javafx.geometry.Orientation;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockSplitGroup;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.drag.DropZone;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;

import java.util.Collection;

final class DockTreeService {

    private final DockManager manager;

    DockTreeService(DockManager manager) {
        this.manager = manager;
    }

    void removeElement(DockElement element) {
        DockElement parent = element.getParent();

        if (parent instanceof DockSplitGroup split) {
            DockElement sibling = (split.getFirst() == element) ? split.getSecond() : split.getFirst();

            detachChild(split, element);
            detachChild(split, sibling);

            DockElement grandparent = split.getParent();
            if (grandparent instanceof DockSplitGroup grandSplit) {
                grandSplit.replaceChild(split, sibling);
            } else if (grandparent == null) {
                manager.setRoot(sibling);
            }

            split.dispose();
        } else if (parent == null) {
            manager.setRoot((DockElement) null);
        }

        element.dispose();
    }

    void removeLeafFromDock(DockLeaf leaf) {
        DockTabGroup parent = leaf.getParent();
        if (parent != null) {
            parent.removeLeaf(leaf);
            if (parent.getTabs().isEmpty()) {
                removeElementWithoutDispose(parent);
            }
        }
    }

    void insertLeafIntoDock(DockLeaf leaf) {
        DockElement currentRoot = manager.getRoot();

        if (currentRoot == null) {
            DockTabGroup tabGroup = manager.createTabGroup();
            tabGroup.addLeaf(leaf);
            manager.setRoot(tabGroup);
        } else if (currentRoot instanceof DockTabGroup tabGroup) {
            tabGroup.addLeaf(leaf);
        } else {
            DockTabGroup tabGroup = manager.createTabGroup();
            tabGroup.addLeaf(leaf);
            DockSplitGroup newSplit = manager.createHorizontalSplit(currentRoot, tabGroup, 0.75);
            manager.setRoot(newSplit);
        }
    }

    boolean tryRestoreWithHint(DockLeaf leaf, RestoreHint hint) {
        if (hint == null) {
            return false;
        }

        if (hint.parentId() != null) {
            DockElement target = findElementById(manager.getRoot(), hint.parentId());

            if (target instanceof DockTabGroup tabGroup && hint.zone() == DropZone.TAB_BAR) {
                int index = Math.min(hint.tabIndex(), tabGroup.getTabs().size());
                tabGroup.addLeaf(index >= 0 ? index : tabGroup.getTabs().size(), leaf);
                return true;
            }

            if (target instanceof DockSplitGroup split) {
                DropZone zone = hint.zone();
                if (zone == DropZone.WEST || zone == DropZone.NORTH) {
                    if (split.getFirst() == null) {
                        DockTabGroup tabGroup = manager.createTabGroup();
                        tabGroup.addLeaf(leaf);
                        split.setFirst(tabGroup);
                        return true;
                    }
                } else if (zone == DropZone.EAST || zone == DropZone.SOUTH) {
                    if (split.getSecond() == null) {
                        DockTabGroup tabGroup = manager.createTabGroup();
                        tabGroup.addLeaf(leaf);
                        split.setSecond(tabGroup);
                        return true;
                    }
                }
            }
        }

        if (hint.siblingId() != null) {
            DockElement sibling = findElementById(manager.getRoot(), hint.siblingId());
            if (sibling != null) {
                DockElement siblingParent = sibling.getParent();
                DropZone zone = hint.zone();
                Orientation orientation = (zone == DropZone.WEST || zone == DropZone.EAST)
                    ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                boolean leafFirst = (zone == DropZone.WEST || zone == DropZone.NORTH);

                DockSplitGroup newSplit = new DockSplitGroup(orientation, manager.themeProperty());
                newSplit.setDividerPosition(hint.splitPosition());

                if (siblingParent instanceof DockSplitGroup parentSplit) {
                    parentSplit.replaceChild(sibling, newSplit);
                } else if (siblingParent == null) {
                    manager.setRoot(newSplit);
                }

                if (leafFirst) {
                    DockTabGroup tabGroup = manager.createTabGroup();
                    tabGroup.addLeaf(leaf);
                    newSplit.setFirst(tabGroup);
                    newSplit.setSecond(sibling);
                } else {
                    DockTabGroup tabGroup = manager.createTabGroup();
                    tabGroup.addLeaf(leaf);
                    newSplit.setFirst(sibling);
                    newSplit.setSecond(tabGroup);
                }

                return true;
            }
        }

        return false;
    }

    DockElement findElementById(DockElement element, String id) {
        if (element == null) {
            return null;
        }
        if (element.getMetadata().id().equals(id)) {
            return element;
        }
        if (element instanceof DockSplitGroup split) {
            DockElement found = findElementById(split.getFirst(), id);
            if (found != null) {
                return found;
            }
            return findElementById(split.getSecond(), id);
        }
        if (element instanceof DockTabGroup tabGroup) {
            for (DockLeaf tab : tabGroup.getTabs()) {
                if (tab.getMetadata().id().equals(id)) {
                    return tabGroup;
                }
            }
        }
        return null;
    }

    void collectLeaves(DockElement element, Collection<DockLeaf> leaves) {
        if (element == null) {
            return;
        }
        if (element instanceof DockTabGroup tabGroup) {
            leaves.addAll(tabGroup.getTabs());
        } else if (element instanceof DockSplitGroup split) {
            collectLeaves(split.getFirst(), leaves);
            collectLeaves(split.getSecond(), leaves);
        }
    }

    private void removeElementWithoutDispose(DockElement element) {
        DockElement parent = element.getParent();

        if (parent instanceof DockSplitGroup split) {
            DockElement sibling = (split.getFirst() == element) ? split.getSecond() : split.getFirst();

            detachChild(split, element);
            detachChild(split, sibling);

            DockElement grandparent = split.getParent();
            if (grandparent instanceof DockSplitGroup grandSplit) {
                grandSplit.replaceChild(split, sibling);
            } else if (grandparent == null) {
                manager.setRoot(sibling);
            }

            split.dispose();
        } else if (parent == null) {
            manager.setRoot((DockElement) null);
        }
    }

    private void detachChild(DockSplitGroup split, DockElement child) {
        if (child == null) {
            return;
        }
        if (split.getFirst() == child) {
            split.setFirst(null);
        } else if (split.getSecond() == child) {
            split.setSecond(null);
        }
    }
}
