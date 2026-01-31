package org.metalib.papifly.fx.docks.serial;

import javafx.geometry.Orientation;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.SplitData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes and deserializes layout definitions to/from Map structures.
 * The Map structure can be easily converted to JSON using any JSON library.
 */
public class LayoutSerializer {

    private static final String TYPE_KEY = "type";
    private static final String ID_KEY = "id";
    private static final String TITLE_KEY = "title";
    private static final String CONTENT_FACTORY_KEY = "contentFactoryId";
    private static final String CONTENT_KEY = "content";
    private static final String CONTENT_TYPE_KEY = "typeKey";
    private static final String CONTENT_ID_KEY = "contentId";
    private static final String CONTENT_VERSION_KEY = "version";
    private static final String CONTENT_STATE_KEY = "state";
    private static final String ORIENTATION_KEY = "orientation";
    private static final String DIVIDER_KEY = "dividerPosition";
    private static final String FIRST_KEY = "first";
    private static final String SECOND_KEY = "second";
    private static final String TABS_KEY = "tabs";
    private static final String ACTIVE_TAB_KEY = "activeTabIndex";

    private static final String TYPE_LEAF = "leaf";
    private static final String TYPE_SPLIT = "split";
    private static final String TYPE_TAB_GROUP = "tabGroup";

    /**
     * Serializes a LayoutNode to a Map.
     */
    public Map<String, Object> serialize(LayoutNode node) {
        if (node == null) {
            return null;
        }

        return switch (node) {
            case LeafData leaf -> serializeLeaf(leaf);
            case SplitData split -> serializeSplit(split);
            case TabGroupData tabGroup -> serializeTabGroup(tabGroup);
        };
    }

