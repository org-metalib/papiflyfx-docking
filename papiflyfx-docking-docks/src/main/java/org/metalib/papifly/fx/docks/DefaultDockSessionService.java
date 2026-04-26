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

final class DefaultDockSessionService implements DockSessionService {

    private static final Logger LOG = Logger.getLogger(DefaultDockSessionService.class.getName());

    private final DockManagerContext context;
    private final DockTreeService treeService;
    private final DockFloatingService floatingService;
    private final DockMinMaxService minMaxService;
    private final DockSessionPersistence persistence;

    DefaultDockSessionService(
        DockManagerContext context,
        DockTreeService treeService,
        DockFloatingService floatingService,
        DockMinMaxService minMaxService
    ) {
        this.context = context;
        this.treeService = treeService;
        this.floatingService = floatingService;
        this.minMaxService = minMaxService;
        this.persistence = new DockSessionPersistence();
    }

    @Override
    public LayoutNode captureLayout() {
        refreshContentStatesForLayout();
        DockElement root = context.getRoot();
        return root != null ? root.serialize() : null;
    }

    @Override
    public DockSessionData captureSession() {
        refreshContentStatesForSession();
        DockElement root = context.getRoot();
        LayoutNode layout = root != null ? root.serialize() : null;

        List<FloatingLeafData> floatingList = new ArrayList<>();
        FloatingWindowManager floatingWindowManager = floatingService.getFloatingWindowManager();
        if (floatingWindowManager != null) {
            for (FloatingDockWindow window : floatingWindowManager.getFloatingWindows()) {
                DockLeaf leaf = window.getLeaf();
                String leafId = leaf.getMetadata().id();
                LeafData leafData = (LeafData) leaf.serialize();
                Rectangle2D bounds = window.getBounds();
                BoundsData boundsData = bounds != null
                    ? new BoundsData(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight())
                    : null;
                RestoreHint hint = floatingService.getRestoreHint(leafId);
                floatingList.add(new FloatingLeafData(leafData, boundsData, toRestoreHintData(hint)));
            }
        }

        List<MinimizedLeafData> minimizedList = new ArrayList<>();
        MinimizedStore minimizedStore = minMaxService.getMinimizedStore();
        for (DockLeaf leaf : minimizedStore.getMinimizedLeaves()) {
            LeafData leafData = (LeafData) leaf.serialize();
            minimizedList.add(new MinimizedLeafData(leafData, toRestoreHintData(minimizedStore.getRestoreHint(leaf))));
        }

        MaximizedLeafData maximizedData = null;
        DockLeaf maximizedLeaf = minMaxService.getMaximizedLeaf();
        if (maximizedLeaf != null) {
            LeafData leafData = (LeafData) maximizedLeaf.serialize();
            maximizedData = new MaximizedLeafData(leafData, toRestoreHintData(minMaxService.getMaximizeRestoreHint()));
        }

        DockSessionData session = DockSessionData.of(layout, floatingList, minimizedList, maximizedData);
        return applySessionContributorsOnCapture(session);
    }

    @Override
    public void restoreSession(DockSessionData session) {
        if (session == null) {
            return;
        }

        if (minMaxService.getMaximizedLeaf() != null) {
            minMaxService.restoreMaximized();
        }

        floatingService.closeAll();
        floatingService.clearRestoreHints();
        minMaxService.clearMinimized();

        if (session.layout() != null) {
            context.restore(session.layout());
        }

        boolean canRestoreFloating = true;
        if (session.floating() != null && !session.floating().isEmpty()) {
            canRestoreFloating = floatingService.ensureFloatingWindowManager("restore floating leaves");
        }
        if (session.floating() != null) {
            for (FloatingLeafData floatingData : session.floating()) {
                if (floatingData.leaf() == null) {
                    continue;
                }

                DockLeaf leaf = context.getLayoutFactory().buildLeaf(floatingData.leaf());
                context.setupLeafCloseHandler(leaf);

                RestoreHint hint = toRestoreHint(floatingData.restoreHint());
                Rectangle2D bounds = toRectangle2D(floatingData.bounds());

                if (canRestoreFloating && floatingService.restoreFloating(leaf, hint, bounds)) {
                    continue;
                }
                if (hint != null && treeService.tryRestoreWithHint(leaf, hint)) {
                    context.updateLeafState(leaf, DockState.DOCKED);
                    continue;
                }
                treeService.insertLeafIntoDock(leaf);
                context.updateLeafState(leaf, DockState.DOCKED);
            }
        }

        if (session.minimized() != null) {
            for (MinimizedLeafData minimizedData : session.minimized()) {
                if (minimizedData.leaf() == null) {
                    continue;
                }

                DockLeaf leaf = context.getLayoutFactory().buildLeaf(minimizedData.leaf());
                context.setupLeafCloseHandler(leaf);
                minMaxService.addMinimizedLeaf(leaf, toRestoreHint(minimizedData.restoreHint()));
            }
        }

        applySessionContributorsOnRestore(session);
    }

