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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializes and deserializes dock session data to/from Map structures.
 * Uses LayoutSerializer for layout node encoding plus floating, minimized,
 * maximized, and namespaced extension state.
 */
public class DockSessionSerializer {

    private static final Logger LOG = Logger.getLogger(DockSessionSerializer.class.getName());

    private static final String TYPE_KEY = "type";
    private static final String VERSION_KEY = "version";
    private static final String LAYOUT_KEY = "layout";
    private static final String FLOATING_KEY = "floating";
    private static final String MINIMIZED_KEY = "minimized";
    private static final String MAXIMIZED_KEY = "maximized";
    private static final String EXTENSIONS_KEY = "extensions";

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
    private static final String ROOT_PATH = TYPE_DOCK_SESSION;

    private final LayoutSerializer layoutSerializer;

    /**
     * Creates a new DockSessionSerializer with default layout serializer.
     */
    public DockSessionSerializer() {
        this(new LayoutSerializer());
    }

    /**
     * Creates a new DockSessionSerializer with custom layout serializer.
     *
     * @param layoutSerializer layout serializer used for layout node conversion
     */
    public DockSessionSerializer(LayoutSerializer layoutSerializer) {
        this.layoutSerializer = layoutSerializer;
    }

    /**
     * Serializes a DockSessionData to a Map.
     *
     * @param session session data to serialize
     * @return serialized session map, or {@code null} when session is {@code null}
     */
    public Map<String, Object> serialize(DockSessionData session) {
        if (session == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(TYPE_KEY, TYPE_DOCK_SESSION);
        map.put(VERSION_KEY, session.version());

        if (session.layout() != null) {
            map.put(LAYOUT_KEY, layoutSerializer.serialize(session.layout()));
        }

        if (!session.floating().isEmpty()) {
            List<Map<String, Object>> floatingList = new ArrayList<>();
            for (FloatingLeafData floating : session.floating()) {
                floatingList.add(serializeFloating(floating));
            }
            map.put(FLOATING_KEY, floatingList);
        }

        if (!session.minimized().isEmpty()) {
            List<Map<String, Object>> minimizedList = new ArrayList<>();
            for (MinimizedLeafData minimized : session.minimized()) {
                minimizedList.add(serializeMinimized(minimized));
            }
            map.put(MINIMIZED_KEY, minimizedList);
        }

        if (session.maximized() != null) {
            map.put(MAXIMIZED_KEY, serializeMaximized(session.maximized()));
        }

        if (!session.extensions().isEmpty()) {
            map.put(EXTENSIONS_KEY, serializeExtensions(session.extensions()));
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

    private Map<String, Object> serializeExtensions(Map<String, Map<String, Object>> extensions) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : extensions.entrySet()) {
            map.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return map;
    }

    /**
     * Deserializes a Map to a DockSessionData.
     *
     * @param map serialized session map
     * @return deserialized session data, or {@code null} when input is {@code null}
     */
    public DockSessionData deserialize(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        String type = requiredString(map, TYPE_KEY, ROOT_PATH);
        if (!TYPE_DOCK_SESSION.equals(type)) {
            throw new IllegalArgumentException("Invalid session type: " + type);
        }

        int version = optionalInt(map, VERSION_KEY, ROOT_PATH, DockSessionData.CURRENT_VERSION);

        LayoutNode layout = null;
        Map<String, Object> layoutMap = optionalMap(map, LAYOUT_KEY, ROOT_PATH);
        if (layoutMap != null) {
            layout = layoutSerializer.deserialize(layoutMap);
        }

        List<FloatingLeafData> floating = new ArrayList<>();
        List<Object> floatingEntries = optionalList(map, FLOATING_KEY, ROOT_PATH);
        for (int index = 0; index < floatingEntries.size(); index++) {
            String entryPath = ROOT_PATH + "." + FLOATING_KEY + "[" + index + "]";
            floating.add(deserializeFloating(requireMapEntry(floatingEntries, index, ROOT_PATH + "." + FLOATING_KEY), entryPath));
        }

        List<MinimizedLeafData> minimized = new ArrayList<>();
        List<Object> minimizedEntries = optionalList(map, MINIMIZED_KEY, ROOT_PATH);
        for (int index = 0; index < minimizedEntries.size(); index++) {
            String entryPath = ROOT_PATH + "." + MINIMIZED_KEY + "[" + index + "]";
            minimized.add(deserializeMinimized(requireMapEntry(minimizedEntries, index, ROOT_PATH + "." + MINIMIZED_KEY), entryPath));
        }

        MaximizedLeafData maximized = null;
        Map<String, Object> maximizedMap = optionalMap(map, MAXIMIZED_KEY, ROOT_PATH);
        if (maximizedMap != null) {
            maximized = deserializeMaximized(maximizedMap, ROOT_PATH + "." + MAXIMIZED_KEY);
        }

        Map<String, Map<String, Object>> extensions = deserializeExtensions(map);

        return new DockSessionData(version, layout, floating, minimized, maximized, extensions);
    }

    private FloatingLeafData deserializeFloating(Map<String, Object> map, String path) {
        LeafData leaf = deserializeLeaf(map, LEAF_KEY, path);
        BoundsData bounds = deserializeBounds(optionalMap(map, BOUNDS_KEY, path), path + "." + BOUNDS_KEY);
        RestoreHintData restoreHint = deserializeRestoreHint(
            optionalMap(map, RESTORE_HINT_KEY, path),
            path + "." + RESTORE_HINT_KEY
        );
        return new FloatingLeafData(leaf, bounds, restoreHint);
    }

    private MinimizedLeafData deserializeMinimized(Map<String, Object> map, String path) {
        LeafData leaf = deserializeLeaf(map, LEAF_KEY, path);
        RestoreHintData restoreHint = deserializeRestoreHint(
            optionalMap(map, RESTORE_HINT_KEY, path),
            path + "." + RESTORE_HINT_KEY
        );
        return new MinimizedLeafData(leaf, restoreHint);
    }

    private MaximizedLeafData deserializeMaximized(Map<String, Object> map, String path) {
        LeafData leaf = deserializeLeaf(map, LEAF_KEY, path);
        RestoreHintData restoreHint = deserializeRestoreHint(
            optionalMap(map, RESTORE_HINT_KEY, path),
            path + "." + RESTORE_HINT_KEY
        );
        return new MaximizedLeafData(leaf, restoreHint);
    }

    private LeafData deserializeLeaf(Map<String, Object> map, String key, String path) {
        Map<String, Object> leafMap = optionalMap(map, key, path);
        if (leafMap == null) {
            return null;
        }
        LayoutNode node = layoutSerializer.deserialize(leafMap);
        if (node instanceof LeafData leafData) {
            return leafData;
        }
        throw new IllegalArgumentException("Invalid " + path + "." + key + ": expected leaf node but found " + describeType(node));
    }

    private BoundsData deserializeBounds(Map<String, Object> map, String path) {
        if (map == null) {
            return null;
        }
        double x = optionalDouble(map, BOUNDS_X_KEY, path, 0.0);
        double y = optionalDouble(map, BOUNDS_Y_KEY, path, 0.0);
        double width = optionalDouble(map, BOUNDS_WIDTH_KEY, path, 400.0);
        double height = optionalDouble(map, BOUNDS_HEIGHT_KEY, path, 300.0);
        return new BoundsData(x, y, width, height);
    }

    private RestoreHintData deserializeRestoreHint(Map<String, Object> map, String path) {
        if (map == null) {
            return null;
        }
        String parentId = optionalString(map, HINT_PARENT_ID_KEY, path);
        String zone = optionalString(map, HINT_ZONE_KEY, path);
        int tabIndex = optionalInt(map, HINT_TAB_INDEX_KEY, path, -1);
        double splitPosition = optionalDouble(map, HINT_SPLIT_POSITION_KEY, path, 0.5);
        String siblingId = optionalString(map, HINT_SIBLING_ID_KEY, path);
        return new RestoreHintData(parentId, zone, tabIndex, splitPosition, siblingId);
    }

    private Map<String, Map<String, Object>> deserializeExtensions(Map<String, Object> map) {
        Object rawExtensions = map.get(EXTENSIONS_KEY);
        if (rawExtensions == null) {
            return Map.of();
        }
        if (!(rawExtensions instanceof Map<?, ?> extensionsValue)) {
            LOG.warning(
                "Ignoring dock session extensions because the container is not an object: "
                    + describeType(rawExtensions)
            );
            return Map.of();
        }

        LinkedHashMap<String, Map<String, Object>> extensions = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : extensionsValue.entrySet()) {
            if (!(entry.getKey() instanceof String namespace) || namespace.isBlank()) {
                LOG.warning("Ignoring dock session extension with blank namespace");
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> rawPayload)) {
                LOG.warning(
                    "Ignoring dock session extension '" + namespace + "' because the payload is not an object: "
                        + describeType(entry.getValue())
                );
                continue;
            }
            try {
                extensions.put(namespace, copyPayloadMap(rawPayload, ROOT_PATH + "." + EXTENSIONS_KEY + "." + namespace));
            } catch (IllegalArgumentException exception) {
                LOG.log(
                    Level.WARNING,
                    "Ignoring malformed dock session extension '" + namespace + "'",
                    exception
                );
            }
        }
        return extensions;
    }

