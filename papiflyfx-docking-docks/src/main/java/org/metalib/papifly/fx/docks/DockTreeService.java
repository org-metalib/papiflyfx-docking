package org.metalib.papifly.fx.docks;

import javafx.geometry.Orientation;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockElementVisitor;
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
        removeElementInternal(element, true);
    }

    void removeLeafFromDock(DockLeaf leaf) {
        DockTabGroup parent = leaf.getParent();
        if (parent != null) {
            parent.removeLeaf(leaf);
            if (parent.getTabs().isEmpty()) {
                removeElementInternal(parent, false);
            }
        }
    }

    void insertLeafIntoDock(DockLeaf leaf) {
        DockElement currentRoot = manager.getRoot();

        if (currentRoot == null) {
            DockTabGroup tabGroup = createTabGroupWithLeaf(leaf);
            manager.setRoot(tabGroup);
            return;
        }

        currentRoot.accept(new DockElementVisitor<Void>() {
            @Override
            public Void visitTabGroup(DockTabGroup tabGroup) {
                tabGroup.addLeaf(leaf);
                return null;
            }

            @Override
            public Void visitSplitGroup(DockSplitGroup splitGroup) {
                DockTabGroup tabGroup = createTabGroupWithLeaf(leaf);
                DockSplitGroup newSplit = manager.createHorizontalSplit(splitGroup, tabGroup, 0.75);
                manager.setRoot(newSplit);
                return null;
            }
        });
    }

    boolean tryRestoreWithHint(DockLeaf leaf, RestoreHint hint) {
        if (hint == null) {
            return false;
        }

        if (hint.parentId() != null) {
            DockElement target = findElementById(manager.getRoot(), hint.parentId());
            if (target != null && restoreIntoParentTarget(target, leaf, hint)) {
                return true;
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

                replaceChildAtParent(siblingParent, sibling, newSplit);

                DockTabGroup tabGroup = createTabGroupWithLeaf(leaf);
                if (leafFirst) {
                    newSplit.setFirst(tabGroup);
                    newSplit.setSecond(sibling);
                } else {
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
        return element.accept(new DockElementVisitor<>() {
            @Override
            public DockElement visitTabGroup(DockTabGroup tabGroup) {
                for (DockLeaf tab : tabGroup.getTabs()) {
                    if (tab.getMetadata().id().equals(id)) {
                        return tabGroup;
                    }
                }
                return null;
            }

            @Override
            public DockElement visitSplitGroup(DockSplitGroup splitGroup) {
                DockElement found = findElementById(splitGroup.getFirst(), id);
                return found != null ? found : findElementById(splitGroup.getSecond(), id);
            }
        });
    }

    void collectLeaves(DockElement element, Collection<DockLeaf> leaves) {
        if (element == null) {
            return;
        }
        element.accept(new DockElementVisitor<>() {
            @Override
            public Void visitTabGroup(DockTabGroup tabGroup) {
                leaves.addAll(tabGroup.getTabs());
                return null;
            }

            @Override
            public Void visitSplitGroup(DockSplitGroup splitGroup) {
                collectLeaves(splitGroup.getFirst(), leaves);
                collectLeaves(splitGroup.getSecond(), leaves);
                return null;
            }
        });
    }

    private boolean restoreIntoParentTarget(DockElement target, DockLeaf leaf, RestoreHint hint) {
        return target.accept(new DockElementVisitor<>() {
            @Override
            public Boolean visitTabGroup(DockTabGroup tabGroup) {
                if (hint.zone() != DropZone.TAB_BAR) {
                    return false;
                }
                int index = Math.min(hint.tabIndex(), tabGroup.getTabs().size());
                tabGroup.addLeaf(index >= 0 ? index : tabGroup.getTabs().size(), leaf);
                return true;
            }

            @Override
            public Boolean visitSplitGroup(DockSplitGroup splitGroup) {
                DropZone zone = hint.zone();
                if (zone == DropZone.WEST || zone == DropZone.NORTH) {
                    if (splitGroup.getFirst() == null) {
                        splitGroup.setFirst(createTabGroupWithLeaf(leaf));
                        return true;
                    }
                } else if (zone == DropZone.EAST || zone == DropZone.SOUTH) {
                    if (splitGroup.getSecond() == null) {
                        splitGroup.setSecond(createTabGroupWithLeaf(leaf));
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void removeElementInternal(DockElement element, boolean disposeElement) {
        DockElement parent = element.getParent();
        if (parent == null) {
            manager.setRoot((DockElement) null);
            if (disposeElement) {
                element.dispose();
            }
            return;
        }

        parent.accept(new DockElementVisitor<>() {
            @Override
            public Void visitTabGroup(DockTabGroup tabGroup) {
                return null;
            }

            @Override
            public Void visitSplitGroup(DockSplitGroup splitGroup) {
                DockElement sibling = (splitGroup.getFirst() == element) ? splitGroup.getSecond() : splitGroup.getFirst();

                detachChild(splitGroup, element);
                detachChild(splitGroup, sibling);

                replaceChildAtParent(splitGroup.getParent(), splitGroup, sibling);
                splitGroup.dispose();
                return null;
            }
        });

        if (disposeElement) {
            element.dispose();
        }
    }

    private DockTabGroup createTabGroupWithLeaf(DockLeaf leaf) {
        DockTabGroup tabGroup = manager.createTabGroup();
        tabGroup.addLeaf(leaf);
        return tabGroup;
    }

    private void replaceChildAtParent(DockElement parent, DockElement oldChild, DockElement newChild) {
        if (parent == null) {
            manager.setRoot(newChild);
            return;
        }
        parent.accept(new DockElementVisitor<>() {
            @Override
            public Void visitTabGroup(DockTabGroup tabGroup) {
                return null;
            }

            @Override
            public Void visitSplitGroup(DockSplitGroup splitGroup) {
                splitGroup.replaceChild(oldChild, newChild);
                return null;
            }
        });
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
