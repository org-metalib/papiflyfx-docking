package org.metalib.papifly.fx.docks;

import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.floating.FloatingWindowManager;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;

public interface DockFloatingService {

    void setOwnerStage(Stage stage);

    boolean ensureFloatingWindowManager(String operation);

    FloatingWindowManager getFloatingWindowManager();

    boolean isFloating(DockLeaf leaf);

    void unfloatLeaf(DockLeaf leaf);

    void floatLeaf(DockLeaf leaf);

    void floatLeaf(DockLeaf leaf, double x, double y);

    void dockLeaf(DockLeaf leaf);

    boolean restoreFloating(DockLeaf leaf, RestoreHint restoreHint, Rectangle2D bounds);

    RestoreHint getRestoreHint(String leafId);

    void forgetRestoreHint(String leafId);

    void clearRestoreHints();

    void closeAll();

    void dispose();
}
