package org.metalib.papifly.fx.github.git;

import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;

import java.util.List;

public interface GitRepository extends AutoCloseable {

    RepoStatus loadStatus();

    List<BranchRef> listBranches();

    void checkout(String branchName, boolean force);

    void createAndCheckout(String branchName, String startPoint);

    CommitInfo commitAll(String message);

    CommitInfo getHeadCommit();

    void rollback(RollbackMode mode);

    void push(String remoteName);

    boolean isHeadPushed();

    String detectDefaultBranch();

    @Override
    void close();
}