    private Map<String, Object> serializeLeaf(LeafData leaf) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(TYPE_KEY, TYPE_LEAF);
        map.put(ID_KEY, leaf.id());
        map.put(TITLE_KEY, leaf.title());
        if (leaf.contentFactoryId() != null) {
            map.put(CONTENT_FACTORY_KEY, leaf.contentFactoryId());
        }
        if (leaf.content() != null) {
            map.put(CONTENT_KEY, serializeContent(leaf.content()));
        }
        return map;
    }

    private Map<String, Object> serializeContent(LeafContentData content) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (content.typeKey() != null) {
            map.put(CONTENT_TYPE_KEY, content.typeKey());
        }
        if (content.contentId() != null) {
            map.put(CONTENT_ID_KEY, content.contentId());
        }
        map.put(CONTENT_VERSION_KEY, content.version());
        if (content.state() != null) {
            map.put(CONTENT_STATE_KEY, content.state());
        }
        return map;
    }

    private Map<String, Object> serializeSplit(SplitData split) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(TYPE_KEY, TYPE_SPLIT);
        map.put(ID_KEY, split.id());
        map.put(ORIENTATION_KEY, split.orientation().name());
        map.put(DIVIDER_KEY, split.dividerPosition());
        map.put(FIRST_KEY, serialize(split.first()));
        map.put(SECOND_KEY, serialize(split.second()));
        return map;
    }

    private Map<String, Object> serializeTabGroup(TabGroupData tabGroup) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(TYPE_KEY, TYPE_TAB_GROUP);
        map.put(ID_KEY, tabGroup.id());

        List<Map<String, Object>> tabs = new ArrayList<>();
        for (LeafData tab : tabGroup.tabs()) {
            tabs.add(serializeLeaf(tab));
        }
        map.put(TABS_KEY, tabs);
        map.put(ACTIVE_TAB_KEY, tabGroup.activeTabIndex());

        return map;
    }

    /**
     * Deserializes a Map to a LayoutNode.
     */
    @SuppressWarnings("unchecked")
    public LayoutNode deserialize(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        String type = (String) map.get(TYPE_KEY);

        if (type == null) {
            throw new IllegalArgumentException("Missing type");
        }

        return switch (type) {
            case TYPE_LEAF -> deserializeLeaf(map);
            case TYPE_SPLIT -> deserializeSplit(map);
            case TYPE_TAB_GROUP -> deserializeTabGroup(map);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    private LeafData deserializeLeaf(Map<String, Object> map) {
        String id = (String) map.get(ID_KEY);
        String title = (String) map.get(TITLE_KEY);
        String contentFactoryId = (String) map.get(CONTENT_FACTORY_KEY);
        LeafContentData content = null;
        if (map.containsKey(CONTENT_KEY)) {
            content = deserializeContent(map.get(CONTENT_KEY));
        }
        return new LeafData(id, title, contentFactoryId, content);
    }

    @SuppressWarnings("unchecked")
    private LeafContentData deserializeContent(Object value) {
        if (!(value instanceof Map<?, ?> contentMap)) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) contentMap;
        String typeKey = (String) map.get(CONTENT_TYPE_KEY);
        String contentId = (String) map.get(CONTENT_ID_KEY);
        int version = ((Number) map.getOrDefault(CONTENT_VERSION_KEY, 1)).intValue();

        Map<String, Object> state = null;
        Object stateValue = map.get(CONTENT_STATE_KEY);
        if (stateValue instanceof Map<?, ?> stateMap) {
            state = (Map<String, Object>) stateMap;
        }

        return new LeafContentData(typeKey, contentId, version, state);
    }

    @SuppressWarnings("unchecked")
    private SplitData deserializeSplit(Map<String, Object> map) {
        String id = (String) map.get(ID_KEY);
        Orientation orientation = Orientation.valueOf((String) map.get(ORIENTATION_KEY));
        double dividerPosition = ((Number) map.get(DIVIDER_KEY)).doubleValue();
        LayoutNode first = deserialize((Map<String, Object>) map.get(FIRST_KEY));
        LayoutNode second = deserialize((Map<String, Object>) map.get(SECOND_KEY));
        return new SplitData(id, orientation, dividerPosition, first, second);
    }

    @SuppressWarnings("unchecked")
    private TabGroupData deserializeTabGroup(Map<String, Object> map) {
        String id = (String) map.get(ID_KEY);
        List<Map<String, Object>> tabMaps = (List<Map<String, Object>>) map.get(TABS_KEY);
        int activeTabIndex = ((Number) map.get(ACTIVE_TAB_KEY)).intValue();

        List<LeafData> tabs = new ArrayList<>();
        for (Map<String, Object> tabMap : tabMaps) {
            tabs.add(deserializeLeaf(tabMap));
        }

        return new TabGroupData(id, tabs, activeTabIndex);
    }

    /**
     * Converts a map to a simple JSON string (basic implementation without dependencies).
     */
    public String toJson(Map<String, Object> map) {
        return toJsonValue(map, 0);
    }

    /**
     * Parses a simple JSON string to a map (basic implementation without dependencies).
     */
    public Map<String, Object> fromJson(String json) {
        return new SimpleJsonParser(json).parseObject();
    }

    @SuppressWarnings("unchecked")
    private String toJsonValue(Object value, int indent) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "\"" + escapeJson(s) + "\"";
        }
        if (value instanceof Number n) {
            return n.toString();
        }
        if (value instanceof Boolean b) {
            return b.toString();
        }
        if (value instanceof Map<?, ?> m) {
            return toJsonObject((Map<String, Object>) m, indent);
        }
        if (value instanceof List<?> l) {
            return toJsonArray((List<Object>) l, indent);
        }
        return "\"" + value.toString() + "\"";
    }

    private String toJsonObject(Map<String, Object> map, int indent) {
        if (map.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        String indentStr = "  ".repeat(indent + 1);
        String closingIndent = "  ".repeat(indent);

        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            sb.append(indentStr)
              .append("\"").append(escapeJson(entry.getKey())).append("\": ")
              .append(toJsonValue(entry.getValue(), indent + 1));
            if (it.hasNext()) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(closingIndent).append("}");
        return sb.toString();
    }

    private String toJsonArray(List<Object> list, int indent) {
        if (list.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        String indentStr = "  ".repeat(indent + 1);
        String closingIndent = "  ".repeat(indent);

        Iterator<Object> it = list.iterator();
        while (it.hasNext()) {
            sb.append(indentStr).append(toJsonValue(it.next(), indent + 1));
            if (it.hasNext()) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(closingIndent).append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Simple JSON parser for basic layout deserialization.
     */
    private static class SimpleJsonParser {
        private final String json;
        private int pos;

        SimpleJsonParser(String json) {
            this.json = json.trim();
            this.pos = 0;
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();

            if (peek() != '}') {
                do {
                    skipWhitespace();
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    skipWhitespace();
                    Object value = parseValue();
                    map.put(key, value);
                    skipWhitespace();
                } while (consumeIf(','));
            }

            expect('}');
            return map;
        }

        private Object parseValue() {
            skipWhitespace();
            char c = peek();

            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (c == 't' || c == 'f') {
                return parseBoolean();
            }
            if (c == 'n') {
                return parseNull();
            }
            return parseNumber();
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();

            if (peek() != ']') {
                do {
                    skipWhitespace();
                    list.add(parseValue());
                    skipWhitespace();
                } while (consumeIf(','));
            }

            expect(']');
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();

            while (pos < json.length()) {
                char c = json.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    c = json.charAt(pos++);
                    switch (c) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default -> sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            }

            throw new IllegalStateException("Unterminated string");
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < json.length() && isNumberChar(json.charAt(pos))) {
                pos++;
            }
            String numStr = json.substring(start, pos);
            if (numStr.contains(".")) {
                return Double.parseDouble(numStr);
            }
            return Long.parseLong(numStr);
        }

        private boolean parseBoolean() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return true;
            }
            if (json.startsWith("false", pos)) {
                pos += 5;
                return false;
            }
            throw new IllegalStateException("Expected boolean at position " + pos);
        }

        private Object parseNull() {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalStateException("Expected null at position " + pos);
        }

        private boolean isNumberChar(char c) {
            return c >= '0' && c <= '9' || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E';
        }

        private void expect(char c) {
            skipWhitespace();
            if (pos >= json.length() || json.charAt(pos) != c) {
                throw new IllegalStateException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        private boolean consumeIf(char c) {
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        private char peek() {
            skipWhitespace();
            return pos < json.length() ? json.charAt(pos) : '\0';
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
}
