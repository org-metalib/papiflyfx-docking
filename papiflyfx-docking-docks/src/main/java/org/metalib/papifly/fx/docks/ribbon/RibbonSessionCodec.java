package org.metalib.papifly.fx.docks.ribbon;

import org.metalib.papifly.fx.docks.DockSessionExtensionCodec;
import org.metalib.papifly.fx.docks.layout.data.RibbonSessionData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RibbonSessionCodec implements DockSessionExtensionCodec<RibbonSessionData> {

    private static final String MINIMIZED_KEY = "minimized";
    private static final String SELECTED_TAB_ID_KEY = "selectedTabId";
    private static final String QUICK_ACCESS_COMMAND_IDS_KEY = "quickAccessCommandIds";
    private static final String EXTENSION_PATH = "extensions." + RibbonSessionStateContributor.EXTENSION_NAMESPACE;

    @Override
    public Map<String, Object> encode(RibbonSessionData state) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put(MINIMIZED_KEY, state.minimized());
        if (state.selectedTabId() != null) {
            payload.put(SELECTED_TAB_ID_KEY, state.selectedTabId());
        }
        if (!state.quickAccessCommandIds().isEmpty()) {
            payload.put(QUICK_ACCESS_COMMAND_IDS_KEY, state.quickAccessCommandIds());
        }
        return payload;
    }

    @Override
    public RibbonSessionData decode(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        boolean minimized = optionalBoolean(payload, MINIMIZED_KEY, false);
        String selectedTabId = optionalString(payload, SELECTED_TAB_ID_KEY);
        List<String> quickAccessCommandIds = optionalStringList(payload, QUICK_ACCESS_COMMAND_IDS_KEY);
        return new RibbonSessionData(minimized, selectedTabId, quickAccessCommandIds);
    }

    private boolean optionalBoolean(Map<String, Object> payload, String key, boolean defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw invalidField(key, "boolean", value);
    }

    private String optionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw invalidField(key, "string", value);
    }

    private List<String> optionalStringList(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawValues)) {
            throw invalidField(key, "list", value);
        }
        List<String> values = new ArrayList<>(rawValues.size());
        for (int index = 0; index < rawValues.size(); index++) {
            Object item = rawValues.get(index);
            if (!(item instanceof String stringValue)) {
                throw new IllegalArgumentException(
                    "Invalid " + EXTENSION_PATH + "." + key + "[" + index + "]: expected string but was "
                        + describeType(item)
                );
            }
            values.add(stringValue);
        }
        return values;
    }

    private IllegalArgumentException invalidField(String key, String expectedType, Object value) {
        return new IllegalArgumentException(
            "Invalid " + EXTENSION_PATH + "." + key + ": expected " + expectedType + " but was " + describeType(value)
        );
    }

    private String describeType(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
