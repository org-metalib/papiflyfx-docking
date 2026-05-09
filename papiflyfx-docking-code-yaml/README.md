# papiflyfx-docking-code-yaml

YAML language pack for `papiflyfx-docking-code`.

## Scope

This module owns YAML syntax highlighting, YAML fold regions, their tests, and the `LanguageSupportProvider` that registers language id `yaml`.

It intentionally keeps the public lexer package path stable:

- `org.metalib.papifly.fx.code.lexer.YamlLexer`
- `org.metalib.papifly.fx.code.folding.YamlFoldProvider`

Consumers that imported `YamlLexer` directly add this module as a dependency; no import change is required.

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-yaml</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

`papiflyfx-docking-code-yaml` depends on `papiflyfx-docking-code`, so the core editor is pulled in transitively.

## Discovery

The module registers `org.metalib.papifly.fx.code.folding.YamlLanguageSupportProvider` through:

```text
META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider
```

When this module is on the classpath, `LanguageSupportRegistry.defaultRegistry()` discovers YAML support through `ServiceLoader`.

| Language | ID | Aliases | Extensions |
|----------|----|---------|------------|
| YAML | `yaml` | `yml` | `yaml`, `yml` |

`TokenType`, `FoldKind`, `CodeEditorTheme`, and token-color routing stay in `papiflyfx-docking-code`.
