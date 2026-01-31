package org.metalib.papifly.fx.docks.serial;

import javafx.geometry.Orientation;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.docks.layout.data.BoundsData;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.FloatingLeafData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafContentData;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.MaximizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.MinimizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.RestoreHintData;
import org.metalib.papifly.fx.docks.layout.data.SplitData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockSessionSerializerTest {

    private final DockSessionSerializer serializer = new DockSessionSerializer();

    @Test
    void serializeDeserialize_roundTrip() {
        DockSessionData session = buildSession("Editor 2");

        Map<String, Object> map = serializer.serialize(session);
        DockSessionData restored = serializer.deserialize(map);

        assertEquals(session, restored);
    }

    @Test
    void toJsonFromJson_roundTrip_preservesEscaping() {
        DockSessionData session = buildSession("tab \"one\" \\\\ two\nline");

        Map<String, Object> map = serializer.serialize(session);
        String json = serializer.toJson(map);
        Map<String, Object> restoredMap = serializer.fromJson(json);
        DockSessionData restored = serializer.deserialize(restoredMap);

        assertEquals(session, restored);
    }

    @Test
    void serializeDeserialize_roundTrip_withContentState() {
        LeafContentData contentData = LeafContentData.of(
            "chart",
            "chart-1",
            2,
            Map.of("series", List.of("A", "B"), "zoom", 1)
        );

        LayoutNode layout = new TabGroupData(
            "tabs-1",
            List.of(LeafData.of("leaf-1", "Chart", "chart", contentData)),
            0
        );

        DockSessionData session = DockSessionData.of(layout, List.of(), List.of(), null);

        Map<String, Object> map = serializer.serialize(session);
        DockSessionData restored = serializer.deserialize(map);

        assertEquals(session, restored);
    }

    @Test
    void deserialize_invalidType_throws() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "nope");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(map));
        assertTrue(ex.getMessage().contains("Invalid session type"));
    }

    private DockSessionData buildSession(String floatingTitle) {
        LayoutNode layout = new SplitData(
            "split-1",
            Orientation.HORIZONTAL,
            0.6,
            new TabGroupData(
                "tabs-1",
                List.of(
                    LeafData.of("leaf-1", "Editor 1"),
                    LeafData.of("leaf-2", floatingTitle)
                ),
                1
            ),
            LeafData.of("leaf-3", "Console")
        );

        FloatingLeafData floating = new FloatingLeafData(
            LeafData.of("leaf-4", floatingTitle),
            new BoundsData(120, 80, 480, 320),
            new RestoreHintData("tabs-1", "WEST", 1, 0.5, "leaf-1")
        );

        MinimizedLeafData minimized = new MinimizedLeafData(
            LeafData.of("leaf-5", "Minimized"),
            new RestoreHintData("tabs-1", "SOUTH", 0, 0.4, "leaf-2")
        );

        MaximizedLeafData maximized = new MaximizedLeafData(
            LeafData.of("leaf-6", "Maximized"),
            new RestoreHintData("tabs-1", "CENTER", 0, 0.5, null)
        );

        return DockSessionData.of(layout, List.of(floating), List.of(minimized), maximized);
    }
}
