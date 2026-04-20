package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockState;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.LayoutFactory;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;

import java.util.List;

interface DockManagerContext {

    DockElement getRoot();

    void setRoot(DockElement element);

    void restore(LayoutNode layout);

    ObjectProperty<DockElement> rootProperty();

    ObjectProperty<Theme> themeProperty();

    LayoutFactory getLayoutFactory();

    DockTreeService getTreeService();

    DockTabGroup createTabGroup();

    void setupLeafCloseHandler(DockLeaf leaf);

    void updateLeafState(DockLeaf leaf, DockState state);

    void restoreMaximizedIfNecessary(DockLeaf leaf);

    void closeLeaf(DockLeaf leaf);

    StackPane getDockingLayer();

    StackPane getRootStack();

    List<DockSessionStateContributor> getSessionStateContributors();
}
