package org.metalib.papifly.fx.docks.serial;

import org.metalib.papifly.fx.docks.layout.data.BoundsData;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.FloatingLeafData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.MaximizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.MinimizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.RestoreHintData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes and deserializes dock session data to/from Map structures.
 * Uses LayoutSerializer for layout node encoding plus floating, minimized, and maximized state.
 */
public class DockSessionSerializer {

    private static final String TYPE_KEY = "type";
    private static final String VERSION_KEY = "version";
    private static final String LAYOUT_KEY = "layout";
    private static final String FLOATING_KEY = "floating";
    private static final String MINIMIZED_KEY = "minimized";
    private static final String MAXIMIZED_KEY = "maximized";

    private static final String LEAF_KEY = "leaf";
    private static final String BOUNDS_KEY = "bounds";
    private static final String RESTORE_HINT_KEY = "restoreHint";

    private static final String BOUNDS_X_KEY = "x";
    private static final String BOUNDS_Y_KEY = "y";
    private static final String BOUNDS_WIDTH_KEY = "width";
    private static final String BOUNDS_HEIGHT_KEY = "height";

    private static final String HINT_PARENT_ID_KEY = "parentId";
    private static final String HINT_ZONE_KEY = "zone";
    private static final String HINT_TAB_INDEX_KEY = "tabIndex";
    private static final String HINT_SPLIT_POSITION_KEY = "splitPosition";
    private static final String HINT_SIBLING_ID_KEY = "siblingId";

    private static final String TYPE_DOCK_SESSION = "dockSession";

    private final LayoutSerializer layoutSerializer;

    /**
     * Creates a new DockSessionSerializer with default layout serializer.
     */
    public DockSessionSerializer() {
        this(new LayoutSerializer());
    }

    /**
     * Creates a new DockSessionSerializer with custom layout serializer.
     */
    public DockSessionSerializer(LayoutSerializer layoutSerializer) {
        this.layoutSerializer = layoutSerializer;
    }

    /**
     * Serializes a DockSessionData to a Map.
     */
    public Map<String, Object> serialize(DockSessionData session) {
        if (session == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(TYPE_KEY, TYPE_DOCK_SESSION);
        map.put(VERSION_KEY, session.version());

        // Serialize layout tree
        if (session.layout() != null) {
            map.put(LAYOUT_KEY, layoutSerializer.serialize(session.layout()));
        }

        // Serialize floating leaves
        if (session.floating() != null && !session.floating().isEmpty()) {
            List<Map<String, Object>> floatingList = new ArrayList<>();
            for (FloatingLeafData floating : session.floating()) {
                floatingList.add(serializeFloating(floating));
            }
            map.put(FLOATING_KEY, floatingList);
        }

        // Serialize minimized leaves
        if (session.minimized() != null && !session.minimized().isEmpty()) {
            List<Map<String, Object>> minimizedList = new ArrayList<>();
            for (MinimizedLeafData minimized : session.minimized()) {
                minimizedList.add(serializeMinimized(minimized));
            }
            map.put(MINIMIZED_KEY, minimizedList);
        }

        // Serialize maximized leaf
        if (session.maximized() != null) {
            map.put(MAXIMIZED_KEY, serializeMaximized(session.maximized()));
        }

        return map;
    }

    private Map<String, Object> serializeFloating(FloatingLeafData floating) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (floating.leaf() != null) {
            map.put(LEAF_KEY, layoutSerializer.serialize(floating.leaf()));
        }

        if (floating.bounds() != null) {
            map.put(BOUNDS_KEY, serializeBounds(floating.bounds()));
        }

        if (floating.restoreHint() != null) {
            map.put(RESTORE_HINT_KEY, serializeRestoreHint(floating.restoreHint()));
        }

        return map;
    }

    private Map<String, Object> serializeMinimized(MinimizedLeafData minimized) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (minimized.leaf() != null) {
            map.put(LEAF_KEY, layoutSerializer.serialize(minimized.leaf()));
        }

