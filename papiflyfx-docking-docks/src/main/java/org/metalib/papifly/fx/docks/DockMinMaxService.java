package org.metalib.papifly.fx.docks;

import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.minimize.MinimizedBar;
import org.metalib.papifly.fx.docks.minimize.MinimizedStore;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;

public interface DockMinMaxService {

    MinimizedBar getMinimizedBar();

    MinimizedStore getMinimizedStore();

    void addMinimizedLeaf(DockLeaf leaf, RestoreHint restoreHint);

    void removeMinimizedLeaf(DockLeaf leaf);

    void clearMinimized();

    void minimizeLeaf(DockLeaf leaf);

    void restoreLeaf(DockLeaf leaf);

    void restoreLeaf(String leafId);

    void maximizeLeaf(DockLeaf leaf);

    void restoreMaximized();

    boolean isMaximized();

    DockLeaf getMaximizedLeaf();

    RestoreHint getMaximizeRestoreHint();

    void dispose();
}
