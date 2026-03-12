package org.metalib.papifly.fx.github.api;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.git.JGitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.ui.CommandRunner;
import org.metalib.papifly.fx.github.ui.GitHubToolbarViewModel;
import org.metalib.papifly.fx.github.ui.dialog.CommitDialog;
import org.metalib.papifly.fx.github.ui.dialog.DirtyCheckoutAlert;
import org.metalib.papifly.fx.github.ui.dialog.NewBranchDialog;
import org.metalib.papifly.fx.github.ui.dialog.PullRequestDialog;
import org.metalib.papifly.fx.github.ui.dialog.RollbackDialog;
import org.metalib.papifly.fx.github.ui.dialog.TokenDialog;
import org.metalib.papifly.fx.github.ui.theme.GitHubThemeSupport;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarTheme;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarThemeMapper;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GitHubToolbar extends HBox implements AutoCloseable {

    public static final String FACTORY_ID = "github-toolbar";

    private static final List<String> BADGE_VARIANTS = List.of(
        "pf-github-badge-accent",
        "pf-github-badge-success",
        "pf-github-badge-warning",
        "pf-github-badge-danger",
        "pf-github-badge-muted"
    );

    private final GitHubToolbarViewModel viewModel;

    private final Hyperlink repoLink;
    private final HBox repoLinkSurface;
    private final ComboBox<String> branchCombo;
    private final Button checkoutButton;
    private final Button newBranchButton;
    private final Button commitButton;
    private final Button rollbackButton;
    private final Button pushButton;
    private final Button prButton;
    private final Button tokenButton;
    private final Label currentBranchBadge;
    private final Label dirtyBadge;
    private final Label modeBadge;
    private final Label defaultBranchBadge;
    private final Label detachedBadge;
    private final Label aheadBadge;
    private final Label behindBadge;
    private final Label authBadge;
    private final Label statusLabel;
    private final Label errorLabel;
    private final ProgressIndicator busyIndicator;
    private final HBox repoGroup;
    private final HBox branchGroup;
    private final HBox changesGroup;
    private final HBox remoteGroup;
    private final HBox statusGroup;

    private ObjectProperty<Theme> themeProperty;
    private ChangeListener<Theme> themeListener;
    private Theme currentBaseTheme;
    private GitHubToolbarTheme currentTheme;

    public GitHubToolbar(GitHubRepoContext context) {
        this(context, new PatCredentialStore(), null);
    }

    public GitHubToolbar(GitHubRepoContext context, CredentialStore credentialStore) {
        this(context, credentialStore, null);
    }

    public GitHubToolbar(GitHubRepoContext context, CredentialStore credentialStore, Theme initialTheme) {
        this(buildViewModel(context, credentialStore), initialTheme);
    }

    public GitHubToolbar(GitHubToolbarViewModel viewModel) {
        this(viewModel, null);
    }

    public GitHubToolbar(GitHubToolbarViewModel viewModel, Theme initialTheme) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");

        GitHubThemeSupport.ensureStylesheetLoaded(this, GitHubThemeSupport.TOOLBAR_STYLESHEET);
        getStyleClass().add("pf-github-toolbar");
        setId("github-toolbar");
        setAlignment(Pos.CENTER_LEFT);

        repoLink = new Hyperlink(viewModel.context().owner() + "/" + viewModel.context().repo());
        repoLink.setId("github-repo-link");
        repoLink.getStyleClass().add("pf-github-repo-link");
        repoLink.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        repoLink.setMaxWidth(260);
        repoLink.setOnAction(event -> openBrowser(viewModel.context().remoteUrl()));

        repoLinkSurface = new HBox(repoLink);
        repoLinkSurface.setAlignment(Pos.CENTER_LEFT);
        repoLinkSurface.setId("github-repo-link-surface");
        repoLinkSurface.getStyleClass().add("pf-github-link-surface");

        branchCombo = new ComboBox<>(FXCollections.observableArrayList());
        branchCombo.setId("github-branch-combo");
        branchCombo.getStyleClass().add("pf-github-combo");
        branchCombo.setPrefWidth(190);
        branchCombo.setVisibleRowCount(12);

        checkoutButton = createButton("Checkout", "github-checkout-button");
        checkoutButton.setOnAction(event -> onCheckout());

        newBranchButton = createButton("New Branch", "github-new-branch-button");
        newBranchButton.setOnAction(event -> onCreateBranch());

        commitButton = createButton("Commit", "github-commit-button");
        commitButton.setOnAction(event -> onCommit());

        rollbackButton = createButton("Rollback", "github-rollback-button", "pf-github-button-outline-danger");
        rollbackButton.setOnAction(event -> onRollback());

        pushButton = createButton("Push", "github-push-button", "pf-github-button-primary");
        pushButton.setOnAction(event -> viewModel.push());

        prButton = createButton("Create PR", "github-pr-button", "pf-github-button-outline-accent");
        prButton.setOnAction(event -> onCreatePullRequest());

        tokenButton = createButton("Token", "github-token-button");
        tokenButton.setOnAction(event -> onToken());

        currentBranchBadge = createBadge("github-current-branch-badge");
        dirtyBadge = createBadge("github-dirty-badge");
        modeBadge = createBadge("github-mode-badge");
        defaultBranchBadge = createBadge("github-default-branch-badge");
        detachedBadge = createBadge("github-detached-badge");
        aheadBadge = createBadge("github-ahead-badge");
        behindBadge = createBadge("github-behind-badge");
        authBadge = createBadge("github-auth-badge");

        busyIndicator = new ProgressIndicator();
        busyIndicator.setId("github-busy-indicator");
        busyIndicator.getStyleClass().add("pf-github-busy-indicator");
        busyIndicator.setPrefSize(16, 16);
        busyIndicator.setMaxSize(16, 16);
        busyIndicator.visibleProperty().bind(viewModel.busyProperty());
        busyIndicator.managedProperty().bind(busyIndicator.visibleProperty());

        statusLabel = new Label();
        statusLabel.setId("github-status-text");
        statusLabel.getStyleClass().add("pf-github-status-label");
        statusLabel.textProperty().bind(viewModel.statusTextProperty());

        errorLabel = new Label();
        errorLabel.setId("github-error-text");
        errorLabel.getStyleClass().add("pf-github-error-label");
        errorLabel.textProperty().bind(viewModel.errorTextProperty());
        errorLabel.visibleProperty().bind(viewModel.errorTextProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        repoGroup = createGroup("github-repo-group");
        repoGroup.getChildren().addAll(
            repoLinkSurface,
            currentBranchBadge,
            dirtyBadge,
            modeBadge,
            defaultBranchBadge,
            detachedBadge
        );

        branchGroup = createGroup("github-branch-group");
        branchGroup.getChildren().addAll(branchCombo, checkoutButton, newBranchButton);

        changesGroup = createGroup("github-changes-group");
        changesGroup.getChildren().addAll(commitButton, rollbackButton);

        remoteGroup = createGroup("github-remote-group");
        remoteGroup.getChildren().addAll(pushButton, prButton, tokenButton);

        statusGroup = createGroup("github-status-group", "pf-github-status-group");
        statusGroup.getChildren().addAll(busyIndicator, aheadBadge, behindBadge, authBadge, statusLabel, errorLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().setAll(repoGroup, branchGroup, changesGroup, remoteGroup, spacer, statusGroup);

        bindManagedToVisible(branchGroup);
        bindManagedToVisible(changesGroup);
        bindManagedToVisible(pushButton);

        branchGroup.visibleProperty().bind(viewModel.localAvailableProperty());
        changesGroup.visibleProperty().bind(viewModel.localAvailableProperty());
        pushButton.visibleProperty().bind(viewModel.localAvailableProperty());

        commitButton.disableProperty().bind(viewModel.commitDisabledProperty());
        pushButton.disableProperty().bind(viewModel.pushDisabledProperty());
        prButton.disableProperty().bind(viewModel.pullRequestDisabledProperty());
        checkoutButton.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        newBranchButton.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        rollbackButton.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        branchCombo.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        tokenButton.disableProperty().bind(viewModel.busyProperty());

        viewModel.branchesProperty().addListener((obs, oldList, newList) -> syncBranches(newList));
        viewModel.currentBranchProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                branchCombo.setValue(newValue);
            }
            refreshBadges();
        });
        viewModel.localAvailableProperty().addListener((obs, oldValue, newValue) -> refreshBadges());
        viewModel.dirtyProperty().addListener((obs, oldValue, newValue) -> refreshBadges());
        viewModel.dirtyCountProperty().addListener((obs, oldValue, newValue) -> refreshBadges());
        viewModel.defaultBranchActiveProperty().addListener((obs, oldValue, newValue) -> refreshBadges());
        viewModel.detachedHeadProperty().addListener((obs, oldValue, newValue) -> refreshBadges());
        viewModel.aheadCountProperty().addListener((obs, oldValue, newValue) -> refreshBadges());
        viewModel.behindCountProperty().addListener((obs, oldValue, newValue) -> refreshBadges());
        viewModel.authenticatedProperty().addListener((obs, oldValue, newValue) -> refreshBadges());

        syncBranches(viewModel.branchesProperty());
        applyTheme(initialTheme == null ? Theme.dark() : initialTheme);
        refreshBadges();
        viewModel.refresh();
    }

    public void bindThemeProperty(ObjectProperty<Theme> themeProperty) {
        unbindThemeProperty();
        this.themeProperty = themeProperty;
        if (themeProperty == null) {
            return;
        }
        themeListener = (obs, oldTheme, newTheme) -> applyTheme(newTheme);
        themeProperty.addListener(themeListener);
        applyTheme(themeProperty.get());
    }

    public void unbindThemeProperty() {
        if (themeProperty != null && themeListener != null) {
            themeProperty.removeListener(themeListener);
        }
        themeProperty = null;
        themeListener = null;
    }

    public GitHubRepoContext context() {
        return viewModel.context();
    }

    public Map<String, Object> captureState() {
        String localPath = context().localClonePath() == null ? "" : context().localClonePath().toString();
        return Map.of(
            "remoteUrl", context().remoteUrl().toString(),
            "localClonePath", localPath,
            "owner", context().owner(),
            "repo", context().repo()
        );
    }

    @Override
    public void close() {
        unbindThemeProperty();
        viewModel.close();
    }

    private void onCheckout() {
        String selected = branchCombo.getValue();
        if (selected == null || selected.isBlank()) {
            return;
        }
        boolean force = false;
        String currentBranch = viewModel.currentBranchProperty().get();
        if (viewModel.dirtyProperty().get() && !selected.equals(currentBranch)) {
            force = DirtyCheckoutAlert.confirm(selected, activeTheme());
            if (!force) {
                return;
            }
        }
        viewModel.switchBranch(selected, force);
    }

    private void onCreateBranch() {
        NewBranchDialog dialog = new NewBranchDialog(
            viewModel.branchesProperty(),
            viewModel.currentBranchProperty().get(),
            activeTheme()
        );
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(result -> viewModel.createBranch(result.name(), result.startPoint()));
    }

    private void onCommit() {
        CommitDialog dialog = new CommitDialog(activeTheme());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::commit);
    }

    private void onRollback() {
        RollbackDialog dialog = new RollbackDialog(viewModel.isHeadPushed(), activeTheme());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::rollback);
    }

    private void onCreatePullRequest() {
        PullRequestDialog dialog = new PullRequestDialog(
            viewModel.currentBranchProperty().get(),
            viewModel.defaultBranchProperty().get(),
            activeTheme()
        );
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(draft ->
            viewModel.createPullRequest(draft, result -> onPullRequestCreated(result, draft.openInBrowser())));
    }

    private void onPullRequestCreated(PullRequestResult result, boolean openInBrowser) {
        if (openInBrowser) {
            openBrowser(result.url());
        }
    }

    private void onToken() {
        TokenDialog dialog = new TokenDialog(activeTheme());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::saveToken);
    }

    private void syncBranches(List<BranchRef> refs) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (BranchRef ref : refs) {
            if (ref.local()) {
                names.add(ref.name());
            }
        }
        if (names.isEmpty()) {
            for (BranchRef ref : refs) {
                names.add(ref.name());
            }
        }
        ObservableList<String> items = branchCombo.getItems();
        items.setAll(names);
        String current = viewModel.currentBranchProperty().get();
        if (current != null && !current.isBlank()) {
            branchCombo.setValue(current);
        }
    }

    private void refreshBadges() {
        boolean localAvailable = viewModel.localAvailableProperty().get();
        String currentBranch = viewModel.currentBranchProperty().get();
        boolean hasBranch = currentBranch != null && !currentBranch.isBlank();
        boolean dirty = viewModel.dirtyProperty().get();
        int dirtyCount = viewModel.dirtyCountProperty().get();
        int aheadCount = viewModel.aheadCountProperty().get();
        int behindCount = viewModel.behindCountProperty().get();
        boolean authenticated = viewModel.authenticatedProperty().get();

        currentBranchBadge.setText(currentBranch == null ? "" : currentBranch);
        currentBranchBadge.setVisible(localAvailable && hasBranch);
        setBadgeVariant(currentBranchBadge, "pf-github-badge-accent");

        dirtyBadge.setText(dirtyCount > 0 ? "Dirty " + dirtyCount : "Dirty");
        dirtyBadge.setVisible(localAvailable && dirty);
        setBadgeVariant(dirtyBadge, "pf-github-badge-warning");

        modeBadge.setText(localAvailable ? "Local clone" : "Remote only");
        modeBadge.setVisible(true);
        setBadgeVariant(modeBadge, localAvailable ? "pf-github-badge-success" : "pf-github-badge-muted");

        defaultBranchBadge.setText("Default");
        defaultBranchBadge.setVisible(localAvailable && viewModel.defaultBranchActiveProperty().get());
        setBadgeVariant(defaultBranchBadge, "pf-github-badge-warning");

        detachedBadge.setText("Detached");
        detachedBadge.setVisible(localAvailable && viewModel.detachedHeadProperty().get());
        setBadgeVariant(detachedBadge, "pf-github-badge-danger");

        aheadBadge.setText("Ahead " + aheadCount);
        aheadBadge.setVisible(localAvailable && aheadCount > 0);
        setBadgeVariant(aheadBadge, "pf-github-badge-accent");

        behindBadge.setText("Behind " + behindCount);
        behindBadge.setVisible(localAvailable && behindCount > 0);
        setBadgeVariant(behindBadge, "pf-github-badge-warning");

        authBadge.setText(authenticated ? "Token" : "No token");
        authBadge.setVisible(true);
        setBadgeVariant(authBadge, authenticated ? "pf-github-badge-success" : "pf-github-badge-muted");
    }

    private void openBrowser(URI uri) {
        if (!Desktop.isDesktopSupported()) {
            viewModel.publishError("Desktop browser integration is not available");
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException ex) {
            viewModel.publishError("Failed to open browser: " + ex.getMessage());
        }
    }

    private void applyTheme(Theme theme) {
        currentBaseTheme = theme == null ? Theme.dark() : theme;
        currentTheme = GitHubToolbarThemeMapper.map(currentBaseTheme);

        setStyle(buildRootStyle(currentBaseTheme, currentTheme));
        setSpacing(currentTheme.groupGap());
        setPadding(currentTheme.contentPadding());
        setMinHeight(currentTheme.toolbarHeight());
        setPrefHeight(currentTheme.toolbarHeight());

        double groupSpacing = currentTheme.groupGap();
        for (HBox group : List.of(repoGroup, branchGroup, changesGroup, remoteGroup, statusGroup)) {
            group.setSpacing(groupSpacing);
        }

        double buttonHeight = currentTheme.buttonHeight();
        for (Button button : List.of(checkoutButton, newBranchButton, commitButton, rollbackButton, pushButton, prButton, tokenButton)) {
            button.setMinHeight(buttonHeight);
            button.setPrefHeight(buttonHeight);
            button.setMaxHeight(buttonHeight);
        }
        branchCombo.setMinHeight(buttonHeight);
        branchCombo.setPrefHeight(buttonHeight);
        branchCombo.setMaxHeight(buttonHeight);

        for (Labeled labeled : List.of(
            repoLink,
            currentBranchBadge,
            dirtyBadge,
            modeBadge,
            defaultBranchBadge,
            detachedBadge,
            aheadBadge,
            behindBadge,
            authBadge,
            statusLabel,
            errorLabel,
            checkoutButton,
            newBranchButton,
            commitButton,
            rollbackButton,
            pushButton,
            prButton,
            tokenButton
        )) {
            labeled.setFont(currentBaseTheme.contentFont());
        }
    }

    private Theme activeTheme() {
        return currentBaseTheme == null ? Theme.dark() : currentBaseTheme;
    }

    private static HBox createGroup(String id, String... extraClasses) {
        HBox group = new HBox();
        group.setId(id);
        group.setAlignment(Pos.CENTER_LEFT);
        group.getStyleClass().add("pf-github-group");
        group.getStyleClass().addAll(extraClasses);
        return group;
    }

    private static Button createButton(String text, String id, String... extraClasses) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().add("pf-github-button");
        button.getStyleClass().addAll(extraClasses);
        return button;
    }

    private static Label createBadge(String id) {
        Label badge = new Label();
        badge.setId(id);
        badge.getStyleClass().add("pf-github-badge");
        bindManagedToVisible(badge);
        badge.setVisible(false);
        return badge;
    }

    private static void bindManagedToVisible(Node node) {
        node.managedProperty().bind(node.visibleProperty());
    }

    private static void setBadgeVariant(Label badge, String variant) {
        badge.getStyleClass().removeAll(BADGE_VARIANTS);
        if (variant != null && !variant.isBlank()) {
            badge.getStyleClass().add(variant);
        }
    }

    private static String buildRootStyle(Theme baseTheme, GitHubToolbarTheme toolbarTheme) {
        String family = baseTheme.contentFont() == null ? "System" : baseTheme.contentFont().getFamily().replace("\"", "\\\"");
        double size = baseTheme.contentFont() == null ? 12.0 : baseTheme.contentFont().getSize();
        return GitHubThemeSupport.themeVariables(toolbarTheme)
            + String.format(Locale.ROOT, "-fx-font-family: \"%s\";-fx-font-size: %.1fpx;", family, size);
    }

    private static GitHubToolbarViewModel buildViewModel(GitHubRepoContext context, CredentialStore credentialStore) {
        GitRepository gitRepository = null;
        if (context.hasLocalClone()) {
            gitRepository = new JGitRepository(context.localClonePath(), credentialStore::toJGitCredentials);
        }
        GitHubApiService apiService = new GitHubApiService(java.net.http.HttpClient.newHttpClient(), credentialStore::getToken);
        return new GitHubToolbarViewModel(context, credentialStore, gitRepository, apiService, new CommandRunner());
    }

    public static GitHubToolbar fromState(Map<String, Object> state) {
        return fromState(state, null);
    }

    public static GitHubToolbar fromState(Map<String, Object> state, Theme initialTheme) {
        String remoteUrl = readString(state, "remoteUrl");
        String localClonePath = readString(state, "localClonePath");
        try {
            GitHubRepoContext context = (localClonePath == null || localClonePath.isBlank())
                ? GitHubRepoContext.remoteOnly(new URI(remoteUrl))
                : GitHubRepoContext.of(new URI(remoteUrl), java.nio.file.Path.of(localClonePath));
            return new GitHubToolbar(context, new PatCredentialStore(), initialTheme);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid persisted remote URL", ex);
        }
    }

    private static String readString(Map<String, Object> state, String key) {
        Object value = state.get(key);
        if (value instanceof String text) {
            return text;
        }
        return "";
    }
}
