package org.metalib.papifly.fx.github.ui;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.github.api.GitHubRepoContext;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubToolbarViewModelTest {

    @Test
    void remoteOnlyModeDisablesLocalActions() {
        PatCredentialStore store = new PatCredentialStore();
        GitHubRepoContext context = GitHubRepoContext.remoteOnly(URI.create("https://github.com/org/repo"));
        FakeGitHubApiService api = new FakeGitHubApiService();

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            null,
            api,
            new CommandRunner(true)
        )) {
            viewModel.refresh();

            assertFalse(viewModel.localAvailableProperty().get());
            assertTrue(viewModel.commitDisabledProperty().get());
            assertTrue(viewModel.pushDisabledProperty().get());
        }
    }

    @Test
    void commitDisabledOnDefaultBranch() {
        PatCredentialStore store = new PatCredentialStore();
        store.setToken("token");
        GitHubRepoContext context = GitHubRepoContext.of(
            URI.create("https://github.com/org/repo"),
            Path.of(".")
        );

        FakeGitRepository repository = new FakeGitRepository();
        repository.status = new RepoStatus(
            "main",
            "main",
            false,
            0,
            0,
            Set.of("A"),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of()
        );

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            repository,
            new FakeGitHubApiService(),
            new CommandRunner(true)
        )) {
            viewModel.refresh();
            assertTrue(viewModel.commitDisabledProperty().get());

            repository.status = new RepoStatus(
                "feature/x",
                "main",
                false,
                0,
                0,
                Set.of("A"),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
            );
            viewModel.refresh();

            assertFalse(viewModel.commitDisabledProperty().get());
        }
    }

    @Test
    void pushAndPullRequestRequireToken() {
        PatCredentialStore store = new PatCredentialStore();
        GitHubRepoContext context = GitHubRepoContext.of(
            URI.create("https://github.com/org/repo"),
            Path.of(".")
        );

        FakeGitRepository repository = new FakeGitRepository();
        repository.status = new RepoStatus(
            "feature/x",
            "main",
            false,
            0,
            0,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of()
        );

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            repository,
            new FakeGitHubApiService(),
            new CommandRunner(true)
        )) {
            viewModel.refresh();

            assertTrue(viewModel.pushDisabledProperty().get());
            assertTrue(viewModel.pullRequestDisabledProperty().get());

            viewModel.saveToken("token");

            assertFalse(viewModel.pushDisabledProperty().get());
            assertFalse(viewModel.pullRequestDisabledProperty().get());
        }
    }

    private static final class FakeGitHubApiService extends GitHubApiService {
        @Override
        public String fetchDefaultBranch(String owner, String repo) {
            return "main";
        }

        @Override
        public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
            return new PullRequestResult(1, URI.create("https://github.com/org/repo/pull/1"));
        }
    }

    private static final class FakeGitRepository implements GitRepository {

        private RepoStatus status = new RepoStatus(
            "main",
            "main",
            false,
            0,
            0,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of()
        );

        @Override
        public RepoStatus loadStatus() {
            return status;
        }

        @Override
        public List<BranchRef> listBranches() {
            return List.of(
                new BranchRef("main", "refs/heads/main", true, false, "main".equals(status.currentBranch())),
                new BranchRef("feature/x", "refs/heads/feature/x", true, false, "feature/x".equals(status.currentBranch()))
            );
        }

        @Override
        public void checkout(String branchName, boolean force) {
            status = new RepoStatus(
                branchName,
                status.defaultBranch(),
                false,
                0,
                0,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
            );
        }

        @Override
        public void createAndCheckout(String branchName, String startPoint) {
            checkout(branchName, false);
        }

        @Override
        public CommitInfo commitAll(String message) {
            return new CommitInfo("abcdef123456", "abcdef1", message, "tester", Instant.now());
        }

        @Override
        public CommitInfo getHeadCommit() {
            return new CommitInfo("abcdef123456", "abcdef1", "head", "tester", Instant.now());
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
            return status.defaultBranch();
        }

        @Override
        public void close() {
        }
    }
}
