# papiflyfx-docking-settings

The dockable settings shell for PapiflyFX applications. This module provides the settings panel UI, JSON settings persistence, secure secret storage implementations, built-in categories, and docking integration.

## Features

- two-pane settings panel with search, scope switching, and page actions
- ServiceLoader discovery for built-in and module-contributed settings categories
- JSON persistence for application, workspace, and session settings
- secure secret storage with platform-aware backends and encrypted-file fallback
- built-in settings pages for appearance, workspace, security, profiles, network, AI models, MCP servers, and keyboard shortcuts
- docking integration through `SettingsContentFactory` and `SettingsStateAdapter`

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-settings</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Runtime

Applications normally create one `SettingsRuntime`, bind it to the application theme property, and use `SettingsContentFactory` to expose the settings panel as dockable content.

## SPI Boundary

Content modules should depend on `papiflyfx-docking-settings-api` only. This implementation module stays on the application/runtime side.
