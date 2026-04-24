package org.metalib.papifly.fx.docks.ribbon;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.MutableBoolState;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRegistryTest {

    @Test
    void canonicalize_returnsStableCanonicalInstanceForId() {
        CommandRegistry registry = new CommandRegistry();
        PapiflyCommand first = PapiflyCommand.of("save", "Save", () -> {
        });
        PapiflyCommand second = PapiflyCommand.of("save", "Save Again", () -> {
        });

        PapiflyCommand registered = registry.canonicalize(first);
        PapiflyCommand reRegistered = registry.canonicalize(second);

        assertSame(registered, reRegistered);
        assertEquals("Save", registered.label());
        assertNotSame(second, reRegistered);
        assertEquals(1, registry.size());
    }

    @Test
    void canonicalize_projectsIncomingRuntimeStateIntoCanonicalCommand() {
        CommandRegistry registry = new CommandRegistry();
        MutableBoolState canonicalEnabled = new MutableBoolState(false);
        MutableBoolState canonicalSelected = new MutableBoolState(false);
        PapiflyCommand disabled = new PapiflyCommand(
            "refresh",
            "Refresh",
            "Refresh",
            null,
            null,
            canonicalEnabled,
            canonicalSelected,
            () -> {
            }
        );
        PapiflyCommand enabled = new PapiflyCommand(
            "refresh",
            "Refresh",
            "Refresh",
            null,
            null,
            new MutableBoolState(true),
            new MutableBoolState(true),
            () -> {
            }
        );

        PapiflyCommand first = registry.canonicalize(disabled);
        PapiflyCommand second = registry.canonicalize(enabled);

        assertSame(first, second);
        assertTrue(canonicalEnabled.get());
        assertTrue(canonicalSelected.get());
    }

    @Test
    void canonicalize_refreshesActionDispatchFromLatestEmission() {
        CommandRegistry registry = new CommandRegistry();
        AtomicInteger firstExecutions = new AtomicInteger();
        AtomicInteger secondExecutions = new AtomicInteger();
        PapiflyCommand first = PapiflyCommand.of("refresh", "Refresh", firstExecutions::incrementAndGet);
        PapiflyCommand second = PapiflyCommand.of("refresh", "Refresh", secondExecutions::incrementAndGet);

        PapiflyCommand canonical = registry.canonicalize(first);
        canonical.execute();
        registry.canonicalize(second);
        canonical.execute();

        assertEquals(1, firstExecutions.get());
        assertEquals(1, secondExecutions.get());
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
        assertEquals(save.id(), registry.commands().iterator().next().id());
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

        assertEquals(save.id(), registry.unregister("save").orElseThrow().id());
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
