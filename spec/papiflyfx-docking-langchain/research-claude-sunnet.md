# PapiflyFX Docking — LangChain4j Integration (Claude Sonnet)

## Use Cases

### UC-1: Dockable AI Chat Panel
A first-class dockable panel backed by LangChain4j `AiServices`. Supports multi-turn conversation
via `MessageWindowChatMemory`. Streams tokens in real time. Floatable, minimizable, splittable
like any other dock leaf. Session state (history, scroll offset, provider config) survives restart.

### UC-2: Inline Code Completions in CodeEditor
While the user types, a debounced background request sends the surrounding context window to the
LLM and shows ghost-text after the caret. Tab to accept, Esc to dismiss. Cancel in-flight requests
on new keystrokes. Toggle on/off via editor toolbar.

### UC-3: Explain & Refactor Selected Code
Ctrl+Shift+E explains a selection; Ctrl+Shift+R proposes a refactored version shown as a
side-by-side diff popup. Both actions read from `CodeEditor.selectionModel` and write back via
`EditorEditController`.

### UC-4: AI-Generated Commit Messages
In `papiflyfx-docking-github`, a "✨ Generate" button reads the staged diff via JGit and calls
the LLM with a Conventional Commits system prompt. The result pre-fills the commit message field.

### UC-5: AI-Generated PR Title & Body
During PR creation, optionally invoke the LLM with `git log base..HEAD` + full diff to draft the
PR title and description. User reviews before submission.

### UC-6: AI Code Review as Gutter Annotations
A "Review" action in the GitHub toolbar sends the branch diff to the LLM. Findings are parsed and
injected into `MarkerModel` so they appear as colored gutter markers navigable via keyboard in
`CodeEditor`.

### UC-7: Project-Level RAG Q&A Panel
Index source files, `spec/` docs, and READMEs into an `InMemoryEmbeddingStore` (swappable to
persistent store). A dockable Q&A panel answers natural-language questions about the codebase and
cites source file + line range in each answer.

### UC-8: Natural-Language Code Search
A new mode in `SearchController` sends a natural-language query to the embedding store and shows
semantically relevant code fragments in the existing search results list. Clicking a result
navigates the editor to that file and line.

### UC-9: Hugo AI Draft & Expand
In `papiflyfx-docking-hugo`, an "AI Draft" toolbar button accepts a user prompt and generates
Hugo-flavored markdown in a new `CodeEditor` tab. "Expand Selection" elaborates on highlighted
text via streaming replacement.

### UC-10: Multi-Provider Settings Panel
A dockable settings panel for selecting the LLM provider (OpenAI, Anthropic, Ollama, Gemini),
entering credentials, and tuning model parameters. Config persisted via the docking session;
API keys stored separately (same pattern as GitHub PAT storage). A live health-check indicator
confirms connectivity on save.

### UC-11: LangChain4j Tools — IDE Actions
The chat AI can call docking-aware `@Tool` methods: open a file in the editor, read file content,
list project files, run semantic search. Tool invocations are shown as collapsible blocks in the
chat panel so the user follows what the AI is doing.

### UC-12: Background Indexing with Incremental Re-index
A background service monitors the project with `WatchService`, re-embeds only changed files, and
updates the embedding store. A status indicator in the dock bottom bar shows indexing progress.

---

## Tasks

### Phase 0 — Maven Module Scaffold

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 0.1 | Create `papiflyfx-docking-langchain` module | New Maven module. Package `org.metalib.papifly.fx.langchain`. Compile dep on `papiflyfx-docking-api`; test dep on `papiflyfx-docking-docks`. Add to parent `<modules>` list. | — |
| 0.2 | Add LangChain4j BOM to parent pom | Add `langchain4j-bom` to `<dependencyManagement>`. Default to latest stable (≥ 1.0). Add `langchain4j-open-ai` in the new module. | — |
| 0.3 | Define `LlmService` interface | `ChatResponse chat(String prompt)`, `TokenStream chatStreaming(String prompt)`, `void reconfigure(LlmConfig config)`. Kept in the langchain module's public API package. | 0.1 |
| 0.4 | Define `LlmConfig` record | Immutable: provider enum, apiKey, modelName, baseUrl, temperature, maxTokens. Serialize/deserialize via existing Map-based JSON helpers. | 0.1 |
| 0.5 | Implement `DefaultLlmService` | Wraps LangChain4j `ChatModel` + `StreamingChatModel`. Builds the correct provider from `LlmConfig`. Holds a `MessageWindowChatMemory` (configurable window). Thread-safe reconfiguration. | 0.3, 0.4 |
| 0.6 | Unit tests for `DefaultLlmService` | Test memory windowing, provider switching, missing-key error. Mock `ChatModel` via LangChain4j test utilities. | 0.5 |