    @Override
    public String saveSessionToString() {
        String json = persistence.toJsonString(captureSession());
        return json != null ? json : "";
    }

    @Override
    public void restoreSessionFromString(String json) {
        DockSessionData session = persistence.fromJsonString(json);
        if (session != null) {
            restoreSession(session);
        }
    }

    @Override
    public void saveSessionToFile(Path path) {
        persistence.toJsonFile(captureSession(), path);
    }

    @Override
    public void loadSessionFromFile(Path path) {
        DockSessionData session = persistence.fromJsonFile(path);
        if (session != null) {
            restoreSession(session);
        }
    }

    private void refreshContentStatesForLayout() {
        ContentStateRegistry registry = context.getLayoutFactory().getContentStateRegistry();
        if (registry == null || registry.isEmpty()) {
            return;
        }
        DockElement root = context.getRoot();
        if (root == null) {
            return;
        }
        List<DockLeaf> leaves = new ArrayList<>();
        treeService.collectLeaves(root, leaves);
        refreshContentStates(leaves, registry);
    }

    private void refreshContentStatesForSession() {
        ContentStateRegistry registry = context.getLayoutFactory().getContentStateRegistry();
        if (registry == null || registry.isEmpty()) {
            return;
        }
        Collection<DockLeaf> leaves = new LinkedHashSet<>();
        treeService.collectLeaves(context.getRoot(), leaves);

        FloatingWindowManager floatingWindowManager = floatingService.getFloatingWindowManager();
        if (floatingWindowManager != null) {
            for (FloatingDockWindow window : floatingWindowManager.getFloatingWindows()) {
                leaves.add(window.getLeaf());
            }
        }

        leaves.addAll(minMaxService.getMinimizedStore().getMinimizedLeaves());

        DockLeaf maximizedLeaf = minMaxService.getMaximizedLeaf();
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

    private DockSessionData applySessionContributorsOnCapture(DockSessionData session) {
        DockSessionData current = session;
        for (DockSessionStateContributor<?> contributor : context.getSessionStateContributors()) {
            if (contributor == null || current == null) {
                continue;
            }
            try {
                current = captureContributorState(current, contributor);
            } catch (RuntimeException exception) {
                LOG.log(Level.WARNING, "Session contributor capture failed: " + contributorDescription(contributor), exception);
            }
        }
        return current;
    }

    private void applySessionContributorsOnRestore(DockSessionData session) {
        for (DockSessionStateContributor<?> contributor : context.getSessionStateContributors()) {
            if (contributor == null) {
                continue;
            }
            try {
                restoreContributorState(session, contributor);
            } catch (RuntimeException exception) {
                LOG.log(Level.WARNING, "Session contributor restore failed: " + contributorDescription(contributor), exception);
            }
        }
    }

    private <T> DockSessionData captureContributorState(
        DockSessionData session,
        DockSessionStateContributor<T> contributor
    ) {
        T contributorState = contributor.captureSessionState();
        if (contributorState == null) {
            return session.withoutExtension(contributor.extensionNamespace());
        }
        Map<String, Object> payload = contributor.codec().encode(contributorState);
        if (payload == null) {
            throw new IllegalArgumentException(
                "Session contributor codec returned null payload for namespace " + contributor.extensionNamespace()
            );
        }
        return session.withExtension(contributor.extensionNamespace(), payload);
    }

    private <T> void restoreContributorState(
        DockSessionData session,
        DockSessionStateContributor<T> contributor
    ) {
        Map<String, Object> payload = session.extension(contributor.extensionNamespace());
        if (payload == null) {
            return;
        }
        T contributorState = contributor.codec().decode(payload);
        if (contributorState == null) {
            return;
        }
        contributor.restoreSessionState(contributorState);
    }

    private String contributorDescription(DockSessionStateContributor<?> contributor) {
        return contributor.getClass().getName() + "[namespace=" + contributor.extensionNamespace() + "]";
    }
}
