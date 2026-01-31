package org.metalib.papifly.fx.docks.serial;

import javafx.geometry.Orientation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docks.layout.data.BoundsData;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.FloatingLeafData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.MaximizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.MinimizedLeafData;
import org.metalib.papifly.fx.docks.layout.data.RestoreHintData;
import org.metalib.papifly.fx.docks.layout.data.SplitData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockSessionPersistenceTest {

    private final DockSessionPersistence persistence = new DockSessionPersistence();

    @Test
    void testToJsonString_withSession() {
        DockSessionData session = buildSession();

        String json = persistence.toJsonString(session);

        assertNotNull(json);
        assertTrue(json.contains("\"type\": \"dockSession\""));
        assertTrue(json.contains("\"layout\""));
    }

    @Test
    void testToJsonString_withNull() {
        String json = persistence.toJsonString(null);
        assertNull(json);
    }

    @Test
    void testFromJsonString_withNull() {
        DockSessionData session = persistence.fromJsonString(null);
        assertNull(session);
    }

    @Test
    void testFromJsonString_withEmpty() {
        DockSessionData session = persistence.fromJsonString("");
        assertNull(session);
    }

    @Test
    void testRoundTrip_session() {
        DockSessionData original = buildSession();

        String json = persistence.toJsonString(original);
        DockSessionData restored = persistence.fromJsonString(json);

        assertEquals(original, restored);
    }

    @Test
    void testToJsonFile_createsParentDirs(@TempDir Path tempDir) {
        Path file = tempDir.resolve("nested").resolve("session.json");
        DockSessionData session = buildSession();

        persistence.toJsonFile(session, file);

        assertTrue(file.toFile().exists());
        assertTrue(file.toFile().length() > 0);
    }

    @Test
    void testFromJsonFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("session.json");
        DockSessionData original = buildSession();

        persistence.toJsonFile(original, file);
        DockSessionData restored = persistence.fromJsonFile(file);

        assertEquals(original, restored);
    }

    @Test
    void testFromJsonFile_nonExistent(@TempDir Path tempDir) {
        Path file = tempDir.resolve("nonexistent.json");

        assertThrows(DockSessionPersistence.SessionFileIOException.class, () -> persistence.fromJsonFile(file));
    }

    private DockSessionData buildSession() {
        LayoutNode layout = new SplitData(
            "split-1",
            Orientation.VERTICAL,
            0.7,
            new TabGroupData(
                "tabs-1",
                List.of(
                    LeafData.of("leaf-1", "Editor 1"),
                    LeafData.of("leaf-2", "Editor 2")
                ),
                0
            ),
            LeafData.of("leaf-3", "Console")
        );

        FloatingLeafData floating = new FloatingLeafData(
            LeafData.of("leaf-4", "Floating"),
            new BoundsData(40, 60, 320, 240),
            new RestoreHintData("tabs-1", "EAST", 1, 0.6, "leaf-2")
        );

        MinimizedLeafData minimized = new MinimizedLeafData(
            LeafData.of("leaf-5", "Minimized"),
            new RestoreHintData("tabs-1", "SOUTH", 0, 0.5, null)
        );

        MaximizedLeafData maximized = new MaximizedLeafData(
            LeafData.of("leaf-6", "Maximized"),
            new RestoreHintData("tabs-1", "CENTER", 0, 0.5, null)
        );

        return DockSessionData.of(layout, List.of(floating), List.of(minimized), maximized);
    }
}
