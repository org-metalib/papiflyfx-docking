package org.metalib.papifly.fx.samples.github;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
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
import org.metalib.papifly.fx.github.ui.CommandRunner;
import org.metalib.papifly.fx.github.ui.GitHubToolbarViewModel;
import org.metalib.papifly.fx.samples.SampleScene;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GitHubToolbarSample implements SampleScene {

    @Override
    public String category() {
        return "GitHub";
    }

    @Override
    public String title() {
        return "GitHub Toolbar";
    }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        GitHubToolbar localToolbar = createToolbar(themeProperty.get(), sampleLocalState());
        GitHubToolbar remoteToolbar = createToolbar(themeProperty.get(), sampleRemoteState());
        localToolbar.setMaxWidth(Double.MAX_VALUE);
        remoteToolbar.setMaxWidth(Double.MAX_VALUE);
        localToolbar.bindThemeProperty(themeProperty);
        remoteToolbar.bindThemeProperty(themeProperty);

        VBox toolbarShowcase = new VBox(
            10,
            createSection("Local clone workflow", localToolbar),
            createSection("Remote-only workflow", remoteToolbar)
        );
        toolbarShowcase.setFillWidth(true);
        toolbarShowcase.setMaxWidth(Double.MAX_VALUE);
        toolbarShowcase.setPadding(new Insets(16));
        toolbarShowcase.setPrefSize(1180, 180);
        return toolbarShowcase;
    }

    private static VBox createSection(String title, Node toolbar) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");

        VBox section = new VBox(4, titleLabel, toolbar);
        section.setFillWidth(true);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private static GitHubToolbar createToolbar(Theme initialTheme, SampleState state) {
        PatCredentialStore store = new PatCredentialStore();
        if (state.authenticated()) {
            store.setToken("sample-token");
        }

        GitHubRepoContext context = state.remoteOnly()
            ? GitHubRepoContext.remoteOnly(URI.create("https://github.com/org-metalib/papiflyfx-docking"))
            : GitHubRepoContext.of(
                URI.create("https://github.com/org-metalib/papiflyfx-docking"),
                Path.of(".")
            );

        GitRepository repository = state.remoteOnly() ? null : new SampleGitRepository(state);
        GitHubApiService apiService = new SampleGitHubApiService(state.defaultBranch());
        GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            repository,
            apiService,
            new CommandRunner(true)
        );
        return new GitHubToolbar(viewModel, initialTheme);
    }

    private static SampleState sampleLocalState() {
        return new SampleState(false, "feature/theme-pass", "main", true, 2, 1, 3, false);
    }

    private static SampleState sampleRemoteState() {
        return new SampleState(true, "", "main", false, 0, 0, 0, false);
    }

    private static Set<String> entries(String prefix, int count) {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        for (int index = 1; index <= count; index++) {
            entries.add(prefix + index + ".java");
        }
        return entries;
    }

    private record SampleState(
        boolean remoteOnly,
        String currentBranch,
        String defaultBranch,
        boolean authenticated,
        int aheadCount,
        int behindCount,
        int dirtyCount,
        boolean detachedHead
    ) {
    }

    private static final class SampleGitHubApiService extends GitHubApiService {

        private final String defaultBranch;

        private SampleGitHubApiService(String defaultBranch) {
            this.defaultBranch = defaultBranch;
        }

        @Override
        public String fetchDefaultBranch(String owner, String repo) {
            return defaultBranch;
        }

        @Override
        public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
            return new PullRequestResult(18, URI.create("https://github.com/org-metalib/papiflyfx-docking/pull/18"));
        }
    }

    private static final class SampleGitRepository implements GitRepository {

        private final SampleState state;

        private SampleGitRepository(SampleState state) {
            this.state = state;
        }

        @Override
        public RepoStatus loadStatus() {
            return new RepoStatus(
                state.currentBranch(),
                state.defaultBranch(),
                state.detachedHead(),
                state.aheadCount(),
                state.behindCount(),
                entries("added-", state.dirtyCount()),
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
                new BranchRef(state.defaultBranch(), "refs/heads/" + state.defaultBranch(), true, false,
                    state.defaultBranch().equals(state.currentBranch())),
                new BranchRef("feature/theme-pass", "refs/heads/feature/theme-pass", true, false,
                    "feature/theme-pass".equals(state.currentBranch())),
                new BranchRef("release/0.0.x", "refs/heads/release/0.0.x", true, false,
                    "release/0.0.x".equals(state.currentBranch()))
            );
        }

        @Override
        public void checkout(String branchName, boolean force) {
        }

        @Override
        public void createAndCheckout(String branchName, String startPoint) {
        }

        @Override
        public CommitInfo commitAll(String message) {
            return new CommitInfo("abcdef123456", "abcdef1", message, "sample", Instant.now());
        }

        @Override
        public CommitInfo getHeadCommit() {
            return new CommitInfo("abcdef123456", "abcdef1", "Sample commit", "sample", Instant.now());
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
            return state.defaultBranch();
        }

        @Override
        public void close() {
        }
    }
}
