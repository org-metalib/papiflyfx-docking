package org.metalib.papifly.fx.docks.ribbon;

import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Ribbon-runtime command registry that stores long-lived {@link PapiflyCommand}
 * instances keyed by {@link PapiflyCommand#id() command identifier}.
 *
 * <p>Providers typically rebuild contributed specs on every
 * {@code getTabs(context)} invocation. Without a canonicalizing registry, each
 * rebuild produces fresh command instances and invalidates any UI bindings or
 * QAT selections that held the previous instances. The registry solves this by
 * guaranteeing that for a given command identifier the runtime keeps a single
 * canonical instance for as long as that identifier is reachable — either
 * because it appears in the currently materialized tab model or because it is
 * referenced by the Quick Access Toolbar.</p>
 *
 * <h2>Identity semantics</h2>
 * <p>{@link #canonicalize(PapiflyCommand)} is the primary entry point. The
 * first command with a given identifier wins for stable command metadata and
 * action dispatch: subsequent calls with the same identifier return the
 * original instance. Runtime state is refreshed on every call by projecting the
 * incoming {@code enabled} / {@code selected}
 * {@link org.metalib.papifly.fx.api.ribbon.BoolState} snapshots onto the
 * canonical command. This lets providers rebuild specs from the current
 * context without leaking replacement command instances into rendered UI.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>The registry is intentionally not thread-safe. It is owned by
 * {@link RibbonManager} and is expected to be accessed from the JavaFX
 * application thread (or whichever thread drives the hosting
 * {@link RibbonManager#refresh() refresh} cycle). Hosts and providers should
 * treat it as runtime-owned infrastructure rather than a shared mutable store
 * for ad hoc background updates.</p>
 *
 * <h2>Refresh invariants</h2>
 * <p>Ribbon 2 steady-state refresh performance relies on canonical command
 * identity plus node-cache reuse in the shell layer. When a refresh emits the
 * same command ids, the runtime reuses the existing command surfaces and
 * telemetry reports cache hits instead of rebuilds. Replacing command ids for
 * the same semantic action breaks QAT persistence and defeats those
 * invariants.</p>
 *
 * @since Ribbon 2
 */
public final class CommandRegistry {

    private final Map<String, PapiflyCommand> commands = new LinkedHashMap<>();

    /**
     * Returns the canonical instance for the supplied command.
     *
     * <p>If the registry already holds a command with the same
     * {@link PapiflyCommand#id()} identifier, the stored instance is returned
     * after its runtime state has been updated from the incoming command.
     * Otherwise the supplied command is stored and returned as the new
     * canonical instance.</p>
     *
     * <p>Callers are expected to invoke this from the same FX-thread /
     * refresh-driving thread that owns the surrounding {@link RibbonManager}.</p>
     *
     * @param command command to canonicalize
     * @return canonical command instance for the supplied identifier
     * @throws NullPointerException if {@code command} is {@code null}
     */
    public PapiflyCommand canonicalize(PapiflyCommand command) {
        Objects.requireNonNull(command, "command");
        PapiflyCommand existing = commands.get(command.id());
        if (existing != null) {
            projectRuntimeState(existing, command);
            return existing;
        }
        commands.put(command.id(), command);
        return command;
    }

    private static void projectRuntimeState(PapiflyCommand canonical, PapiflyCommand incoming) {
        canonical.enabled().set(incoming.enabled().get());
        canonical.selected().set(incoming.selected().get());
    }

    /**
     * Looks up a command by identifier.
     *
     * @param id command identifier
     * @return command when registered under the identifier
     */
    public Optional<PapiflyCommand> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(commands.get(id));
    }

    /**
     * Returns whether a command is registered for the supplied identifier.
     *
     * @param id command identifier
     * @return {@code true} when the identifier is registered
     */
    public boolean contains(String id) {
        return id != null && !id.isBlank() && commands.containsKey(id);
    }

    /**
     * Returns the registered command identifiers in insertion order.
     *
     * @return unmodifiable view of registered identifiers
     */
    public Set<String> ids() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    /**
     * Returns the registered commands in insertion order.
     *
     * @return unmodifiable view of registered commands
     */
    public Collection<PapiflyCommand> commands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    /**
     * Returns the number of registered commands.
     *
     * @return registry size
     */
    public int size() {
        return commands.size();
    }

    /**
     * Returns whether the registry is empty.
     *
     * @return {@code true} when no commands are registered
     */
    public boolean isEmpty() {
        return commands.isEmpty();
    }

    /**
     * Registers the supplied command if its identifier is not already present.
     *
     * <p>Unlike {@link #canonicalize(PapiflyCommand)} this method explicitly
     * surfaces whether the registration was a no-op.</p>
     *
     * @param command command to register
     * @return previously stored command when the identifier was already registered,
     *     otherwise {@link Optional#empty()}
     * @throws NullPointerException if {@code command} is {@code null}
     */
    public Optional<PapiflyCommand> register(PapiflyCommand command) {
        Objects.requireNonNull(command, "command");
        return Optional.ofNullable(commands.putIfAbsent(command.id(), command));
    }

    /**
     * Removes the command registered under the supplied identifier.
     *
     * @param id command identifier
     * @return removed command when present
     */
    public Optional<PapiflyCommand> unregister(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(commands.remove(id));
    }

    /**
     * Retains the commands whose identifiers are present in the supplied set
     * and removes the rest.
     *
     * <p>Used by {@link RibbonManager} during refresh cycles to evict commands
     * that are no longer reachable through visible tabs or through Quick Access
     * Toolbar references.</p>
     *
     * <p>This pruning behavior is part of the Ribbon 2 cache/telemetry model:
     * steady-state refreshes should normally keep the same identifiers alive,
     * allowing the shell to reuse command-backed controls instead of rebuilding
     * them wholesale.</p>
     *
     * @param idsToKeep identifiers to retain
     * @throws NullPointerException if {@code idsToKeep} is {@code null}
     */
    public void retain(Set<String> idsToKeep) {
        Objects.requireNonNull(idsToKeep, "idsToKeep");
        commands.keySet().removeIf(id -> !idsToKeep.contains(id));
    }

    /**
     * Removes every registered command.
     */
    public void clear() {
        commands.clear();
    }
}
