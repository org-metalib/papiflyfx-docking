package org.metalib.papifly.fx.github.ui;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import org.metalib.papifly.fx.github.api.GitHubRepoContext;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.git.GitOperationException;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiException;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class GitHubToolbarViewModel implements AutoCloseable {

    private final GitHubRepoContext context;
    private final CredentialStore credentialStore;
    private final GitRepository gitRepository;
    private final GitHubApiService gitHubApiService;
    private final CommandRunner commandRunner;

    private final StringProperty currentBranch;
    private final StringProperty defaultBranch;
    private final BooleanProperty dirty;
    private final BooleanProperty busy;
    private final StringProperty statusText;
    private final StringProperty errorText;
    private final BooleanProperty authenticated;
    private final BooleanProperty localAvailable;
    private final ObjectProperty<CommitInfo> headCommit;
    private final ListProperty<BranchRef> branches;

    private final BooleanBinding commitDisabled;
    private final BooleanBinding pushDisabled;
    private final BooleanBinding pullRequestDisabled;

    public GitHubToolbarViewModel(
        GitHubRepoContext context,
        CredentialStore credentialStore,
        GitRepository gitRepository,
        GitHubApiService gitHubApiService,
        CommandRunner commandRunner
    ) {
        this.context = Objects.requireNonNull(context, "context");
        this.credentialStore = Objects.requireNonNull(credentialStore, "credentialStore");
        this.gitRepository = gitRepository;
        this.gitHubApiService = Objects.requireNonNull(gitHubApiService, "gitHubApiService");
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner");

        this.currentBranch = new SimpleStringProperty("");
        this.defaultBranch = new SimpleStringProperty("main");
        this.dirty = new SimpleBooleanProperty(false);
        this.busy = new SimpleBooleanProperty(false);
        this.statusText = new SimpleStringProperty("Idle");
        this.errorText = new SimpleStringProperty("");
        this.authenticated = new SimpleBooleanProperty(credentialStore.isAuthenticated());
        this.localAvailable = new SimpleBooleanProperty(context.hasLocalClone() && gitRepository != null);
        this.headCommit = new SimpleObjectProperty<>();
        this.branches = new SimpleListProperty<>(FXCollections.observableArrayList());

        this.commitDisabled = busy
            .or(localAvailable.not())
            .or(dirty.not())
            .or(currentBranch.isEqualTo(defaultBranch));
        this.pushDisabled = busy
            .or(localAvailable.not())
            .or(authenticated.not());
        this.pullRequestDisabled = busy
            .or(authenticated.not())
            .or(currentBranch.isEqualTo(defaultBranch));
    }

    public void refresh() {
        runCommand("Refreshing status...", () -> {
            RepoStatus repoStatus = null;
            List<BranchRef> branchRefs = List.of();
            CommitInfo commitInfo = null;
            String resolvedDefault = defaultBranch.get();
            String apiError = "";

            if (gitRepository != null) {
                repoStatus = gitRepository.loadStatus();
                branchRefs = gitRepository.listBranches();
                commitInfo = gitRepository.getHeadCommit();
                if (repoStatus.defaultBranch() != null && !repoStatus.defaultBranch().isBlank()) {
                    resolvedDefault = repoStatus.defaultBranch();
                }
            }

            try {
                String apiDefault = gitHubApiService.fetchDefaultBranch(context.owner(), context.repo());
                if (apiDefault != null && !apiDefault.isBlank()) {
                    resolvedDefault = apiDefault;
                }
            } catch (GitHubApiException ex) {
                apiError = ex.getMessage();
            }

            if ((resolvedDefault == null || resolvedDefault.isBlank()) && gitRepository != null) {
                resolvedDefault = gitRepository.detectDefaultBranch();
            }
            if (resolvedDefault == null || resolvedDefault.isBlank()) {
                resolvedDefault = "main";
            }

            return new RefreshResult(repoStatus, branchRefs, commitInfo, resolvedDefault, apiError);
        }, result -> {
            defaultBranch.set(result.defaultBranch());
            if (result.repoStatus() != null) {
                currentBranch.set(result.repoStatus().currentBranch());
                dirty.set(result.repoStatus().dirty());
            } else {
                currentBranch.set("");
                dirty.set(false);
            }
            branches.setAll(result.branches());
            headCommit.set(result.headCommit());
            authenticated.set(credentialStore.isAuthenticated());
            localAvailable.set(context.hasLocalClone() && gitRepository != null);
            if (result.apiError().isBlank()) {
                errorText.set("");
            } else {
                errorText.set(result.apiError());
            }
            statusText.set("Ready");
        });
    }

    public void switchBranch(String branchName, boolean force) {
        if (!localAvailable.get()) {
            errorText.set("Local repository is unavailable");
            return;
        }
        if (branchName == null || branchName.isBlank()) {
            errorText.set("Branch name is required");
            return;
        }
        runCommand("Checking out " + branchName + "...", () -> {
            gitRepository.checkout(branchName, force);
            return branchName;
        }, value -> {
            statusText.set("Checked out " + value);
            refresh();
        });
    }

    public void createBranch(String branchName, String startPoint) {
        if (!localAvailable.get()) {
            errorText.set("Local repository is unavailable");
            return;
        }
        if (branchName == null || branchName.isBlank()) {
            errorText.set("New branch name is required");
            return;
        }
        runCommand("Creating branch " + branchName + "...", () -> {
            gitRepository.createAndCheckout(branchName, startPoint);
            return branchName;
        }, value -> {
            statusText.set("Created branch " + value);
            refresh();
        });
    }

    public void commit(String message) {
        if (!localAvailable.get()) {
            errorText.set("Local repository is unavailable");
            return;
        }
        if (currentBranch.get().equals(defaultBranch.get())) {
            errorText.set("Committing to default branch is blocked");
            return;
        }
        if (!dirty.get()) {
            errorText.set("Nothing to commit");
            return;
        }
        runCommand("Creating commit...", () -> gitRepository.commitAll(message), commit -> {
            headCommit.set(commit);
            statusText.set("Committed " + commit.shortHash());
            refresh();
        });
    }

    public void rollback(RollbackMode mode) {
        if (!localAvailable.get()) {
            errorText.set("Local repository is unavailable");
            return;
        }
        if (mode != RollbackMode.REVERT && gitRepository.isHeadPushed()) {
            errorText.set("Only revert is allowed for pushed commits");
            return;
        }
        runCommand("Rolling back commit...", () -> {
            gitRepository.rollback(mode);
            return mode;
        }, value -> {
            statusText.set("Rollback complete: " + value);
            refresh();
        });
    }

    public void push() {
        if (!localAvailable.get()) {
            errorText.set("Local repository is unavailable");
            return;
        }
        if (!authenticated.get()) {
            errorText.set("Authentication required");
            return;
        }
        runCommand("Pushing changes...", () -> {
            gitRepository.push("origin");
            return true;
        }, value -> statusText.set("Push completed"));
    }

    public void createPullRequest(PullRequestDraft draft, Consumer<PullRequestResult> onSuccess) {
        if (!authenticated.get()) {
            errorText.set("Authentication required");
            return;
        }
        PullRequestDraft normalized = draft.withDefaults(
            "PR: " + currentBranch.get(),
            currentBranch.get(),
            defaultBranch.get()
        );
        runCommand("Creating pull request...", () ->
                gitHubApiService.createPullRequest(context.owner(), context.repo(), normalized),
            result -> {
                statusText.set("PR #" + result.number() + " created");
                onSuccess.accept(result);
            });
    }

    public void saveToken(String token) {
        if (token == null || token.isBlank()) {
            credentialStore.clearToken();
            authenticated.set(false);
            statusText.set("Token cleared");
            return;
        }
        credentialStore.setToken(token);
        authenticated.set(credentialStore.isAuthenticated());
        errorText.set("");
        statusText.set("Token saved");
    }

    public void publishError(String message) {
        errorText.set(message == null ? "" : message);
    }

    private <T> void runCommand(String busyText, Callable<T> action, Consumer<T> onSuccess) {
        if (busy.get()) {
            return;
        }
        busy.set(true);
        statusText.set(busyText);
        errorText.set("");

        commandRunner.run(action, value -> {
            busy.set(false);
            onSuccess.accept(value);
        }, throwable -> {
            busy.set(false);
            String message = mapError(throwable);
            errorText.set(message);
            statusText.set("Failed");
        });
    }

    private static String mapError(Throwable throwable) {
        if (throwable instanceof GitOperationException gitOperationException) {
            return gitOperationException.getMessage();
        }
        if (throwable instanceof GitHubApiException gitHubApiException) {
            return gitHubApiException.getMessage();
        }
        if (throwable.getCause() != null) {
            return mapError(throwable.getCause());
        }
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? "Operation failed" : message;
    }

    public GitHubRepoContext context() {
        return context;
    }

    public ReadOnlyStringProperty currentBranchProperty() {
        return currentBranch;
    }

    public ReadOnlyStringProperty defaultBranchProperty() {
        return defaultBranch;
    }

    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty;
    }

    public ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    public ReadOnlyStringProperty statusTextProperty() {
        return statusText;
    }

    public ReadOnlyStringProperty errorTextProperty() {
        return errorText;
    }

    public ReadOnlyBooleanProperty authenticatedProperty() {
        return authenticated;
    }

    public ReadOnlyBooleanProperty localAvailableProperty() {
        return localAvailable;
    }

    public ReadOnlyObjectProperty<CommitInfo> headCommitProperty() {
        return headCommit;
    }

    public ReadOnlyListProperty<BranchRef> branchesProperty() {
        return branches;
    }

    public BooleanBinding commitDisabledProperty() {
        return commitDisabled;
    }

    public BooleanBinding pushDisabledProperty() {
        return pushDisabled;
    }

    public BooleanBinding pullRequestDisabledProperty() {
        return pullRequestDisabled;
    }

    public boolean isHeadPushed() {
        if (!localAvailable.get()) {
            return false;
        }
        return gitRepository.isHeadPushed();
    }

    @Override
    public void close() {
        commandRunner.close();
        if (gitRepository != null) {
            gitRepository.close();
        }
    }

    private record RefreshResult(
        RepoStatus repoStatus,
        List<BranchRef> branches,
        CommitInfo headCommit,
        String defaultBranch,
        String apiError
    ) {
    }
}
