package org.metalib.papifly.fx.github.ui;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.github.FxTestUtil;
import org.metalib.papifly.fx.github.api.GitHubRepoContext;
import org.metalib.papifly.fx.github.api.GitHubToolbar;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class GitHubToolbarFxTest {

    private StackPane root;

    @Start
    void start(Stage stage) {
        root = new StackPane();
        stage.setScene(new Scene(root, 1000, 120));
        stage.show();
    }

    @Test
    void rendersExpectedControls() {
        GitHubToolbar toolbar = createToolbar(false, "feature/x", "main", true, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-repo-link")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-branch-combo")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-commit-button")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-push-button")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-pr-button")));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void remoteOnlyDisablesLocalActions() {
        GitHubToolbar toolbar = createToolbar(true, "", "main", false, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-commit-button").isDisable()));
        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-checkout-button").isDisable()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void commitDisabledOnDefaultBranch() {
        GitHubToolbar toolbar = createToolbar(false, "main", "main", true, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-commit-button").isDisable()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void pushAndPullRequestDisabledUntilTokenProvided() {
        GitHubToolbarViewModel viewModel = createViewModel(false, "feature/x", "main", false, false);
        GitHubToolbar toolbar = FxTestUtil.callFx(() -> new GitHubToolbar(viewModel));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-push-button").isDisable()));
        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-pr-button").isDisable()));

        FxTestUtil.runFx(() -> viewModel.saveToken("token"));

        assertFalse(FxTestUtil.callFx(() -> toolbar.lookup("#github-push-button").isDisable()));
        assertFalse(FxTestUtil.callFx(() -> toolbar.lookup("#github-pr-button").isDisable()));

        FxTestUtil.runFx(toolbar::close);
    }

    private GitHubToolbar createToolbar(
        boolean remoteOnly,
        String currentBranch,
        String defaultBranch,
        boolean authenticated,
        boolean clean
    ) {
        GitHubToolbarViewModel viewModel = createViewModel(remoteOnly, currentBranch, defaultBranch, authenticated, clean);
        return FxTestUtil.callFx(() -> new GitHubToolbar(viewModel));
    }

    private GitHubToolbarViewModel createViewModel(
        boolean remoteOnly,
        String currentBranch,
        String defaultBranch,
        boolean authenticated,
        boolean clean
    ) {
        PatCredentialStore store = new PatCredentialStore();
        if (authenticated) {
            store.setToken("token");
        }

        GitHubRepoContext context = remoteOnly
            ? GitHubRepoContext.remoteOnly(URI.create("https://github.com/org/repo"))
            : GitHubRepoContext.of(URI.create("https://github.com/org/repo"), Path.of("."));

        GitRepository repository = remoteOnly ? null : new FakeGitRepository(currentBranch, defaultBranch, clean);
        GitHubApiService apiService = new FakeGitHubApiService(defaultBranch);

        return new GitHubToolbarViewModel(context, store, repository, apiService, new CommandRunner(true));
    }

    private static final class FakeGitHubApiService extends GitHubApiService {

        private final String defaultBranch;

        private FakeGitHubApiService(String defaultBranch) {
            this.defaultBranch = defaultBranch;
        }

        @Override
        public String fetchDefaultBranch(String owner, String repo) {
            return defaultBranch;
        }

        @Override
        public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
            return new PullRequestResult(7, URI.create("https://github.com/org/repo/pull/7"));
        }
    }

    private static final class FakeGitRepository implements GitRepository {

        private String currentBranch;
        private final String defaultBranch;
        private final boolean clean;

        private FakeGitRepository(String currentBranch, String defaultBranch, boolean clean) {
            this.currentBranch = currentBranch;
            this.defaultBranch = defaultBranch;
            this.clean = clean;
        }

        @Override
        public RepoStatus loadStatus() {
            return new RepoStatus(
                currentBranch,
                defaultBranch,
                false,
                0,
                0,
                clean ? Set.of() : Set.of("file.txt"),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
            );
        }

        @Override
        public List<BranchRef> listBranches() {
            return List.of(
                new BranchRef(defaultBranch, "refs/heads/" + defaultBranch, true, false, defaultBranch.equals(currentBranch)),
                new BranchRef("feature/x", "refs/heads/feature/x", true, false, "feature/x".equals(currentBranch))
            );
        }

        @Override
        public void checkout(String branchName, boolean force) {
            this.currentBranch = branchName;
        }

        @Override
        public void createAndCheckout(String branchName, String startPoint) {
            this.currentBranch = branchName;
        }

        @Override
        public CommitInfo commitAll(String message) {
            return new CommitInfo("abcdef123", "abcdef1", message, "tester", Instant.now());
        }

        @Override
        public CommitInfo getHeadCommit() {
            return new CommitInfo("abcdef123", "abcdef1", "head", "tester", Instant.now());
        }

        @Override
        public void rollback(RollbackMode mode) {
        }

        @Override
        public void push(String remoteName) {
        }

        @Override
        public boolean isHeadPushed() {
            return false;
        }

        @Override
        public String detectDefaultBranch() {
            return defaultBranch;
        }

        @Override
        public void close() {
        }
    }
}
