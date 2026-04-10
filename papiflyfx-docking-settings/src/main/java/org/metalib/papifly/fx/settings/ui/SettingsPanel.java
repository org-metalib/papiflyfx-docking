package org.metalib.papifly.fx.settings.ui;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsCategoryDefinitions;
import org.metalib.papifly.fx.settings.api.SettingsCategoryMetadata;
import org.metalib.papifly.fx.settings.api.SettingsCategoryUI;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.api.SettingsContributor;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class SettingsPanel extends BorderPane implements DisposableContent {

    private final SettingsRuntime runtime;
    private final SettingsSearchBar searchBar;
    private final SettingsCategoryList categoryList;
    private final ScrollPane contentScroll;
    private final VBox contentArea;
    private final SettingsToolbar toolbar;
    private final Map<String, SettingsCategory> categoriesById = new LinkedHashMap<>();
    private final Map<String, Node> paneCache = new LinkedHashMap<>();
    private final ChangeListener<Theme> themeListener;
    private final ChangeListener<Boolean> dirtyBindingListener = (obs, oldValue, newValue) -> refreshToolbarState();

    private SettingsCategory activeCategory;
    private ReadOnlyBooleanProperty activeDirtyProperty;
    private String initialCategoryId;

    public SettingsPanel(SettingsRuntime runtime) {
        this(runtime, null);
    }

    public SettingsPanel(SettingsRuntime runtime, String initialCategoryId) {
        this.runtime = runtime;
        this.initialCategoryId = initialCategoryId;
        this.searchBar = new SettingsSearchBar();
        this.categoryList = new SettingsCategoryList();
        this.contentArea = new VBox();
        this.contentScroll = new ScrollPane(contentArea);
        this.toolbar = new SettingsToolbar();

        contentArea.setPadding(new Insets(12));
        VBox.setVgrow(contentScroll, Priority.ALWAYS);
        contentScroll.setFitToWidth(true);
        contentScroll.setFitToHeight(true);
        contentScroll.setContent(contentArea);

        setTop(searchBar);
        setLeft(categoryList);
        setCenter(contentScroll);
        setBottom(toolbar);

        searchBar.getSearchField().textProperty().addListener((obs, oldValue, newValue) -> {
            categoryList.filter(newValue);
            ensureSelectedCategoryVisible();
        });
        categoryList.selectedCategoryProperty().addListener((obs, oldValue, newValue) -> showCategory(newValue));
        toolbar.onApply(this::applyActiveCategory);
        toolbar.onReset(this::resetActiveCategory);
        toolbar.activeScopeProperty().addListener((obs, oldValue, newValue) -> onScopeChanged(newValue));

        themeListener = (obs, oldTheme, newTheme) -> applyTheme(newTheme);
        runtime.themeProperty().addListener(themeListener);
        applyTheme(runtime.themeProperty().get());

        loadCategories();
    }

    public void applyActiveCategory() {
        if (activeCategory == null) {
            return;
        }
        activeCategory.apply(currentContext());
        refreshToolbarState();
    }

    public void resetActiveCategory() {
        if (activeCategory == null) {
            return;
        }
        activeCategory.reset(currentContext());
        refreshToolbarState();
    }

    public String getActiveCategoryId() {
        return activeCategory == null ? null : activeCategory.id();
    }

    public void selectCategory(String categoryId) {
        categoryList.selectById(categoryId);
    }

    public SettingsSearchBar searchBar() {
        return searchBar;
    }

    public List<String> visibleCategoryIds() {
        return categoryList.visibleCategoryIds();
    }

    @Override
    public void dispose() {
        runtime.themeProperty().removeListener(themeListener);
        unbindDirtyProperty();
        runtime.storage().save();
    }

    private void loadCategories() {
        Map<String, SettingsCategory> loaded = new LinkedHashMap<>();
        ServiceLoader.load(SettingsCategory.class).forEach(category -> loaded.put(category.id(), category));
        ServiceLoader.load(SettingsContributor.class).forEach(contributor ->
            contributor.getCategories().forEach(category -> loaded.put(category.id(), category))
        );
        List<SettingsCategory> categories = new ArrayList<>(loaded.values());
        categories.sort(Comparator.comparingInt(SettingsCategoryMetadata::order).thenComparing(SettingsCategoryMetadata::displayName));
        categoriesById.clear();
        for (SettingsCategory category : categories) {
            categoriesById.put(category.id(), category);
        }
        categoryList.setCategories(categories);
        if (initialCategoryId != null && categoriesById.containsKey(initialCategoryId)) {
            categoryList.selectById(initialCategoryId);
            initialCategoryId = null;
        } else if (!categories.isEmpty()) {
            categoryList.getSelectionModel().select(categories.getFirst());
        }
    }

    private void ensureSelectedCategoryVisible() {
        if (activeCategory != null && visibleCategoryIds().contains(activeCategory.id())) {
            return;
        }
        List<String> visibleIds = visibleCategoryIds();
        if (!visibleIds.isEmpty()) {
            categoryList.selectById(visibleIds.getFirst());
        } else {
            contentArea.getChildren().clear();
            activeCategory = null;
        }
    }

    private void showCategory(SettingsCategory category) {
        unbindDirtyProperty();
        if (category == null) {
            contentArea.getChildren().clear();
            activeCategory = null;
            refreshToolbarState();
            return;
        }
        activeCategory = category;
        toolbar.setSupportedScopes(category.supportedScopes());
        Node pane = paneCache.computeIfAbsent(category.id(), ignored -> buildPane(category));
        contentArea.getChildren().setAll(pane);
        bindDirtyProperty(category);
        refreshToolbarState();
    }

    private void bindDirtyProperty(SettingsCategoryUI category) {
        ReadOnlyBooleanProperty dp = category.dirtyProperty();
        if (dp != null) {
            activeDirtyProperty = dp;
            dp.addListener(dirtyBindingListener);
        }
    }

    private void unbindDirtyProperty() {
        if (activeDirtyProperty != null) {
            activeDirtyProperty.removeListener(dirtyBindingListener);
            activeDirtyProperty = null;
        }
    }

    private void onScopeChanged(SettingScope scope) {
        paneCache.clear();
        if (activeCategory != null) {
            showCategory(activeCategory);
        }
        refreshToolbarState();
    }

    private SettingsContext currentContext() {
        return runtime.context(toolbar.getActiveScope());
    }

    private void refreshToolbarState() {
        toolbar.setDirty(activeCategory != null && isDirty(activeCategory));
        toolbar.setActions(activeCategory == null ? List.of() : activeCategory.actions(), this::currentContext);
    }

    private Node buildPane(SettingsCategoryUI category) {
        return category.buildSettingsPane(currentContext());
    }

    private boolean isDirty(SettingsCategoryUI category) {
        return category.isDirty();
    }

    private void applyTheme(Theme theme) {
        String background = ThemeStyleSupport.toCss(theme.background());
        String headerBackground = ThemeStyleSupport.toCss(theme.headerBackground());
        String border = ThemeStyleSupport.toCss(theme.borderColor());
        String accent = ThemeStyleSupport.toCss(theme.accentColor());
        setStyle("-fx-background-color: " + background + ";");
        categoryList.setStyle("-fx-control-inner-background: " + headerBackground + "; -fx-border-color: " + border + ";");
        contentArea.setStyle("-fx-background-color: " + background + ";");
        contentScroll.setStyle("-fx-background: " + background + "; -fx-border-color: " + border + ";");
        searchBar.setStyle("-fx-background-color: " + headerBackground + "; -fx-border-color: " + border + ";");
        searchBar.getSearchField().setStyle("-fx-highlight-fill: " + accent + ";");
        toolbar.setStyle("-fx-background-color: " + headerBackground + "; -fx-border-color: " + border + ";");
    }
}
