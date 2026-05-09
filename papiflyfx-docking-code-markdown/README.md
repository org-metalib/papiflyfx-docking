# papiflyfx-docking-code-markdown

Markdown language pack for `papiflyfx-docking-code`.

## Scope

This module owns Markdown syntax highlighting, Markdown fold regions, their tests, and the `LanguageSupportProvider` that registers language id `markdown`.

It intentionally keeps the public lexer package path stable:

- `org.metalib.papifly.fx.code.lexer.MarkdownLexer`
- `org.metalib.papifly.fx.code.folding.MarkdownFoldProvider`

Consumers that imported `MarkdownLexer` directly add this module as a dependency; no import change is required.

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-markdown</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

`papiflyfx-docking-code-markdown` depends on `papiflyfx-docking-code`, so the core editor is pulled in transitively.

## Discovery

The module registers `org.metalib.papifly.fx.code.folding.MarkdownLanguageSupportProvider` through:

```text
META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider
```

When this module is on the classpath, `LanguageSupportRegistry.defaultRegistry()` discovers Markdown support through `ServiceLoader`.

| Language | ID | Aliases | Extensions |
|----------|----|---------|------------|
| Markdown | `markdown` | `md` | `md`, `markdown` |

`TokenType`, `FoldKind`, `CodeEditorTheme`, and token-color routing stay in `papiflyfx-docking-code`.
