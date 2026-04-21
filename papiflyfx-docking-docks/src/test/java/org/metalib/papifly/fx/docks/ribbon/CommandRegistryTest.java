package org.metalib.papifly.fx.docks.ribbon;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRegistryTest {

    @Test
    void canonicalize_returnsFirstRegisteredInstanceForId() {
        CommandRegistry registry = new CommandRegistry();
        PapiflyCommand first = PapiflyCommand.of("save", "Save", () -> {
        });
        PapiflyCommand second = PapiflyCommand.of("save", "Save Again", () -> {
        });

        PapiflyCommand registered = registry.canonicalize(first);
        PapiflyCommand reRegistered = registry.canonicalize(second);

        assertSame(first, registered);
        assertSame(first, reRegistered);
        assertNotSame(second, reRegistered);
        assertEquals(1, registry.size());
    }

    @Test
    void canonicalize_registersDistinctIdsInInsertionOrder() {
        CommandRegistry registry = new CommandRegistry();
        PapiflyCommand save = PapiflyCommand.of("save", "Save", () -> {
        });
        PapiflyCommand undo = PapiflyCommand.of("undo", "Undo", () -> {
        });
        PapiflyCommand redo = PapiflyCommand.of("redo", "Redo", () -> {
        });

        registry.canonicalize(save);
        registry.canonicalize(undo);
        registry.canonicalize(redo);

        assertEquals(Set.of("save", "undo", "redo"), registry.ids());
        assertEquals(3, registry.size());
        assertEquals(save, registry.commands().iterator().next());
    }

    @Test
    void find_returnsEmptyForUnknownOrBlankIds() {
        CommandRegistry registry = new CommandRegistry();
        assertTrue(registry.find(null).isEmpty());
        assertTrue(registry.find("").isEmpty());
        assertTrue(registry.find("   ").isEmpty());
        assertTrue(registry.find("missing").isEmpty());
    }

    @Test
    void register_reportsPreviousValueWhenIdAlreadyRegistered() {
        CommandRegistry registry = new CommandRegistry();
        PapiflyCommand first = PapiflyCommand.of("copy", "Copy", () -> {
        });
        PapiflyCommand second = PapiflyCommand.of("copy", "Copy (replacement)", () -> {
        });

        assertTrue(registry.register(first).isEmpty());
        assertSame(first, registry.register(second).orElseThrow());
        assertSame(first, registry.find("copy").orElseThrow());
    }

    @Test
    void unregister_removesCommandAndReturnsIt() {
        CommandRegistry registry = new CommandRegistry();
        PapiflyCommand save = PapiflyCommand.of("save", "Save", () -> {
        });
        registry.canonicalize(save);

        assertSame(save, registry.unregister("save").orElseThrow());
        assertTrue(registry.isEmpty());
        assertTrue(registry.unregister("save").isEmpty());
    }

    @Test
    void retain_keepsOnlyIdentifiersInTheKeepSet() {
        CommandRegistry registry = new CommandRegistry();
        registry.canonicalize(PapiflyCommand.of("a", "A", () -> {
        }));
        registry.canonicalize(PapiflyCommand.of("b", "B", () -> {
        }));
        registry.canonicalize(PapiflyCommand.of("c", "C", () -> {
        }));

        registry.retain(Set.of("a", "c"));

        assertEquals(Set.of("a", "c"), registry.ids());
        assertFalse(registry.contains("b"));
    }

    @Test
    void retain_nullKeepSetThrows() {
        CommandRegistry registry = new CommandRegistry();
        assertThrows(NullPointerException.class, () -> registry.retain(null));
    }
}
