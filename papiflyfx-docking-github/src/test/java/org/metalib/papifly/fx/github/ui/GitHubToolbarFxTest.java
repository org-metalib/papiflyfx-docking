package org.metalib.papifly.fx.github.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class GitHubToolbarFxTest {

    private StackPane root;

    @Start
    void start(Stage stage) {
        root = new StackPane();
        stage.setScene(new Scene(root, 1200, 160));
        stage.show();
    }

    @Test
    void rendersGroupedToolbarAndBadges() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(false, "feature/x", "main", true, false, 2, 1, false));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-repo-group")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-branch-group")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-changes-group")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-remote-group")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-status-group")));
        assertEquals("Local clone", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-mode-badge")).getText()));
        assertEquals("Dirty 1", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-dirty-badge")).getText()));
        assertEquals("Ahead 2", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-ahead-badge")).getText()));
        assertEquals("Behind 1", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-behind-badge")).getText()));
        assertEquals("Token", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-auth-badge")).getText()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void remoteOnlyHidesLocalGroupsAndPushButton() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(true, "", "main", false, true, 0, 0, false));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertFalse(FxTestUtil.callFx(() -> toolbar.lookup("#github-branch-group").isVisible()));
        assertFalse(FxTestUtil.callFx(() -> toolbar.lookup("#github-changes-group").isVisible()));
        assertFalse(FxTestUtil.callFx(() -> toolbar.lookup("#github-push-button").isVisible()));
        assertEquals("Remote only", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-mode-badge")).getText()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void commitDisabledOnDefaultBranchShowsDefaultBadge() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(false, "main", "main", true, true, 0, 0, false));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-commit-button").isDisable()));
        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-default-branch-badge").isVisible()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void pushAndPullRequestDisabledUntilTokenProvided() {
        GitHubToolbarViewModel viewModel = createViewModel(new ToolbarState(false, "feature/x", "main", false, true, 0, 0, false));
        GitHubToolbar toolbar = FxTestUtil.callFx(() -> new GitHubToolbar(viewModel));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-push-button").isDisable()));
        assertTrue(FxTestUtil.callFx(() -> toolbar.lookup("#github-pr-button").isDisable()));
        assertEquals("No token", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-auth-badge")).getText()));

        FxTestUtil.runFx(() -> viewModel.saveToken("token"));

        assertFalse(FxTestUtil.callFx(() -> toolbar.lookup("#github-push-button").isDisable()));
        assertFalse(FxTestUtil.callFx(() -> toolbar.lookup("#github-pr-button").isDisable()));
        assertEquals("Token", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-auth-badge")).getText()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void exportReviewSnapshotsWhenRequested() throws IOException {
        if (!Boolean.getBoolean("papiflyfx.review.snapshots")) {
            return;
        }

        Path snapshotDirectory = resolveSnapshotDirectory();
        Files.createDirectories(snapshotDirectory);

        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-local-dark.png"),
            new ToolbarState(false, "feature/x", "main", true, false, 2, 1, false),
            Theme.dark()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-local-light.png"),
            new ToolbarState(false, "feature/x", "main", true, false, 2, 1, false),
            Theme.light()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-remote-dark.png"),
            new ToolbarState(true, "", "main", false, true, 0, 0, false),
            Theme.dark()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-remote-light.png"),
            new ToolbarState(true, "", "main", false, true, 0, 0, false),
            Theme.light()
        );
    }

    private GitHubToolbar createToolbar(ToolbarState state) {
        return createToolbar(state, null);
    }

    private GitHubToolbar createToolbar(ToolbarState state, Theme initialTheme) {
        GitHubToolbarViewModel viewModel = createViewModel(state);
        return FxTestUtil.callFx(() -> initialTheme == null ? new GitHubToolbar(viewModel) : new GitHubToolbar(viewModel, initialTheme));
    }

    private GitHubToolbarViewModel createViewModel(ToolbarState state) {
        PatCredentialStore store = new PatCredentialStore();
        if (state.authenticated()) {
            store.setToken("token");
        }

        GitHubRepoContext context = state.remoteOnly()
            ? GitHubRepoContext.remoteOnly(URI.create("https://github.com/org/repo"))
            : GitHubRepoContext.of(URI.create("https://github.com/org/repo"), Path.of("."));

        GitRepository repository = state.remoteOnly() ? null : new FakeGitRepository(state);
        GitHubApiService apiService = new FakeGitHubApiService(state.defaultBranch());

        return new GitHubToolbarViewModel(context, store, repository, apiService, new CommandRunner(true));
    }

    private void writeSnapshot(Path path, ToolbarState state, Theme theme) throws IOException {
        GitHubToolbar toolbar = createToolbar(state, theme);
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        WritableImage image = FxTestUtil.callFx(() -> toolbar.snapshot(null, null));
        writeImage(path, image);

        FxTestUtil.runFx(() -> {
            root.getChildren().clear();
            toolbar.close();
        });
    }

    private static void writeImage(Path path, WritableImage image) throws IOException {
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }
        ImageIO.write(bufferedImage, "png", path.toFile());
    }

    private static Path resolveSnapshotDirectory() {
        String configured = System.getProperty("papiflyfx.review.snapshotDir");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path base = Path.of("").toAbsolutePath().normalize();
        Path repoRoot = "papiflyfx-docking-github".equals(base.getFileName().toString()) ? base.getParent() : base;
        return repoRoot.resolve("spec/papiflyfx-docking-github/review0-color-theme");
    }

    private record ToolbarState(
        boolean remoteOnly,
        String currentBranch,
        String defaultBranch,
        boolean authenticated,
        boolean clean,
        int aheadCount,
        int behindCount,
        boolean detachedHead
    ) {
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

        private final ToolbarState state;

        private FakeGitRepository(ToolbarState state) {
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
                state.clean() ? Set.of() : Set.of("file.txt"),
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
                new BranchRef("feature/x", "refs/heads/feature/x", true, false, "feature/x".equals(state.currentBranch()))
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
            return state.defaultBranch();
        }

        @Override
        public void close() {
        }
    }
}
