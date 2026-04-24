package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers ribbon providers and materializes the visible tab model for a
 * runtime {@link RibbonContext}.
 *
 * <p><b>Ribbon 2 command architecture:</b> the manager owns a long-lived
 * {@link CommandRegistry} that canonicalizes every provider-contributed
 * command by identifier. Provider specs are rebuilt on each
 * {@link #refresh() refresh} cycle, but the command instances themselves are
 * reused, so UI bindings, QAT selections, and shortcut wiring do not churn
 * when contextual tabs recompute.</p>
 *
 * <p>The Quick Access Toolbar state is stored as an identifier list
 * ({@link #getQuickAccessCommandIds()}). The {@link #getQuickAccessCommands()
 * command view} is derived from that list and the registry and is only
 * populated for identifiers that currently resolve to a registered command.
 * This keeps QAT restore robust across context changes: hidden contextual
 * commands disappear from the view but their identifiers remain in the list
 * and reappear as soon as the owning tab becomes visible again.</p>
 *
 * @since Ribbon 2
 */
public class RibbonManager {

    private static final Logger LOG = Logger.getLogger(RibbonManager.class.getName());
    private static final Comparator<RibbonProvider> PROVIDER_COMPARATOR =
        Comparator.comparingInt(RibbonProvider::order)
            .thenComparing(RibbonProvider::id);
    private static final Comparator<RibbonTabSpec> TAB_COMPARATOR =
        Comparator.comparingInt(RibbonTabSpec::order)
            .thenComparing(RibbonTabSpec::id);
    private static final Comparator<RibbonGroupSpec> GROUP_COMPARATOR =
        Comparator.comparingInt(RibbonGroupSpec::order)
            .thenComparingInt(RibbonGroupSpec::collapseOrder)
            .thenComparing(RibbonGroupSpec::id);

    private final ObservableList<RibbonProvider> providers = FXCollections.observableArrayList();
    private final ObservableList<RibbonTabSpec> tabs = FXCollections.observableArrayList();
    private final ObservableList<RibbonTabSpec> tabsView = FXCollections.unmodifiableObservableList(tabs);

    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final ObservableList<String> quickAccessCommandIds = FXCollections.observableArrayList();
    private final ObservableList<PapiflyCommand> quickAccessCommands = FXCollections.observableArrayList();
    private final ObservableList<PapiflyCommand> quickAccessCommandsView =
        FXCollections.unmodifiableObservableList(quickAccessCommands);
    private final Set<String> reportedTabIdCollisions = new LinkedHashSet<>();
    private final Set<String> reportedCommandIdCollisions = new LinkedHashSet<>();

    private final ObjectProperty<RibbonContext> context = new SimpleObjectProperty<>(RibbonContext.empty());
    private final ClassLoader classLoader;
    private RibbonLayoutTelemetry telemetry = RibbonLayoutTelemetry.noop();

    /**
     * Creates a manager that discovers providers from the current context class
     * loader.
     */
    public RibbonManager() {
        this(resolveClassLoader());
    }

    /**
     * Creates a manager that discovers providers using the supplied loader.
     *
     * @param classLoader class loader used for ServiceLoader discovery and icon resolution
     */
    public RibbonManager(ClassLoader classLoader) {
        this(classLoader, discoverProviders(classLoader));
    }

    /**
     * Creates a manager with an explicit provider set.
     *
     * @param providers providers to register
     */
    public RibbonManager(Collection<? extends RibbonProvider> providers) {
        this(resolveClassLoader(), providers);
    }

    /**
     * Creates a manager with an explicit loader and provider set.
     *
     * @param classLoader class loader used for ServiceLoader discovery and icon resolution
     * @param providers providers to register
     */
    public RibbonManager(ClassLoader classLoader, Collection<? extends RibbonProvider> providers) {
        this.classLoader = classLoader == null ? resolveClassLoader() : classLoader;
        this.providers.addListener((ListChangeListener<RibbonProvider>) change -> refresh());
        this.context.addListener((obs, oldContext, newContext) -> refresh());
        this.quickAccessCommandIds.addListener((ListChangeListener<String>) change -> refreshQuickAccessCommandView());
        if (providers != null) {
            this.providers.addAll(providers);
        }
        refresh();
    }

    /**
     * Returns the class loader used for ServiceLoader discovery and icon
     * resolution.
     *
     * @return manager class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns the registered providers. Editing this list refreshes the
     * materialized tab model automatically.
     *
     * @return mutable provider list
     */
    public ObservableList<RibbonProvider> getProviders() {
        return providers;
    }

    /**
     * Returns the visible ribbon tabs for the current context.
     *
     * @return unmodifiable visible tab list
     */
    public ObservableList<RibbonTabSpec> getTabs() {
        return tabsView;
    }

    /**
     * Returns the long-lived command registry that owns canonical command
     * identities for every provider-contributed command plus any
     * host-registered QAT commands.
     *
     * @return command registry
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Returns the mutable list of command identifiers pinned to the Quick
     * Access Toolbar.
     *
     * <p>QAT state is stored by identifier, so hidden contextual commands
     * remain in the list and reappear when their owning tab becomes visible
     * again. Duplicate identifiers and blank entries are tolerated by
     * {@link #refreshQuickAccessCommandView()} but should be avoided for
     * predictable UI ordering.</p>
     *
     * @return mutable QAT command identifier list
     * @since Ribbon 2
     */
    public ObservableList<String> getQuickAccessCommandIds() {
        return quickAccessCommandIds;
    }

    /**
     * Returns the currently resolvable Quick Access Toolbar commands.
     *
     * <p>This is a derived, unmodifiable view of
     * {@link #getQuickAccessCommandIds()} joined against the
     * {@link #getCommandRegistry() command registry}. Entries without a
     * registered command are simply omitted; when the command is registered
     * later (for example, when a contextual tab becomes visible) the entry
     * reappears at the position implied by the identifier list.</p>
     *
     * <p><b>Ribbon 2 contract break:</b> the previous list was mutable and
     * hosts wrote command instances directly. Hosts should now mutate
     * {@link #getQuickAccessCommandIds()} and either rely on providers to
     * register the command or call
     * {@link #addQuickAccessCommand(PapiflyCommand)} for host-owned QAT
     * commands.</p>
     *
     * @return unmodifiable Quick Access Toolbar command view
     */
    public ObservableList<PapiflyCommand> getQuickAccessCommands() {
        return quickAccessCommandsView;
    }

    /**
     * Registers a host-owned command and appends its identifier to the Quick
     * Access Toolbar list if it is not already pinned.
     *
     * <p>The command is canonicalized through {@link #getCommandRegistry()},
     * so subsequent registrations of a command with the same identifier reuse
     * the existing instance. Use this method for QAT entries that are not
     * contributed by any {@link RibbonProvider}, for example application-level
     * Save/Undo/Redo actions.</p>
     *
     * @param command command to register and pin to the QAT
     * @return canonical command instance held by the registry
     * @throws NullPointerException if {@code command} is {@code null}
     */
    public PapiflyCommand addQuickAccessCommand(PapiflyCommand command) {
        Objects.requireNonNull(command, "command");
        PapiflyCommand canonical = commandRegistry.canonicalize(command);
        if (!quickAccessCommandIds.contains(canonical.id())) {
            quickAccessCommandIds.add(canonical.id());
        } else {
            // Identifier already pinned; ensure the derived view reflects the
            // canonical command (in case the registry previously missed it).
            refreshQuickAccessCommandView();
        }
        return canonical;
    }

    /**
     * Returns whether a tab with the supplied identifier is currently visible.
     *
     * @param tabId tab identifier
     * @return {@code true} when the current tab model contains the identifier
     */
    public boolean hasTab(String tabId) {
        if (tabId == null || tabId.isBlank()) {
            return false;
        }
        return tabs.stream().anyMatch(tab -> tabId.equals(tab.id()));
    }

    /**
     * Resolves command identifiers to currently known command instances.
     *
     * <p>Missing identifiers are ignored so persisted state remains tolerant of
     * removed providers or commands.</p>
     *
     * @param commandIds command identifiers to resolve
     * @return resolved commands in input order without duplicates
     */
    public List<PapiflyCommand> resolveCommandsById(List<String> commandIds) {
        if (commandIds == null || commandIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> emitted = new LinkedHashSet<>();
        List<PapiflyCommand> resolved = new ArrayList<>();
        for (String commandId : commandIds) {
            if (commandId == null || commandId.isBlank()) {
                continue;
            }
            commandRegistry.find(commandId).ifPresent(command -> {
                if (emitted.add(command.id())) {
                    resolved.add(command);
                }
            });
        }
        return resolved;
    }

    /**
     * Returns the current ribbon context.
     *
     * @return current ribbon context
     */
    public RibbonContext getContext() {
        return context.get();
    }

    /**
     * Updates the current ribbon context and refreshes visible tabs.
     *
     * @param context current ribbon context
     */
    public void setContext(RibbonContext context) {
        this.context.set(context == null ? RibbonContext.empty() : context);
    }

    /**
     * Returns the observable ribbon context property.
     *
     * @return ribbon context property
     */
    public ObjectProperty<RibbonContext> contextProperty() {
        return context;
    }

    void setLayoutTelemetry(RibbonLayoutTelemetry telemetry) {
        this.telemetry = telemetry == null ? RibbonLayoutTelemetry.noop() : telemetry;
    }

    /**
     * Re-runs ServiceLoader discovery against the manager class loader.
     */
    public void reloadProviders() {
        providers.setAll(discoverProviders(classLoader));
    }

    /**
     * Rebuilds the visible tab model for the current context.
     *
     * <p>The refresh cycle is split into two concerns:</p>
     * <ol>
     *   <li><b>Command registration</b> — every provider-contributed command
     *       is canonicalized through the {@link CommandRegistry} so repeated
     *       {@link RibbonProvider#getTabs(RibbonContext)} invocations do not
     *       churn command identities.</li>
     *   <li><b>Tab materialization</b> — the resulting specs are merged and
     *       sorted into the visible {@link #getTabs() tab list}. The
     *       materialized specs always reference canonical commands, so the
     *       rest of the ribbon shell can rely on reference equality.</li>
     * </ol>
     *
     * <p>After materialization the registry is pruned to keep only commands
     * that remain reachable either through visible tabs or through the Quick
     * Access Toolbar identifier list. The QAT command view is then rebuilt so
     * it reflects the latest registry state.</p>
     */
    public final void refresh() {
        RibbonContext resolvedContext = getContext() == null ? RibbonContext.empty() : getContext();
        List<RibbonTabSpec> contributions = new ArrayList<>();

        providers.stream()
            .filter(Objects::nonNull)
            .sorted(PROVIDER_COMPARATOR)
            .forEach(provider -> collectTabs(provider, resolvedContext, contributions));

        List<RibbonTabSpec> merged = mergeTabs(contributions);
        Set<String> reachable = new LinkedHashSet<>();
        List<RibbonTabSpec> canonical = canonicalizeTabs(merged, reachable);
        quickAccessCommandIds.stream()
            .filter(Objects::nonNull)
            .filter(id -> !id.isBlank())
            .forEach(reachable::add);
        commandRegistry.retain(reachable);

        tabs.setAll(canonical);
        refreshQuickAccessCommandView();
    }

    private void collectTabs(RibbonProvider provider, RibbonContext context, List<RibbonTabSpec> target) {
        try {
            List<RibbonTabSpec> providerTabs = provider.getTabs(context);
            if (providerTabs == null) {
                return;
            }
            providerTabs.stream()
                .filter(Objects::nonNull)
                .sorted(TAB_COMPARATOR)
                .filter(tab -> tab.isVisible(context))
                .forEach(target::add);
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Ribbon provider failed: " + provider.id(), exception);
            telemetry.providerFailure(provider.id(), exception);
        }
    }

    private List<RibbonTabSpec> mergeTabs(List<RibbonTabSpec> contributions) {
        Map<String, TabAccumulator> mergedTabs = new LinkedHashMap<>();
        contributions.stream()
            .sorted(TAB_COMPARATOR)
            .forEach(tab -> {
                TabAccumulator accumulator = mergedTabs.get(tab.id());
                if (accumulator == null) {
                    accumulator = new TabAccumulator(tab);
                    mergedTabs.put(tab.id(), accumulator);
                } else {
                    reportTabCollisionIfNeeded(accumulator, tab);
                }
                accumulator.merge(tab);
            });
        return mergedTabs.values().stream()
            .map(TabAccumulator::toSpec)
            .sorted(TAB_COMPARATOR)
            .toList();
    }

    private List<RibbonTabSpec> canonicalizeTabs(List<RibbonTabSpec> merged, Set<String> reachable) {
        List<RibbonTabSpec> result = new ArrayList<>(merged.size());
        for (RibbonTabSpec tab : merged) {
            List<RibbonGroupSpec> canonicalGroups = new ArrayList<>(tab.groups().size());
            for (RibbonGroupSpec group : tab.groups()) {
                PapiflyCommand dialogLauncher = group.dialogLauncher();
                PapiflyCommand canonicalLauncher = dialogLauncher == null
                    ? null
                    : canonicalizeAndTrack(dialogLauncher, reachable);
                List<RibbonControlSpec> canonicalControls = new ArrayList<>(group.controls().size());
                for (RibbonControlSpec control : group.controls()) {
                    canonicalControls.add(canonicalizeControl(control, reachable));
                }
                canonicalGroups.add(new RibbonGroupSpec(
                    group.id(),
                    group.label(),
                    group.order(),
                    group.collapseOrder(),
                    canonicalLauncher,
                    canonicalControls
                ));
            }
            result.add(new RibbonTabSpec(
                tab.id(),
                tab.label(),
                tab.order(),
                tab.contextual(),
                ribbonContext -> true,
                canonicalGroups
            ));
        }
        return result;
    }

    private RibbonControlSpec canonicalizeControl(RibbonControlSpec control, Set<String> reachable) {
        return switch (control) {
            case RibbonButtonSpec button ->
                new RibbonButtonSpec(canonicalizeAndTrack(button.command(), reachable));
            case RibbonToggleSpec toggle ->
                new RibbonToggleSpec(canonicalizeAndTrack(toggle.command(), reachable));
            case RibbonSplitButtonSpec split -> {
                PapiflyCommand primary = canonicalizeAndTrack(split.primaryCommand(), reachable);
                List<PapiflyCommand> secondary = new ArrayList<>(split.secondaryCommands().size());
                for (PapiflyCommand command : split.secondaryCommands()) {
                    if (command != null) {
                        secondary.add(canonicalizeAndTrack(command, reachable));
                    }
                }
                yield new RibbonSplitButtonSpec(primary, secondary);
            }
            case RibbonMenuSpec menu -> {
                List<PapiflyCommand> items = new ArrayList<>(menu.items().size());
                for (PapiflyCommand command : menu.items()) {
                    if (command != null) {
                        items.add(canonicalizeAndTrack(command, reachable));
                    }
                }
                yield new RibbonMenuSpec(menu.id(), menu.label(), menu.tooltip(), menu.smallIcon(), menu.largeIcon(), items);
            }
        };
    }

    private PapiflyCommand canonicalizeAndTrack(PapiflyCommand command, Set<String> reachable) {
        reportCommandCollisionIfNeeded(command);
        PapiflyCommand canonical = commandRegistry.canonicalize(command);
        reachable.add(canonical.id());
        return canonical;
    }

    private void reportTabCollisionIfNeeded(TabAccumulator accumulator, RibbonTabSpec incoming) {
        if (!accumulator.conflictsWith(incoming) || !reportedTabIdCollisions.add(incoming.id())) {
            return;
        }
        LOG.warning(() -> "Duplicate ribbon tab id '" + incoming.id()
            + "'; first label/order wins ('" + accumulator.label + "', " + accumulator.order
            + "), ignoring conflicting label/order ('" + incoming.label() + "', " + incoming.order() + ")");
        telemetry.tabIdCollision(
            incoming.id(),
            accumulator.label,
            accumulator.order,
            incoming.label(),
            incoming.order()
        );
    }

    private void reportCommandCollisionIfNeeded(PapiflyCommand incoming) {
        commandRegistry.find(incoming.id()).ifPresent(existing -> {
            if (existing == incoming
                || !commandPresentationDiffers(existing, incoming)
                || !reportedCommandIdCollisions.add(incoming.id())) {
                return;
            }
            LOG.warning(() -> "Duplicate ribbon command id '" + incoming.id()
                + "'; first command metadata wins ('" + existing.label()
                + "'), projecting runtime state/action from ignored metadata ('" + incoming.label() + "')");
            telemetry.commandIdCollision(incoming.id(), existing.label(), incoming.label());
        });
    }

    private static boolean commandPresentationDiffers(PapiflyCommand left, PapiflyCommand right) {
        return !Objects.equals(left.label(), right.label())
            || !Objects.equals(left.tooltip(), right.tooltip())
            || !Objects.equals(iconPath(left.smallIcon()), iconPath(right.smallIcon()))
            || !Objects.equals(iconPath(left.largeIcon()), iconPath(right.largeIcon()));
    }

    private static String iconPath(org.metalib.papifly.fx.api.ribbon.RibbonIconHandle iconHandle) {
        return iconHandle == null ? null : iconHandle.resourcePath();
    }

    private void refreshQuickAccessCommandView() {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<PapiflyCommand> resolved = new ArrayList<>();
        for (String id : quickAccessCommandIds) {
            if (id == null || id.isBlank() || !seen.add(id)) {
                continue;
            }
            commandRegistry.find(id).ifPresent(resolved::add);
        }
        quickAccessCommands.setAll(resolved);
    }

    private static List<RibbonProvider> discoverProviders(ClassLoader classLoader) {
        ClassLoader loader = classLoader == null ? resolveClassLoader() : classLoader;
        List<RibbonProvider> discovered = new ArrayList<>();
        ServiceLoader.load(RibbonProvider.class, loader).forEach(discovered::add);
        return discovered;
    }

    private static ClassLoader resolveClassLoader() {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader == null ? RibbonManager.class.getClassLoader() : contextLoader;
    }

    private static final class TabAccumulator {
        private final String id;
        private final String label;
        private final int order;
        private boolean contextual;
        private final Map<String, GroupAccumulator> groups = new LinkedHashMap<>();

        private TabAccumulator(RibbonTabSpec initial) {
            this.id = initial.id();
            this.label = initial.label();
            this.order = initial.order();
            this.contextual = initial.contextual();
        }

        private void merge(RibbonTabSpec tab) {
            contextual = contextual || tab.contextual();
            tab.groups().stream()
                .sorted(GROUP_COMPARATOR)
                .forEach(group -> groups.computeIfAbsent(group.id(), ignored -> new GroupAccumulator(group)).merge(group));
        }

        private RibbonTabSpec toSpec() {
            List<RibbonGroupSpec> mergedGroups = groups.values().stream()
                .map(GroupAccumulator::toSpec)
                .sorted(GROUP_COMPARATOR)
                .toList();
            return new RibbonTabSpec(id, label, order, contextual, ribbonContext -> true, mergedGroups);
        }

        private boolean conflictsWith(RibbonTabSpec tab) {
            return !Objects.equals(label, tab.label()) || order != tab.order();
        }
    }

    private static final class GroupAccumulator {
        private final String id;
        private final String label;
        private final int order;
        private final int collapseOrder;
        private PapiflyCommand dialogLauncher;
        private final List<RibbonControlSpec> controls = new ArrayList<>();

        private GroupAccumulator(RibbonGroupSpec initial) {
            this.id = initial.id();
            this.label = initial.label();
            this.order = initial.order();
            this.collapseOrder = initial.collapseOrder();
            this.dialogLauncher = initial.dialogLauncher();
        }

        private void merge(RibbonGroupSpec group) {
            if (dialogLauncher == null && group.dialogLauncher() != null) {
                dialogLauncher = group.dialogLauncher();
            }
            controls.addAll(group.controls().stream().filter(Objects::nonNull).toList());
        }

        private RibbonGroupSpec toSpec() {
            return new RibbonGroupSpec(id, label, order, collapseOrder, dialogLauncher, controls);
        }
    }
}
