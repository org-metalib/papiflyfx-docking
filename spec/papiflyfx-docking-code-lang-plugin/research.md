# papiflyfx-docking-code language plugin research

## 1) Goal and context

The current editor supports a small built-in language set (`java`, `json`, `javascript`, `markdown`, `plain-text`) 
and requires manual language selection via `CodeEditor#setLanguageId(...)`.

This research focuses on:

1. Understanding the current `papiflyfx-docking-code` architecture deeply.
2. Identifying where language support is hardcoded.
3. Proposing a pluggable architecture that lets external modules add language support safely and predictably.

---

## 2) What was analyzed

I reviewed the code module end-to-end with emphasis on language-related runtime flow:

- `api`: `CodeEditor`, `EditorLifecycleService`, `EditorStateCoordinator`, factory/adapter integration.
- `lexer`: contracts, registries, incremental engine/pipeline, built-in lexers.
- `folding`: providers, registry, incremental folding pipeline, fold model.
- `render`: `TextPass` token color mapping and theme coupling.
- `state`: language persistence in `EditorStateData`/`EditorStateCodec`.
- Tests: lexer/folding pipeline and integration coverage.
- Samples: language assignment usage patterns.

---

## 3) Deep architecture findings (current state)

## 3.1 Editor orchestration

`CodeEditor` is the composition root for language behavior:

- Stores `languageId` as a `StringProperty` (default `"plain-text"`).
- On language change, updates both:
  - `IncrementalLexerPipeline#setLanguageId(...)`
  - `IncrementalFoldingPipeline#setLanguageId(...)`
- Persists `languageId` through `EditorStateData` and `EditorStateCodec`.

Important detail: `CodeEditor#setLanguageId(...)` stores the provided string (except null/blank fallback). Normalization
to lowercase happens later in registries/pipelines.

## 3.2 Lexing pipeline design

Lexing is already architected for performance:

- `IncrementalLexerEngine`:
  - line-based lexing with `LexState` propagation,
  - dirty-start incremental recompute,
  - unchanged-suffix reuse.
- `IncrementalLexerPipeline`:
  - debounced async worker,
  - revision gating (drops stale results),
  - lazy text snapshot strategy,
  - exception handling with plain-text fallback.

This is strong foundation for pluggable languages: extension should reuse this pipeline rather than replacing it.

## 3.3 Folding pipeline design

Folding mirrors lexing:

- `IncrementalFoldingPipeline` has the same debounce/revision pattern.
- Uses `FoldProvider` per language.
- Applies `FoldMap` into `Viewport` and `GutterView`.
- On provider failure, falls back to empty fold map.

This means pluggable language support must treat lexer + fold provider as one logical unit.

## 3.4 Hardcoded registration points (core limitation)

Language resolution is currently static and closed:

- `LexerRegistry`: immutable static map of language ID -> `Lexer`.
- `FoldProviderRegistry`: immutable static map of language ID -> `FoldProvider`.
- Both have separate alias tables and separate normalization logic.

There is no:

- runtime registration API,
- SPI discovery for lexers/fold providers,
- language metadata (display name, file extensions, aliases as first-class data),
- unified language descriptor tying lexer + folding together.

## 3.5 Token rendering constraints

Syntax rendering is based on fixed enum categories:

- `TokenType` is a closed enum.
- `TextPass` maps `TokenType` to `CodeEditorTheme` colors via `switch`.

Implication: external language plugins can only express semantics representable by existing token categories unless 
token/theming model is extended later.

## 3.6 State and compatibility behavior

Language persistence behavior is good for plugin evolution:

- `languageId` is persisted as string in editor state.
- Unknown IDs safely degrade to plain-text behavior through registry fallback.

This provides a safe compatibility path if plugins are missing at restore time.

---

## 4) Why language support is not truly pluggable today

Even though `Lexer` and `FoldProvider` are interfaces, practical extensibility is blocked because:

1. Registries are static, immutable, and non-extensible.
2. Default `CodeEditor` constructor does not expose resolver injection publicly.
3. Pipeline custom resolver constructors are package-private (test-oriented seam, not public extension API).
4. Language metadata/discovery does not exist.
5. Lexer and folding resolution are duplicated and can drift.

So, extension is possible only by modifying core module code directly.

---

## 5) Design goals for a plugin architecture

To scale language support safely, the architecture should provide:

