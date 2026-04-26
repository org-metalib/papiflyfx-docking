package org.metalib.papifly.fx.docks.ribbon;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.MutableRibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand;

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
        RibbonCommand first = RibbonCommand.of("save", "Save", () -> {
        });
        RibbonCommand second = RibbonCommand.of("save", "Save Again", () -> {
        });

        RibbonCommand registered = registry.canonicalize(first);
        RibbonCommand reRegistered = registry.canonicalize(second);

        assertSame(registered, reRegistered);
        assertEquals("Save", registered.label());
        assertNotSame(second, reRegistered);
        assertEquals(1, registry.size());
    }

    @Test
    void canonicalize_projectsIncomingRuntimeStateIntoCanonicalCommand() {
        CommandRegistry registry = new CommandRegistry();
        MutableRibbonBooleanState canonicalEnabled = RibbonBooleanState.mutable(false);
        MutableRibbonBooleanState canonicalSelected = RibbonBooleanState.mutable(false);
        RibbonToggleCommand disabled = RibbonToggleCommand.of(
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
        RibbonToggleCommand enabled = RibbonToggleCommand.of(
            "refresh",
            "Refresh",
            "Refresh",
            null,
            null,
            RibbonBooleanState.mutable(true),
            RibbonBooleanState.mutable(true),
            () -> {
            }
        );

        RibbonToggleCommand first = registry.canonicalizeToggle(disabled);
        RibbonToggleCommand second = registry.canonicalizeToggle(enabled);

        assertSame(first, second);
        assertTrue(canonicalEnabled.get());
        assertTrue(canonicalSelected.get());
    }

    @Test
    void canonicalize_refreshesActionDispatchFromLatestEmission() {
        CommandRegistry registry = new CommandRegistry();
        AtomicInteger firstExecutions = new AtomicInteger();
        AtomicInteger secondExecutions = new AtomicInteger();
        RibbonCommand first = RibbonCommand.of("refresh", "Refresh", firstExecutions::incrementAndGet);
        RibbonCommand second = RibbonCommand.of("refresh", "Refresh", secondExecutions::incrementAndGet);

        RibbonCommand canonical = registry.canonicalize(first);
        canonical.execute();
        registry.canonicalize(second);
        canonical.execute();

        assertEquals(1, firstExecutions.get());
        assertEquals(1, secondExecutions.get());
    }

    @Test
    void canonicalize_registersDistinctIdsInInsertionOrder() {
        CommandRegistry registry = new CommandRegistry();
        RibbonCommand save = RibbonCommand.of("save", "Save", () -> {
        });
        RibbonCommand undo = RibbonCommand.of("undo", "Undo", () -> {
        });
        RibbonCommand redo = RibbonCommand.of("redo", "Redo", () -> {
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
        RibbonCommand first = RibbonCommand.of("copy", "Copy", () -> {
        });
        RibbonCommand second = RibbonCommand.of("copy", "Copy (replacement)", () -> {
        });

        assertTrue(registry.register(first).isEmpty());
        assertSame(first, registry.register(second).orElseThrow());
        assertSame(first, registry.find("copy").orElseThrow());
    }

    @Test
    void unregister_removesCommandAndReturnsIt() {
        CommandRegistry registry = new CommandRegistry();
        RibbonCommand save = RibbonCommand.of("save", "Save", () -> {
        });
        registry.canonicalize(save);

        assertEquals(save.id(), registry.unregister("save").orElseThrow().id());
        assertTrue(registry.isEmpty());
        assertTrue(registry.unregister("save").isEmpty());
    }

    @Test
    void retain_keepsOnlyIdentifiersInTheKeepSet() {
        CommandRegistry registry = new CommandRegistry();
        registry.canonicalize(RibbonCommand.of("a", "A", () -> {
        }));
        registry.canonicalize(RibbonCommand.of("b", "B", () -> {
        }));
        registry.canonicalize(RibbonCommand.of("c", "C", () -> {
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
