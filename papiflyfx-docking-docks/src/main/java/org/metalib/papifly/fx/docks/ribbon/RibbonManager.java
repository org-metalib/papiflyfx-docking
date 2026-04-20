package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers ribbon providers and materializes the visible tab model for a
 * runtime {@link RibbonContext}.
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
            .thenComparingInt(RibbonGroupSpec::reductionPriority)
            .thenComparing(RibbonGroupSpec::id);

    private final ObservableList<RibbonProvider> providers = FXCollections.observableArrayList();
    private final ObservableList<RibbonTabSpec> tabs = FXCollections.observableArrayList();
    private final ObservableList<RibbonTabSpec> tabsView = FXCollections.unmodifiableObservableList(tabs);
    private final ObservableList<PapiflyCommand> quickAccessCommands = FXCollections.observableArrayList();
    private final ObjectProperty<RibbonContext> context = new SimpleObjectProperty<>(RibbonContext.empty());
    private final ClassLoader classLoader;

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
     * Returns the Quick Access Toolbar commands owned by the host.
     *
     * @return mutable quick access command list
     */
    public ObservableList<PapiflyCommand> getQuickAccessCommands() {
        return quickAccessCommands;
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

    /**
     * Re-runs ServiceLoader discovery against the manager class loader.
     */
    public void reloadProviders() {
        providers.setAll(discoverProviders(classLoader));
    }

    /**
     * Rebuilds the visible tab model for the current context.
     */
    public final void refresh() {
        RibbonContext resolvedContext = getContext() == null ? RibbonContext.empty() : getContext();
        List<RibbonTabSpec> contributions = new ArrayList<>();

        providers.stream()
            .filter(Objects::nonNull)
            .sorted(PROVIDER_COMPARATOR)
            .forEach(provider -> collectTabs(provider, resolvedContext, contributions));

        tabs.setAll(mergeTabs(contributions));
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
        }
    }

    private List<RibbonTabSpec> mergeTabs(List<RibbonTabSpec> contributions) {
        Map<String, TabAccumulator> mergedTabs = new LinkedHashMap<>();
        contributions.stream()
            .sorted(TAB_COMPARATOR)
            .forEach(tab -> mergedTabs.computeIfAbsent(tab.id(), ignored -> new TabAccumulator(tab)).merge(tab));
        return mergedTabs.values().stream()
            .map(TabAccumulator::toSpec)
            .sorted(TAB_COMPARATOR)
            .toList();
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
            merge(initial);
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
    }

    private static final class GroupAccumulator {
        private final String id;
        private final String label;
        private final int order;
        private final int reductionPriority;
        private PapiflyCommand dialogLauncher;
        private final List<RibbonControlSpec> controls = new ArrayList<>();

        private GroupAccumulator(RibbonGroupSpec initial) {
            this.id = initial.id();
            this.label = initial.label();
            this.order = initial.order();
            this.reductionPriority = initial.reductionPriority();
            this.dialogLauncher = initial.dialogLauncher();
            merge(initial);
        }

        private void merge(RibbonGroupSpec group) {
            if (dialogLauncher == null && group.dialogLauncher() != null) {
                dialogLauncher = group.dialogLauncher();
            }
            controls.addAll(group.controls().stream().filter(Objects::nonNull).toList());
        }

        private RibbonGroupSpec toSpec() {
            return new RibbonGroupSpec(id, label, order, reductionPriority, dialogLauncher, controls);
        }
    }
}
