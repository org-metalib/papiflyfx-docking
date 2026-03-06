# papiflyfx-docking-github

A GitHub workflow toolbar for PapiflyFX docking applications.

## Features

- Clickable repository link (`owner/repo`)
- Current branch and dirty-state indicator for local clones
- Checkout existing branch
- Create and checkout new branch
- Commit changes with default-branch protection
- Roll back last commit (revert-only when head is pushed)
- Push branch to remote
- Create pull request via GitHub REST API
- PAT authentication with pluggable token stores
- Toolbar mounting wrapper for top/bottom host placement

## Maven dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-github</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick start

```java
GitHubRepoContext context = GitHubRepoContext.of(
    URI.create("https://github.com/org-metalib/papiflyfx-docking"),
    Path.of("/workspace/papiflyfx-docking")
);

GitHubToolbarContribution contribution = new GitHubToolbarContribution(
    context,
    GitHubToolbarContribution.Position.TOP
);

BorderPane root = new BorderPane();
contribution.mount(root);
```

## Remote-only mode

```java
GitHubRepoContext context = GitHubRepoContext.remoteOnly(
    URI.create("https://github.com/org-metalib/papiflyfx-docking")
);

GitHubToolbar toolbar = new GitHubToolbar(context);
```

In remote-only mode, local git actions are disabled and PR/auth actions remain available.

## Persistence adapter

The module includes `GitHubToolbarStateAdapter` and ServiceLoader registration for optional docking content-state restore.

## Run tests

```bash
mvn -pl papiflyfx-docking-github -am -Dtestfx.headless=true test
```
