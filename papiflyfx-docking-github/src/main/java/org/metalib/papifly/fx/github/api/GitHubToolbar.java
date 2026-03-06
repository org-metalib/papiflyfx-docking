package org.metalib.papifly.fx.github.api;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GitHubToolbar extends HBox implements AutoCloseable {

    public static final String FACTORY_ID = "github-toolbar";

    private final GitHubToolbarViewModel viewModel;

    private final ComboBox<String> branchCombo;
    private final Label dirtyDot;
    private final Label statusLabel;
    private final Label errorLabel;
    private final ProgressIndicator busyIndicator;

    private ObjectProperty<Theme> themeProperty;
    private ChangeListener<Theme> themeListener;

    public GitHubToolbar(GitHubRepoContext context) {
        this(context, new PatCredentialStore());
    }

    public GitHubToolbar(GitHubRepoContext context, CredentialStore credentialStore) {
        this(buildViewModel(context, credentialStore));
    }

    public GitHubToolbar(GitHubToolbarViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");

        setId("github-toolbar");
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(6, 10, 6, 10));

        Hyperlink repoLink = new Hyperlink(viewModel.context().owner() + "/" + viewModel.context().repo());
        repoLink.setId("github-repo-link");
        repoLink.setOnAction(event -> openBrowser(viewModel.context().remoteUrl()));

        dirtyDot = new Label("●");
        dirtyDot.setId("github-dirty-dot");
        dirtyDot.setVisible(false);
        dirtyDot.managedProperty().bind(dirtyDot.visibleProperty());

        branchCombo = new ComboBox<>(FXCollections.observableArrayList());
        branchCombo.setId("github-branch-combo");
        branchCombo.setPrefWidth(180);

        Button checkoutButton = new Button("Checkout");
        checkoutButton.setId("github-checkout-button");
        checkoutButton.setOnAction(event -> onCheckout());

        Button newBranchButton = new Button("New Branch");
        newBranchButton.setId("github-new-branch-button");
        newBranchButton.setOnAction(event -> onCreateBranch());

        Button commitButton = new Button("Commit");
        commitButton.setId("github-commit-button");
        commitButton.setOnAction(event -> onCommit());

        Button rollbackButton = new Button("Rollback");
        rollbackButton.setId("github-rollback-button");
        rollbackButton.setOnAction(event -> onRollback());

        Button pushButton = new Button("Push");
        pushButton.setId("github-push-button");
        pushButton.setOnAction(event -> viewModel.push());

        Button prButton = new Button("Create PR");
        prButton.setId("github-pr-button");
        prButton.setOnAction(event -> onCreatePullRequest());

        Button tokenButton = new Button("Token");
        tokenButton.setId("github-token-button");
        tokenButton.setOnAction(event -> onToken());

        busyIndicator = new ProgressIndicator();
        busyIndicator.setId("github-busy-indicator");
        busyIndicator.setPrefSize(14, 14);
        busyIndicator.setMaxSize(14, 14);
        busyIndicator.setVisible(false);
        busyIndicator.managedProperty().bind(busyIndicator.visibleProperty());

        statusLabel = new Label();
        statusLabel.setId("github-status-text");

        errorLabel = new Label();
        errorLabel.setId("github-error-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().setAll(
            repoLink,
            createSeparator(),
            dirtyDot,
            branchCombo,
            checkoutButton,
            newBranchButton,
            createSeparator(),
            commitButton,
            rollbackButton,
            pushButton,
            createSeparator(),
            prButton,
            tokenButton,
            spacer,
            busyIndicator,
            statusLabel,
            errorLabel
        );

        dirtyDot.visibleProperty().bind(viewModel.dirtyProperty());
        statusLabel.textProperty().bind(viewModel.statusTextProperty());
        errorLabel.textProperty().bind(viewModel.errorTextProperty());
        busyIndicator.visibleProperty().bind(viewModel.busyProperty());

        commitButton.disableProperty().bind(viewModel.commitDisabledProperty());
        pushButton.disableProperty().bind(viewModel.pushDisabledProperty());
        prButton.disableProperty().bind(viewModel.pullRequestDisabledProperty());
        checkoutButton.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        newBranchButton.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        rollbackButton.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        branchCombo.disableProperty().bind(viewModel.busyProperty().or(viewModel.localAvailableProperty().not()));
        tokenButton.disableProperty().bind(viewModel.busyProperty());

        viewModel.branchesProperty().addListener((obs, oldList, newList) -> syncBranches(newList));
        viewModel.currentBranchProperty().addListener((obs, oldValue, newValue) -> branchCombo.setValue(newValue));

        syncBranches(viewModel.branchesProperty());
        applyTheme(Theme.dark());
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
            force = DirtyCheckoutAlert.confirm(selected);
            if (!force) {
                return;
            }
        }
        viewModel.switchBranch(selected, force);
    }

    private void onCreateBranch() {
        NewBranchDialog dialog = new NewBranchDialog(viewModel.branchesProperty(), viewModel.currentBranchProperty().get());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(result -> viewModel.createBranch(result.name(), result.startPoint()));
    }

    private void onCommit() {
        CommitDialog dialog = new CommitDialog();
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::commit);
    }

    private void onRollback() {
        RollbackDialog dialog = new RollbackDialog(viewModel.isHeadPushed());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::rollback);
    }

    private void onCreatePullRequest() {
        PullRequestDialog dialog = new PullRequestDialog(
            viewModel.currentBranchProperty().get(),
            viewModel.defaultBranchProperty().get()
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
        TokenDialog dialog = new TokenDialog();
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

    private Node createSeparator() {
        Label separator = new Label("|");
        separator.setOpacity(0.55);
        return separator;
    }

    private void applyTheme(Theme theme) {
        Theme resolved = theme == null ? Theme.dark() : theme;
        String background = toHex(resolved.headerBackground());
        String border = toHex(resolved.borderColor());
        String text = toHex(resolved.textColor());
        String accent = toHex(resolved.accentColor());

        setStyle("-fx-background-color: " + background + ";"
            + "-fx-border-color: " + border + ";"
            + "-fx-border-width: 0 0 1 0;");
        statusLabel.setTextFill(Color.web(text));
        dirtyDot.setTextFill(Color.web(accent));
        errorLabel.setTextFill(Color.web("#d9534f"));
    }

    private static String toHex(Paint paint) {
        if (paint instanceof Color color) {
            return String.format("#%02X%02X%02X",
                Math.round((float) (color.getRed() * 255.0)),
                Math.round((float) (color.getGreen() * 255.0)),
                Math.round((float) (color.getBlue() * 255.0)));
        }
        return "#444444";
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
        String remoteUrl = readString(state, "remoteUrl");
        String localClonePath = readString(state, "localClonePath");
        try {
            GitHubRepoContext context = (localClonePath == null || localClonePath.isBlank())
                ? GitHubRepoContext.remoteOnly(new URI(remoteUrl))
                : GitHubRepoContext.of(new URI(remoteUrl), java.nio.file.Path.of(localClonePath));
            return new GitHubToolbar(context);
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