        if (minimized.restoreHint() != null) {
            map.put(RESTORE_HINT_KEY, serializeRestoreHint(minimized.restoreHint()));
        }

        return map;
    }

    private Map<String, Object> serializeMaximized(MaximizedLeafData maximized) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (maximized.leaf() != null) {
            map.put(LEAF_KEY, layoutSerializer.serialize(maximized.leaf()));
        }

        if (maximized.restoreHint() != null) {
            map.put(RESTORE_HINT_KEY, serializeRestoreHint(maximized.restoreHint()));
        }

        return map;
    }

    private Map<String, Object> serializeBounds(BoundsData bounds) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(BOUNDS_X_KEY, bounds.x());
        map.put(BOUNDS_Y_KEY, bounds.y());
        map.put(BOUNDS_WIDTH_KEY, bounds.width());
        map.put(BOUNDS_HEIGHT_KEY, bounds.height());
        return map;
    }

    private Map<String, Object> serializeRestoreHint(RestoreHintData hint) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (hint.parentId() != null) {
            map.put(HINT_PARENT_ID_KEY, hint.parentId());
        }
        if (hint.zone() != null) {
            map.put(HINT_ZONE_KEY, hint.zone());
        }
        map.put(HINT_TAB_INDEX_KEY, hint.tabIndex());
        map.put(HINT_SPLIT_POSITION_KEY, hint.splitPosition());
        if (hint.siblingId() != null) {
            map.put(HINT_SIBLING_ID_KEY, hint.siblingId());
        }
        return map;
    }

    /**
     * Deserializes a Map to a DockSessionData.
     */
    @SuppressWarnings("unchecked")
    public DockSessionData deserialize(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        String type = (String) map.get(TYPE_KEY);
        if (type == null || !TYPE_DOCK_SESSION.equals(type)) {
            throw new IllegalArgumentException("Invalid session type: " + type);
        }

        int version = ((Number) map.getOrDefault(VERSION_KEY, DockSessionData.CURRENT_VERSION)).intValue();

        // Deserialize layout tree
        LayoutNode layout = null;
        if (map.containsKey(LAYOUT_KEY)) {
            Map<String, Object> layoutMap = (Map<String, Object>) map.get(LAYOUT_KEY);
            layout = layoutSerializer.deserialize(layoutMap);
        }

        // Deserialize floating leaves
        List<FloatingLeafData> floating = new ArrayList<>();
        if (map.containsKey(FLOATING_KEY)) {
            List<Map<String, Object>> floatingList = (List<Map<String, Object>>) map.get(FLOATING_KEY);
            for (Map<String, Object> floatingMap : floatingList) {
                floating.add(deserializeFloating(floatingMap));
            }
        }

        // Deserialize minimized leaves
        List<MinimizedLeafData> minimized = new ArrayList<>();
        if (map.containsKey(MINIMIZED_KEY)) {
            List<Map<String, Object>> minimizedList = (List<Map<String, Object>>) map.get(MINIMIZED_KEY);
            for (Map<String, Object> minimizedMap : minimizedList) {
                minimized.add(deserializeMinimized(minimizedMap));
            }
        }

        // Deserialize maximized leaf
        MaximizedLeafData maximized = null;
        if (map.containsKey(MAXIMIZED_KEY)) {
            Map<String, Object> maximizedMap = (Map<String, Object>) map.get(MAXIMIZED_KEY);
            maximized = deserializeMaximized(maximizedMap);
        }

        return new DockSessionData(version, layout, floating, minimized, maximized);
    }

    @SuppressWarnings("unchecked")
    private FloatingLeafData deserializeFloating(Map<String, Object> map) {
        LeafData leaf = null;
        if (map.containsKey(LEAF_KEY)) {
            Map<String, Object> leafMap = (Map<String, Object>) map.get(LEAF_KEY);
            LayoutNode node = layoutSerializer.deserialize(leafMap);
            if (node instanceof LeafData leafData) {
                leaf = leafData;
            }
        }

        BoundsData bounds = null;
        if (map.containsKey(BOUNDS_KEY)) {
            Map<String, Object> boundsMap = (Map<String, Object>) map.get(BOUNDS_KEY);
            bounds = deserializeBounds(boundsMap);
        }

        RestoreHintData restoreHint = null;
        if (map.containsKey(RESTORE_HINT_KEY)) {
            Map<String, Object> hintMap = (Map<String, Object>) map.get(RESTORE_HINT_KEY);
            restoreHint = deserializeRestoreHint(hintMap);
        }

        return new FloatingLeafData(leaf, bounds, restoreHint);
    }

    @SuppressWarnings("unchecked")
    private MinimizedLeafData deserializeMinimized(Map<String, Object> map) {
        LeafData leaf = null;
        if (map.containsKey(LEAF_KEY)) {
            Map<String, Object> leafMap = (Map<String, Object>) map.get(LEAF_KEY);
            LayoutNode node = layoutSerializer.deserialize(leafMap);
            if (node instanceof LeafData leafData) {
                leaf = leafData;
            }
        }

        RestoreHintData restoreHint = null;
        if (map.containsKey(RESTORE_HINT_KEY)) {
            Map<String, Object> hintMap = (Map<String, Object>) map.get(RESTORE_HINT_KEY);
            restoreHint = deserializeRestoreHint(hintMap);
        }

        return new MinimizedLeafData(leaf, restoreHint);
    }

    @SuppressWarnings("unchecked")
    private MaximizedLeafData deserializeMaximized(Map<String, Object> map) {
        LeafData leaf = null;
        if (map.containsKey(LEAF_KEY)) {
            Map<String, Object> leafMap = (Map<String, Object>) map.get(LEAF_KEY);
            LayoutNode node = layoutSerializer.deserialize(leafMap);
            if (node instanceof LeafData leafData) {
                leaf = leafData;
            }
        }

        RestoreHintData restoreHint = null;
        if (map.containsKey(RESTORE_HINT_KEY)) {
            Map<String, Object> hintMap = (Map<String, Object>) map.get(RESTORE_HINT_KEY);
            restoreHint = deserializeRestoreHint(hintMap);
        }

        return new MaximizedLeafData(leaf, restoreHint);
    }

    private BoundsData deserializeBounds(Map<String, Object> map) {
        double x = ((Number) map.getOrDefault(BOUNDS_X_KEY, 0.0)).doubleValue();
        double y = ((Number) map.getOrDefault(BOUNDS_Y_KEY, 0.0)).doubleValue();
        double width = ((Number) map.getOrDefault(BOUNDS_WIDTH_KEY, 400.0)).doubleValue();
        double height = ((Number) map.getOrDefault(BOUNDS_HEIGHT_KEY, 300.0)).doubleValue();
        return new BoundsData(x, y, width, height);
    }

    private RestoreHintData deserializeRestoreHint(Map<String, Object> map) {
        String parentId = (String) map.get(HINT_PARENT_ID_KEY);
        String zone = (String) map.get(HINT_ZONE_KEY);
        int tabIndex = ((Number) map.getOrDefault(HINT_TAB_INDEX_KEY, -1)).intValue();
        double splitPosition = ((Number) map.getOrDefault(HINT_SPLIT_POSITION_KEY, 0.5)).doubleValue();
        String siblingId = (String) map.get(HINT_SIBLING_ID_KEY);
        return new RestoreHintData(parentId, zone, tabIndex, splitPosition, siblingId);
    }

    /**
     * Converts a Map to JSON string using LayoutSerializer's JSON utility.
     */
    public String toJson(Map<String, Object> map) {
        return layoutSerializer.toJson(map);
    }

    /**
     * Converts a JSON string to Map using LayoutSerializer's JSON utility.
     */
    public Map<String, Object> fromJson(String json) {
        return layoutSerializer.fromJson(json);
    }
}
