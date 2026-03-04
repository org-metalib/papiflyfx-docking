# papiflyfx-docking-hugo

A dockable Hugo preview content type for PapiflyFX docking.
It starts `hugo server`, renders pages inside JavaFX `WebView`, supports session persistence, and follows docking theme updates.

## Features

- Internal preview rendering via JavaFX `WebView`/`WebEngine`
- Hugo CLI preflight check (`hugo version`)
- Managed `hugo server` lifecycle (start/stop/cleanup)
- Toolbar actions: start, stop, back, forward, reload, open in browser
- External navigation guard (keeps embedded view on local Hugo origin)
- Docking persistence via `ContentStateAdapter` + `LeafContentData`
- Theme binding with live updates (`Theme.dark()` / `Theme.light()`)

## Requirements

- Java 25
- JavaFX 23.0.1
- Hugo CLI installed and available on `PATH`

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-hugo</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick Start

### 1. Register adapter and factory

```java
DockManager dockManager = new DockManager();

ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new HugoPreviewStateAdapter());
dockManager.setContentStateRegistry(registry);

dockManager.setContentFactory(new HugoPreviewFactory(Path.of("/workspace/my-hugo-site")));
```

### 2. Create preview content

```java
HugoPreviewPane preview = new HugoPreviewPane(new HugoPreviewConfig(
    Path.of("/workspace/my-hugo-site"),
    "hugo:docs",
    "/",
    1313,
    true,
    false
));

DockLeaf leaf = dockManager.createLeaf("Docs Preview", preview);
leaf.setContentFactoryId(HugoPreviewFactory.FACTORY_ID);
leaf.setContentData(LeafContentData.of(
    HugoPreviewFactory.FACTORY_ID,
    "hugo:docs",
    HugoPreviewStateAdapter.VERSION
));
```

### 3. Bind docking theme

```java
preview.bindThemeProperty(dockManager.themeProperty());
```

## Persisted State

`HugoPreviewStateAdapter` persists the following keys:

- `siteDir`
- `relativePath`
- `drafts`

State codec: `HugoPreviewStateCodec`

## Main APIs

| Type | Purpose |
|------|---------|
| `HugoPreviewPane` | Main dock content (`DisposableContent`) |
| `HugoPreviewConfig` | Startup configuration (site root, path, port, behavior) |
| `HugoPreviewFactory` | Docking `ContentFactory` integration |
| `HugoPreviewStateAdapter` | Save/restore integration for sessions |
| `HugoServerProcessManager` | Hugo process lifecycle + readiness handling |
| `HugoCliProbe` | Hugo availability/version probing |

## Run Tests

```bash
mvn -pl papiflyfx-docking-hugo test
```

## Headless UI Tests

```bash
mvn -pl papiflyfx-docking-hugo -Dtestfx.headless=true test
```