    private Map<String, Object> copyPayloadMap(Map<?, ?> payload, String path) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : payload.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                throw new IllegalArgumentException(
                    "Invalid " + path + ": extension payload keys must be non-blank strings"
                );
            }
            copy.put(key, entry.getValue());
        }
        return copy;
    }

    private Map<String, Object> optionalMap(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> rawMap) {
            return copyPayloadMap(rawMap, path + "." + key);
        }
        throw invalidShape(path + "." + key, "object", value);
    }

    private List<Object> optionalList(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> rawList) {
            return List.copyOf(rawList);
        }
        throw invalidShape(path + "." + key, "list", value);
    }

    private Map<String, Object> requireMapEntry(List<Object> values, int index, String path) {
        Object value = values.get(index);
        if (value instanceof Map<?, ?> rawMap) {
            return copyPayloadMap(rawMap, path + "[" + index + "]");
        }
        throw invalidShape(path + "[" + index + "]", "object", value);
    }

    private String requiredString(Map<String, Object> map, String key, String path) {
        String value = optionalString(map, key, path);
        if (value == null) {
            throw new IllegalArgumentException("Missing required " + path + "." + key);
        }
        return value;
    }

    private String optionalString(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw invalidShape(path + "." + key, "string", value);
    }

    private int optionalInt(Map<String, Object> map, String key, String path, int defaultValue) {
        Number number = optionalNumber(map, key, path);
        return number != null ? number.intValue() : defaultValue;
    }

    private double optionalDouble(Map<String, Object> map, String key, String path, double defaultValue) {
        Number number = optionalNumber(map, key, path);
        return number != null ? number.doubleValue() : defaultValue;
    }

    private Number optionalNumber(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number;
        }
        throw invalidShape(path + "." + key, "number", value);
    }

    private IllegalArgumentException invalidShape(String path, String expectedType, Object value) {
        return new IllegalArgumentException(
            "Invalid " + path + ": expected " + expectedType + " but was " + describeType(value)
        );
    }

    private String describeType(Object value) {
        if (value == null) {
            return "null";
        }
        return value.getClass().getSimpleName();
    }

    /**
     * Converts a map to a simple JSON string (basic implementation without dependencies).
     *
     * @param map map to convert
     * @return JSON representation
     */
    public String toJson(Map<String, Object> map) {
        return layoutSerializer.toJson(map);
    }

    /**
     * Parses a simple JSON string to a map (basic implementation without dependencies).
     *
     * @param json JSON text to parse
     * @return parsed map
     */
    public Map<String, Object> fromJson(String json) {
        return layoutSerializer.fromJson(json);
    }
}
