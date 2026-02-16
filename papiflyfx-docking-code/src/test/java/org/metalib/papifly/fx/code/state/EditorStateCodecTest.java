package org.metalib.papifly.fx.code.state;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EditorStateCodecTest {

    @Test
    void roundTripPreservesValues() {
        EditorStateData state = new EditorStateData(
            "/tmp/demo.txt",
            12,
            8,
            120.5,
            "java",
            List.of(1, 4, 7)
        );

        Map<String, Object> map = EditorStateCodec.toMap(state);
        EditorStateData restored = EditorStateCodec.fromMap(map);

        assertEquals(state, restored);
    }

    @Test
    void fromMapWithNullReturnsEmpty() {
        EditorStateData result = EditorStateCodec.fromMap(null);
        assertEquals(EditorStateData.empty(), result);
    }

    @Test
    void fromMapWithEmptyMapReturnsEmpty() {
        EditorStateData result = EditorStateCodec.fromMap(Map.of());
        assertEquals(EditorStateData.empty(), result);
    }

    @Test
    void fromMapWithMissingKeysReturnsFallbackValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("filePath", "/some/path.txt");

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals("/some/path.txt", result.filePath());
        assertEquals(0, result.cursorLine());
        assertEquals(0, result.cursorColumn());
        assertEquals(0.0, result.verticalScrollOffset());
        assertEquals("plain-text", result.languageId());
        assertEquals(List.of(), result.foldedLines());
    }

    @Test
    void fromMapWithTypeMismatchedValuesReturnsFallbacks() {
        Map<String, Object> map = new HashMap<>();
        map.put("filePath", 42);
        map.put("cursorLine", "not-a-number");
        map.put("cursorColumn", List.of());
        map.put("verticalScrollOffset", "bad");
        map.put("languageId", 99);
        map.put("foldedLines", "not-a-list");

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals("", result.filePath());
        assertEquals(0, result.cursorLine());
        assertEquals(0, result.cursorColumn());
        assertEquals(0.0, result.verticalScrollOffset());
        assertEquals("plain-text", result.languageId());
        assertEquals(List.of(), result.foldedLines());
    }

    @Test
    void fromMapFoldedLinesDropsNonNumbers() {
        Map<String, Object> map = new HashMap<>();
        map.put("foldedLines", Arrays.asList(1, "bad", 3, null, 5));

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals(List.of(1, 3, 5), result.foldedLines());
    }

    @Test
    void toMapWithNullStateProducesEmptyDefaults() {
        Map<String, Object> map = EditorStateCodec.toMap(null);
        assertNotNull(map);
        EditorStateData roundTrip = EditorStateCodec.fromMap(map);
        assertEquals(EditorStateData.empty(), roundTrip);
    }
}