### Phase 1 — AI Chat Panel (UC-1)

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 1.1 | Create `AiChatPanel` component | `StackPane`-based: `ScrollPane` with `VBox` of message bubbles above, `TextField` + send button below. Programmatic styling only, bound to `Theme`. | 0.1 |
| 1.2 | Streaming response rendering | Wire `TokenStream.onPartialResponse` to append tokens to the current assistant bubble on the JavaFX thread via `Platform.runLater`. Show spinner while waiting for first token. | 0.5, 1.1 |
| 1.3 | `AiChatContentFactory` | Implements `ContentFactory`. Creates `DockLeaf` containing `AiChatPanel`. Register with `DockManager.setContentFactory()`. | 1.1 |
| 1.4 | `AiChatStateAdapter` | Implements `ContentStateAdapter`. Persists conversation history and provider config via ServiceLoader. | 1.3 |
| 1.5 | Sample: `AiChatSample` | Add to samples catalog. Shows chat panel split-docked beside a `CodeEditor`. | 1.3 |
| 1.6 | Theme binding | All chat UI colors derived from `themeProperty()`. Verify switching LIGHT ↔ DARK at runtime. | 1.1 |

### Phase 2 — Multi-Provider Settings (UC-10)

> Start here so every subsequent phase has a working LLM connection.

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 2.1 | Create `LlmSettingsPanel` | Dockable panel: provider `ChoiceBox`, API key `PasswordField`, model `ComboBox`, temperature `Slider`, max-tokens `Spinner`. All bound to `LlmConfig`. | 0.4 |
| 2.2 | `LlmSettingsStateAdapter` | Saves provider/model/params to session JSON. API key stored separately (use GitHub module's PAT store as precedent — never in plain-text session). | 2.1 |
| 2.3 | Provider health check | On save, call `LlmService.chat("ping")` on a background thread. Show green ✓ / red ✗ indicator in the panel header within 5 s. | 2.1, 0.5 |
| 2.4 | Ollama support | Add `langchain4j-ollama` dependency. Auto-discover running Ollama instance. Populate model list from `/api/tags`. | 2.1, 0.2 |
| 2.5 | Anthropic support | Add `langchain4j-anthropic` dependency. Expose Claude model IDs in the model combo. | 2.1, 0.2 |

### Phase 3 — Code Editor AI Features (UC-2, UC-3)

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 3.1 | "Explain Selection" action | `Ctrl+Shift+E`: extract selection + ±10-line context from `Document`, POST to `LlmService`. Show streaming explanation in a floating popup anchored below the selection. | 0.5 |
| 3.2 | "Refactor Selection" action | `Ctrl+Shift+R`: send selection with a refactoring system prompt. Show original vs suggested in a diff popup. "Accept" replaces via `EditorEditController`. | 0.5 |
| 3.3 | Inline ghost-text completions | Debounce (~300 ms) after key input. Send current line prefix + 5-line context to `LlmService` streaming. Render ghost text in a contrasting color after the caret. Tab to accept (insert via `EditorEditController`), Esc to dismiss. Cancel in-flight request on new input. | 0.5 |
| 3.4 | Completion toggle | Toolbar button to enable/disable completions. Persisted in `CodeEditorStateAdapter` alongside existing state fields. | 3.3 |
| 3.5 | FxTests for editor AI actions | Headless TestFX: trigger explain/refactor with known selection, verify popup visible, verify accept writes correct text. Mock `LlmService`. | 3.1, 3.2 |

### Phase 4 — GitHub AI Features (UC-4, UC-5, UC-6)

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 4.1 | Generate commit message | "✨ Generate" button in commit area. Read staged diff via JGit `DiffFormatter`, pass to `LlmService` with Conventional Commits system prompt. Pre-fill commit message `TextField`. | 0.5 |
| 4.2 | Generate PR title & body | On PR creation dialog open, optional "Draft with AI" button. Feed `git log base..HEAD` + full diff. Pre-fill title + body fields. | 0.5 |
| 4.3 | AI code review → gutter markers | "Review" toolbar button. Send branch diff to `LlmService`; parse response for `file:line:severity:message` items. Inject into `MarkerModel` so findings appear in the `CodeEditor` gutter. | 0.5 |
| 4.4 | Unit tests for GitHub AI | Test prompt construction: verify diff is correctly truncated to token budget. Mock `LlmService`; assert commit message field is populated. | 4.1, 4.2 |

### Phase 5 — RAG & Semantic Search (UC-7, UC-8, UC-12)

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 5.1 | Add embeddings dependencies | Add `langchain4j-embeddings-all-minilm-l6-v2` (local, no API key) + `InMemoryEmbeddingStore`. Swappable later to persistent store. | 0.2 |
| 5.2 | `ProjectIndexer` service | Background thread. Walks project tree, reads `.java`, `.md`, `.json` files as `Document`s, splits with `DocumentSplitter` (by class/method for Java, by heading for markdown), embeds and stores. Reports progress via `LongProperty`. | 5.1 |
| 5.3 | Incremental re-indexing | `WatchService` loop watches source dirs. On `ENTRY_MODIFY`, re-embed only the changed file's segments. Debounce rapid saves. | 5.2 |
| 5.4 | `SemanticSearchService` | `List<SearchResult> search(String query, int topK)`. Embeds the query, retrieves top-K segments with file path + line range + score. | 5.2 |
| 5.5 | Semantic search mode in `CodeEditor` | Toggle in `SearchController` toolbar. Semantic mode sends query to `SemanticSearchService`; results appear in the existing search results list. Click navigates to file+line. | 5.4 |
| 5.6 | RAG Q&A panel | Dockable panel (reuse `AiChatPanel` UI). Backed by `RetrievalAugmentor` + `ContentRetriever` from `SemanticSearchService`. Each answer includes clickable source citations (file path + line). | 5.4, 1.1 |
| 5.7 | Index progress indicator | Status label in the dock bottom bar showing "Indexing… N/M files" during initial index and "Re-indexing…" on file change. | 5.2, 5.3 |

### Phase 6 — Hugo AI Features (UC-9)

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 6.1 | "AI Draft" action | Button in Hugo toolbar. Opens a small prompt dialog. LLM generates Hugo-flavored markdown via streaming. Result opens in a new `CodeEditor` `DockLeaf` tab for review. | 0.5 |
| 6.2 | "Expand Selection" action | Select partial markdown, invoke LLM to elaborate. Streaming tokens replace the selection in real time via `EditorEditController`. Esc cancels mid-stream. | 0.5, 6.1 |

### Phase 7 — LangChain4j `@Tool` / Agentic Actions (UC-11)

| # | Task | Description | Depends On |
|---|------|-------------|------------|
| 7.1 | `DockingTools` tool class | `@Tool`-annotated methods: `openFile(String path)` → creates `DockLeaf` via `DockManager`; `readFile(String path)` → returns content; `listFiles(String glob)` → returns file paths; `searchCode(String query)` → delegates to `SemanticSearchService`. | 0.5, 5.4 |
| 7.2 | Wire tools into `AiServices` builder | Register `DockingTools` with the `AiServices.builder()` used by `AiChatPanel`. | 7.1, 1.1 |
| 7.3 | Tool-call UI blocks in chat | When the LLM emits a tool call, insert a collapsible `HBox` ("Tool: openFile(path=…)") into the message list before the final answer. Expand to see raw JSON. | 7.2 |
| 7.4 | Tests for tool dispatch | Mock `ChatModel` to return a tool-call response. Assert `DockManager.createTabGroup()` is invoked with correct args. Assert `SemanticSearchService.search()` is called when `searchCode` tool fires. | 7.1 |

---

## Dependency Map

```
Phase 0 (module + LlmService)
  ├─→ Phase 1 (chat panel)
  │     └─→ Phase 5.6 (RAG Q&A)
  │           └─→ Phase 7 (tools)
  ├─→ Phase 2 (settings)       — enables real LLM for all other phases
  ├─→ Phase 3 (code editor AI)
  ├─→ Phase 4 (github AI)
  ├─→ Phase 5 (RAG + search)
  │     └─→ Phase 7.1 (searchCode tool)
  └─→ Phase 6 (hugo AI)
```

**Recommended implementation order:** 0 → 2 → 1 → 3 → 4 → 5 → 6 → 7

- **Phase 0** first: everything depends on the module and `LlmService`.
- **Phase 2** next: gives a working provider config before writing any UI that calls the LLM.
- **Phase 1** then: chat panel is the fastest way to verify the LLM pipeline end-to-end.
- **Phases 3–6** are independent of each other and can be parallelized across contributors.
- **Phase 7** last: composes file navigation (Phase 3), semantic search (Phase 5), and chat (Phase 1).