package org.metalib.papifly.fx.docks.ribbon;

import org.metalib.papifly.fx.api.ribbon.MutableRibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Ribbon-runtime command registry that stores long-lived {@link RibbonCommand}
 * instances keyed by {@link RibbonCommand#id() command identifier}.
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
 * <p>{@link #canonicalize(RibbonCommand)} is the primary entry point. The
 * first command with a given identifier wins for stable command metadata, and
 * subsequent calls with the same identifier return that canonical instance.
 * Runtime state is refreshed on every call by projecting the incoming
 * {@code enabled} / {@code selected}
 * {@link org.metalib.papifly.fx.api.ribbon.RibbonBooleanState} snapshots onto the
 * canonical command. Action dispatch is refreshed from the latest provider
 * emission through an internal delegating callback so providers can rebuild
 * context-bound callbacks without leaking replacement command instances into
 * rendered UI.</p>
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

    private final Map<String, RibbonCommand> commands = new LinkedHashMap<>();
    private final Map<String, CommandKind> kinds = new LinkedHashMap<>();

    /**
     * Returns the canonical instance for the supplied command.
     *
     * <p>If the registry already holds a command with the same
     * {@link RibbonCommand#id()} identifier, the stored instance is returned
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
    public RibbonCommand canonicalize(RibbonCommand command) {
        return canonicalize(command, CommandKind.ACTION);
    }

    /**
     * Returns the canonical toggle-capable instance for the supplied command.
     *
     * @param command toggle command to canonicalize
     * @return canonical toggle command
     */
    public RibbonToggleCommand canonicalizeToggle(RibbonToggleCommand command) {
        return (RibbonToggleCommand) canonicalize(command, CommandKind.TOGGLE);
    }

    private RibbonCommand canonicalize(RibbonCommand command, CommandKind kind) {
        Objects.requireNonNull(command, "command");
        RibbonCommand existing = commands.get(command.id());
        if (existing != null) {
            CommandKind existingKind = kinds.get(command.id());
            if (existingKind != kind) {
                throw new IllegalArgumentException("Ribbon command id '" + command.id()
                    + "' was contributed as both " + existingKind.label + " and " + kind.label);
            }
            projectRuntimeSurface(existing, command);
            return existing;
        }
        RibbonCommand canonical = createCanonicalCommand(command, kind);
        commands.put(canonical.id(), canonical);
        kinds.put(canonical.id(), kind);
        return canonical;
    }

    private static RibbonCommand createCanonicalCommand(RibbonCommand command, CommandKind kind) {
        RefreshableAction action = command.action() instanceof RefreshableAction refreshable
            ? refreshable
            : new RefreshableAction(command.action());
        if (kind == CommandKind.TOGGLE) {
            RibbonToggleCommand toggle = (RibbonToggleCommand) command;
            return new CanonicalRibbonToggleCommand(
                command.id(),
                command.label(),
                command.tooltip(),
                command.smallIcon(),
                command.largeIcon(),
                mutableCopy(command.enabled()),
                mutableCopy(toggle.selected()),
                action
            );
        }
        return new CanonicalRibbonCommand(
            command.id(),
            command.label(),
            command.tooltip(),
            command.smallIcon(),
            command.largeIcon(),
            mutableCopy(command.enabled()),
            action
        );
    }

    private static MutableRibbonBooleanState mutableCopy(RibbonBooleanState state) {
        if (state instanceof MutableRibbonBooleanState mutable) {
            return mutable;
        }
        return RibbonBooleanState.mutable(state != null && state.get());
    }

    private static void projectRuntimeSurface(RibbonCommand canonical, RibbonCommand incoming) {
        if (canonical.enabled() instanceof MutableRibbonBooleanState enabled) {
            enabled.set(incoming.enabled().get());
        }
        if (canonical instanceof RibbonToggleCommand canonicalToggle
            && incoming instanceof RibbonToggleCommand incomingToggle
            && canonicalToggle.selected() instanceof MutableRibbonBooleanState selected) {
            selected.set(incomingToggle.selected().get());
        }
        if (canonical.action() instanceof RefreshableAction refreshable
            && canonical.action() != incoming.action()) {
            refreshable.replace(incoming.action());
        }
    }

    /**
     * Looks up a command by identifier.
     *
     * @param id command identifier
     * @return command when registered under the identifier
     */
    public Optional<RibbonCommand> find(String id) {
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
    public Collection<RibbonCommand> commands() {
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
     * <p>Unlike {@link #canonicalize(RibbonCommand)} this method explicitly
     * surfaces whether the registration was a no-op.</p>
     *
     * @param command command to register
     * @return previously stored command when the identifier was already registered,
     *     otherwise {@link Optional#empty()}
     * @throws NullPointerException if {@code command} is {@code null}
     */
    public Optional<RibbonCommand> register(RibbonCommand command) {
        Objects.requireNonNull(command, "command");
        Optional<RibbonCommand> previous = Optional.ofNullable(commands.putIfAbsent(command.id(), command));
        if (previous.isEmpty()) {
            kinds.put(command.id(), command instanceof RibbonToggleCommand ? CommandKind.TOGGLE : CommandKind.ACTION);
        }
        return previous;
    }

    /**
     * Removes the command registered under the supplied identifier.
     *
     * @param id command identifier
     * @return removed command when present
     */
    public Optional<RibbonCommand> unregister(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        kinds.remove(id);
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
        kinds.keySet().removeIf(id -> !commands.containsKey(id));
    }

    /**
     * Removes every registered command.
     */
    public void clear() {
        commands.clear();
        kinds.clear();
    }

    private enum CommandKind {
        ACTION("action-only"),
        TOGGLE("toggle-capable");

        private final String label;

        CommandKind(String label) {
            this.label = label;
        }
    }

    private record CanonicalRibbonCommand(
        String id,
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        RibbonBooleanState enabled,
        Runnable action
    ) implements RibbonCommand {
    }

    private record CanonicalRibbonToggleCommand(
        String id,
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        RibbonBooleanState enabled,
        MutableRibbonBooleanState selected,
        Runnable action
    ) implements RibbonToggleCommand {
    }

    private static final class RefreshableAction implements Runnable {
        private Runnable delegate;

        private RefreshableAction(Runnable delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void run() {
            delegate.run();
        }

        private void replace(Runnable delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }
    }
}