1. **Unified language definition**: one descriptor for lexer + folding + metadata.
2. **Deterministic fallback**: missing/failing plugin must degrade to plain text without breaking editor.
3. **Compatibility is secondary**: allow ID/API cleanup when it materially improves the plugin model.
4. **Discoverability**: support `ServiceLoader` for zero/low-configuration plugins.
5. **Manual control**: allow explicit registration/unregistration for tests and host apps.
6. **Constraint relaxation**: prefer a clean single API over long-lived legacy facades.
7. **Performance safety**: no changes that break incremental lex/fold architecture.
8. **Boot-time policy control**: allow built-ins to be enabled, disabled, or explicitly replaced by host apps.

---

## 6) Recommended solution: unified language support registry + SPI

## 6.1 New core concepts

Introduce a new package, e.g. `org.metalib.papifly.fx.code.language`, with:

### `LanguageSupport` descriptor

```java
public record LanguageSupport(
    String id,
    String displayName,
    Set<String> aliases,
    Set<String> fileExtensions,
    java.util.function.Supplier<Lexer> lexerFactory,
    java.util.function.Supplier<FoldProvider> foldProviderFactory
) {}
```

Notes:

- `foldProviderFactory` may be optional; default to plain/no-fold provider.
- aliases and extensions are normalized internally.

### `LanguageSupportProvider` SPI

```java
public interface LanguageSupportProvider {
    Collection<LanguageSupport> getLanguageSupports();
}
```

External plugin JARs implement this and publish via `META-INF/services`.

### `LanguageSupportRegistry`

Thread-safe registry responsible for:

- `register(LanguageSupport support)`
- `register(LanguageSupport support, RegistrationMode mode)` (`REJECT_ON_CONFLICT`/`REPLACE`)
- `registerAll(Collection<LanguageSupport>)`
- `unregister(String id)` (optional, mostly test/dev)
- `bootstrap(BootstrapOptions options)` (`includeBuiltIns`, `loadServiceProviders`)
- `resolveLexer(String languageId)` -> fallback to plain text lexer
- `resolveFoldProvider(String languageId)` -> fallback to plain fold provider
- `normalizeLanguageId(String id)`
- `detectLanguageId(String fileNameOrPath)` (extension-based)
- `availableLanguages()`
- `loadFromServiceLoader(ClassLoader)`

---

## 6.2 Prefer clean API over compatibility shims

Use `LanguageSupportRegistry` as the single public language resolution surface:

- `IncrementalLexerPipeline` and `IncrementalFoldingPipeline` resolve directly via `LanguageSupportRegistry`.
- Retire `LexerRegistry` and `FoldProviderRegistry` (or keep only short-lived deprecated wrappers during migration).
- Keep normalization/alias logic only in `LanguageSupportRegistry` and remove duplicate paths.

This intentionally allows backward-incompatible API cleanup to eliminate duplicated resolution logic.

---

## 6.3 Built-ins become optional boot profile, not hardcoded maps

Move built-in language definitions into `BuiltInLanguageSupportProvider`:

- `plain-text`
- `java`
- `json`
- `javascript` (+ `js`)
- `markdown` (+ `md`)

Boot policy should be explicit:

- Default bootstrap: include built-ins + optionally load external providers using `ServiceLoader`.
- Minimal bootstrap: do **not** include built-ins at startup (`includeBuiltIns=false`), then register only host/plugin-provided languages.
- Replacement: allow host/plugins to override built-ins via registry conflict policy (strict or permissive, including last-registration-wins).

Implementation note: keep an internal non-registered plain-text fallback instance for runtime safety, even when built-ins are disabled.

---

## 6.4 Optional public API enhancements on `CodeEditor`

Current manual language assignment should stay unchanged.

Add optional convenience:

- `public void detectLanguageFromFilePath()`  
  Uses registry extension mapping and sets `languageId` if match.
- `public void setAutoDetectLanguage(boolean enabled)` (default false to avoid behavior surprises).

This enables better UX for opened files while preserving existing semantics.

---

## 6.5 Plugin packaging example

External plugin module:

```java
public final class PythonLanguageSupportProvider implements LanguageSupportProvider {
    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(
            new LanguageSupport(
                "python",
                "Python",
                Set.of("py", "python3"),
                Set.of("py", "pyw"),
                PythonLexer::new,
                PythonFoldProvider::new
            )
        );
    }
}
```

