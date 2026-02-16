package org.metalib.papifly.fx.code.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderLineTest {

    @Test
    void recordFieldsAreAccessible() {
        RenderLine rl = new RenderLine(5, "hello world", 100.0);
        assertEquals(5, rl.lineIndex());
        assertEquals("hello world", rl.text());
        assertEquals(100.0, rl.y());
    }

    @Test
    void emptyLineText() {
        RenderLine rl = new RenderLine(0, "", 0.0);
        assertEquals(0, rl.lineIndex());
        assertEquals("", rl.text());
        assertEquals(0.0, rl.y());
    }

    @Test
    void equalityAndHashCode() {
        RenderLine a = new RenderLine(1, "abc", 20.0);
        RenderLine b = new RenderLine(1, "abc", 20.0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
