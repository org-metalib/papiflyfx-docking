package org.metalib.papifly.fx.docks;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockState;
import org.metalib.papifly.fx.docks.drag.DropZone;
import org.metalib.papifly.fx.docks.floating.FloatingDockWindow;
import org.metalib.papifly.fx.docks.floating.FloatingWindowManager;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docks.layout.LayoutFactory;
import org.metalib.papifly.fx.docks.layout.data.BoundsData;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.FloatingLeafData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.MaximizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.MinimizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.RestoreHintData;
import org.metalib.papifly.fx.docks.minimize.MinimizedStore;
import org.metalib.papifly.fx.docks.minimize.RestoreHint;
import org.metalib.papifly.fx.docks.serial.DockSessionPersistence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DockSessionService {

    private static final Logger LOG = Logger.getLogger(DockSessionService.class.getName());

    private final DockManager manager;
    private final DockTreeService treeService;
    private final DockSessionPersistence persistence;

    DockSessionService(DockManager manager, DockTreeService treeService) {
        this.manager = manager;
        this.treeService = treeService;
        this.persistence = new DockSessionPersistence();
    }

    LayoutNode captureLayout() {
        refreshContentStatesForLayout();
        DockElement root = manager.getRoot();
        return root != null ? root.serialize() : null;
    }

    DockSessionData captureSession() {
        refreshContentStatesForSession();
        DockElement root = manager.getRoot();
        LayoutNode layout = root != null ? root.serialize() : null;

        List<FloatingLeafData> floatingList = new ArrayList<>();
        FloatingWindowManager floatingWindowManager = manager.getFloatingWindowManager();
        if (floatingWindowManager != null) {
            for (FloatingDockWindow window : floatingWindowManager.getFloatingWindows()) {
                DockLeaf leaf = window.getLeaf();
                String leafId = leaf.getMetadata().id();
                LeafData leafData = (LeafData) leaf.serialize();
                Rectangle2D bounds = window.getBounds();
                BoundsData boundsData = bounds != null
                    ? new BoundsData(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight())
                    : null;
                RestoreHint hint = manager.floatingRestoreHints().get(leafId);
                floatingList.add(new FloatingLeafData(leafData, boundsData, toRestoreHintData(hint)));
            }
        }

        List<MinimizedLeafData> minimizedList = new ArrayList<>();
        MinimizedStore minimizedStore = manager.getMinimizedStore();
        for (DockLeaf leaf : minimizedStore.getMinimizedLeaves()) {
            LeafData leafData = (LeafData) leaf.serialize();
            minimizedList.add(new MinimizedLeafData(leafData, toRestoreHintData(minimizedStore.getRestoreHint(leaf))));
        }

        MaximizedLeafData maximizedData = null;
        DockLeaf maximizedLeaf = manager.getMaximizedLeaf();
        if (maximizedLeaf != null) {
            LeafData leafData = (LeafData) maximizedLeaf.serialize();
            maximizedData = new MaximizedLeafData(leafData, toRestoreHintData(manager.maximizeRestoreHint()));
        }

        return DockSessionData.of(layout, floatingList, minimizedList, maximizedData);
    }

    void restoreSession(DockSessionData session) {
        if (session == null) {
            return;
        }

        if (manager.getMaximizedLeaf() != null) {
            manager.restoreMaximized();
        }

        FloatingWindowManager floatingWindowManager = manager.getFloatingWindowManager();
        if (floatingWindowManager != null) {
            floatingWindowManager.closeAll();
        }
        manager.floatingRestoreHints().clear();
        manager.getMinimizedStore().clear();

        if (session.layout() != null) {
            manager.restore(session.layout());
        }

        boolean canRestoreFloating = true;
        if (session.floating() != null && !session.floating().isEmpty()) {
            canRestoreFloating = manager.ensureFloatingWindowManager("restore floating leaves");
        }
        if (session.floating() != null) {
            for (FloatingLeafData floatingData : session.floating()) {
                if (floatingData.leaf() == null) {
                    continue;
                }

                DockLeaf leaf = manager.getLayoutFactory().buildLeaf(floatingData.leaf());
                manager.setupLeafCloseHandler(leaf);

                RestoreHint hint = toRestoreHint(floatingData.restoreHint());
                Rectangle2D bounds = toRectangle2D(floatingData.bounds());

                if (canRestoreFloating && manager.restoreFloating(leaf, hint, bounds)) {
                    continue;
                }
                if (hint != null && treeService.tryRestoreWithHint(leaf, hint)) {
                    manager.updateLeafState(leaf, DockState.DOCKED);
                    continue;
                }
                treeService.insertLeafIntoDock(leaf);
                manager.updateLeafState(leaf, DockState.DOCKED);
            }
        }

        if (session.minimized() != null) {
            for (MinimizedLeafData minimizedData : session.minimized()) {
                if (minimizedData.leaf() == null) {
                    continue;
                }

                DockLeaf leaf = manager.getLayoutFactory().buildLeaf(minimizedData.leaf());
                manager.setupLeafCloseHandler(leaf);
                manager.updateLeafState(leaf, DockState.MINIMIZED);
                manager.getMinimizedStore().addLeaf(leaf, toRestoreHint(minimizedData.restoreHint()));
            }
        }
    }

    String saveSessionToString() {
        String json = persistence.toJsonString(captureSession());
        return json != null ? json : "";
    }

    void restoreSessionFromString(String json) {
        DockSessionData session = persistence.fromJsonString(json);
        if (session != null) {
            restoreSession(session);
        }
    }

    void saveSessionToFile(Path path) {
        persistence.toJsonFile(captureSession(), path);
    }

    void loadSessionFromFile(Path path) {
        DockSessionData session = persistence.fromJsonFile(path);
        if (session != null) {
            restoreSession(session);
        }
    }

    private void refreshContentStatesForLayout() {
        ContentStateRegistry registry = manager.getLayoutFactory().getContentStateRegistry();
        if (registry == null || registry.isEmpty()) {
            return;
        }
        DockElement root = manager.getRoot();
        if (root == null) {
            return;
        }
        List<DockLeaf> leaves = new ArrayList<>();
        treeService.collectLeaves(root, leaves);
        refreshContentStates(leaves, registry);
    }

    private void refreshContentStatesForSession() {
        ContentStateRegistry registry = manager.getLayoutFactory().getContentStateRegistry();
        if (registry == null || registry.isEmpty()) {
            return;
        }
        Collection<DockLeaf> leaves = new LinkedHashSet<>();
        treeService.collectLeaves(manager.getRoot(), leaves);

        FloatingWindowManager floatingWindowManager = manager.getFloatingWindowManager();
        if (floatingWindowManager != null) {
            for (FloatingDockWindow window : floatingWindowManager.getFloatingWindows()) {
                leaves.add(window.getLeaf());
            }
        }

        leaves.addAll(manager.getMinimizedStore().getMinimizedLeaves());

        DockLeaf maximizedLeaf = manager.getMaximizedLeaf();
        if (maximizedLeaf != null) {
            leaves.add(maximizedLeaf);
        }

        refreshContentStates(leaves, registry);
    }

    private void refreshContentStates(Collection<DockLeaf> leaves, ContentStateRegistry registry) {
        for (DockLeaf leaf : leaves) {
            refreshContentState(leaf, registry);
        }
    }

    private void refreshContentState(DockLeaf leaf, ContentStateRegistry registry) {
        if (leaf == null || registry == null) {
            return;
        }

        LeafContentData existing = leaf.getContentData();
        String typeKey = existing != null && existing.typeKey() != null
            ? existing.typeKey()
            : leaf.getContentFactoryId();
        if (typeKey == null) {
            return;
        }

        ContentStateAdapter adapter = registry.getAdapter(typeKey);
        if (adapter == null) {
            return;
        }

        String contentId = existing != null && existing.contentId() != null
            ? existing.contentId()
            : leaf.getMetadata().id();
        if (contentId == null) {
            return;
        }

        Node content = leaf.getContent();
        if (content == null) {
            return;
        }

        try {
            Map<String, Object> state = adapter.saveState(contentId, content);
            leaf.setContentData(new LeafContentData(typeKey, contentId, adapter.getVersion(), state));
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Adapter saveState failed for typeKey=" + typeKey
                + ", keeping previous contentData", exception);
        }
    }

    private RestoreHintData toRestoreHintData(RestoreHint hint) {
        if (hint == null) {
            return null;
        }
        return new RestoreHintData(
            hint.parentId(),
            hint.zone() != null ? hint.zone().name() : null,
            hint.tabIndex(),
            hint.splitPosition(),
            hint.siblingId()
        );
    }

    private RestoreHint toRestoreHint(RestoreHintData hintData) {
        if (hintData == null) {
            return null;
        }
        DropZone zone = hintData.zone() != null ? DropZone.valueOf(hintData.zone()) : null;
        return new RestoreHint(
            hintData.parentId(),
            zone,
            hintData.tabIndex(),
            hintData.splitPosition(),
            hintData.siblingId()
        );
    }

    private Rectangle2D toRectangle2D(BoundsData boundsData) {
        if (boundsData == null) {
            return null;
        }
        return new Rectangle2D(boundsData.x(), boundsData.y(), boundsData.width(), boundsData.height());
    }
}