`META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`:

```text
com.example.papiflyfx.code.python.PythonLanguageSupportProvider
```

Host app usage (if explicit load is preferred):

```java
LanguageSupportRegistry.defaultRegistry().bootstrap(
    new BootstrapOptions(true, true) // includeBuiltIns, loadServiceProviders
);
```

Then existing `editor.setLanguageId("python")` works.

Host app usage (without built-ins at boot):

```java
LanguageSupportRegistry.defaultRegistry().bootstrap(
    new BootstrapOptions(false, true) // no built-ins
);
```

---

## 7) Migration plan (compatibility-relaxed)

## Phase 1: Infrastructure

1. Add language package (`LanguageSupport`, provider SPI, registry).
2. Extract built-ins into provider + add bootstrap options (`includeBuiltIns` / `loadServiceProviders`).
3. Switch pipelines to the new registry directly and remove legacy registry dependencies.
4. Add unit tests for normalize/resolve/fallback/alias collisions.

## Phase 2: Discovery and host registration

1. Add `ServiceLoader` support.
2. Decide policy:
   - strict profile (reject conflicts), or
   - permissive profile (replacement/priority rules allowed).
3. Add tests using test-only provider classes and document intentional breaking changes.

## Phase 3: UX niceties

1. Add extension-based language detection API.
2. (Optional) wire auto-detect for `setFilePath(...)` behind explicit toggle.
3. Update samples/docs.

## Phase 4 (optional advanced): richer token scopes

If needed for better highlighting quality:

- extend token model with optional semantic scope string (`"entity.name.function"`, etc.),
- keep `TokenType` only as a temporary bridge if needed,
- use scope->color map fallbacking to `TokenType`.

This should be a separate change set because it touches rendering/theme APIs.

---

## 8) Key risks and mitigations

## Risk 1: ID/alias collisions between plugins

Mitigation:

- normalize all IDs/aliases centrally,
- make conflict handling policy-driven (strict or permissive),
- emit clear diagnostics when replacement happens.

## Risk 2: plugin quality/performance

Mitigation:

- document lexer/fold provider contracts (pure, deterministic, no heavy I/O),
- keep async debounce/revision gating unchanged,
- preserve failure fallback behavior.

## Risk 3: mismatch between lexer and folding coverage

Mitigation:

- single `LanguageSupport` descriptor owns both components,
- fallback to plain folding provider when absent.

## Risk 4: restore behavior when plugin unavailable

Mitigation:

- keep persisted `languageId` string,
- resolve unknown IDs to plain-text safely (already current behavior).

---

## 9) Test strategy for the plugin architecture

1. **Registry tests**
   - registration success/failure rules,
   - alias normalization,
   - collision handling,
   - extension detection,
   - fallback correctness.

2. **ServiceLoader tests**
   - provider auto-discovery,
   - multiple provider aggregation.

3. **Pipeline integration tests**
   - custom plugin language selected through `setLanguageId`,
   - token/fold updates visible in viewport,
   - error in plugin lexer/provider falls back correctly.

4. **Persistence tests**
   - saved plugin `languageId` round-trips,
   - restore without plugin degrades to plain text.

5. **Migration/breaking-change tests**
   - validate new bootstrap/conflict policies and explicitly cover removed/deprecated legacy APIs.

---

## 10) Alternatives considered

## A) Add `register()` directly to existing registries only

Pros: tiny change.  
Cons: keeps duplicated lexer/fold registration and no metadata/SPI; error-prone long-term.

## B) Full parser/AST plugin system now

Pros: highest semantic quality.  
Cons: too large/risky for current architecture and scope.

## C) Recommended middle path (chosen)

Unified language descriptor + registry + SPI discovery, with intentional simplification of legacy APIs and preserved incremental lex/fold runtime design.

---

## 11) Final recommendation

Implement pluggable language support by introducing a **unified `LanguageSupportRegistry` + `LanguageSupportProvider` 
SPI** with **boot-time profile control** (include/omit built-ins, conflict-policy-based replacement), and move pipelines to this API directly.

This is the smallest architecture change that:

- removes hardcoded language limits,
- permits deliberate backward-incompatible cleanup where it reduces complexity,
- supports external language plugins cleanly,
- and sets up a safe path for future richer syntax theming.
