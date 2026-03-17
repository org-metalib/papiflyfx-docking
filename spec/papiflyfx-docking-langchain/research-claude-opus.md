# PapiflyFX Docking — LangChain4j Integration

## Use Cases

### UC-1: Dockable AI Chat Panel
A conversational AI panel that lives inside the docking layout. Users type prompts, receive streamed responses rendered
in real time. Supports chat memory so multi-turn conversations maintain context. Can be floated, minimized, or split-docked
like any other panel.

### UC-2: Code Explanation & Refactoring
Select code in `CodeEditor`, right-click or press a shortcut → AI explains the selection, suggests refactoring, or rewrites it.
The result appears in a side-by-side diff view or inline suggestion overlay.

### UC-3: AI-Assisted Commit Messages
In the `papiflyfx-docking-github` toolbar, when the user clicks "Commit", the LLM reads the staged diff and proposes
a commit message. The user can accept, edit, or regenerate.

### UC-4: PR Description Generation
When creating a pull request via the GitHub toolbar, the LLM summarizes all commits on the branch into a PR title and body.
Feeds the diff + commit log as context.

### UC-5: Natural-Language Code Search
Instead of regex search in `CodeEditor`, the user types a natural-language query ("find where the theme colors are applied").
The system embeds project files into a vector store and retrieves semantically relevant code fragments.

### UC-6: Project-Level RAG Q&A
Index the entire project (source files, `spec/` docs, READMEs) into an embedding store. A dockable Q&A panel lets developers
ask questions about the codebase and get grounded answers with source references.

### UC-7: Hugo Content Drafting
In the `papiflyfx-docking-hugo` module, provide an "AI Draft" action that generates or expands Hugo markdown content 
based on a user prompt. The draft appears in a `CodeEditor` tab for review before saving.

### UC-8: Inline Code Completion
As the user types in `CodeEditor`, send the surrounding context to the LLM and display ghost-text completions (similar to Copilot).
Accept with Tab, dismiss with Esc.

### UC-9: AI-Powered Code Review
Given a diff (from the GitHub module or local changes), the LLM reviews it for bugs, style issues, and security concerns.
Results appear as markers/annotations in the `CodeEditor` gutter via `MarkerModel`.

### UC-10: Multi-Provider Configuration
A settings panel where the user selects the LLM provider (OpenAI, Anthropic, Ollama/local, Google Gemini), enters API keys,
and configures model parameters. Stored per-session via docking persistence.

---

## Tasks

### Phase 0 — Foundation Module

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 0.1 | Create `papiflyfx-docking-langchain` module | New Maven module with `langchain4j-bom` dependency management in parent pom. GroupId `org.metalib.papifly.docking`, package `org.metalib.papifly.fx.langchain`. Depends on `papiflyfx-docking-api` (compile) and `papiflyfx-docking-docks` (test). | — |
| 0.2 | Add LangChain4j BOM to parent pom | Add `langchain4j-bom` to `<dependencyManagement>` in root `pom.xml`. Pin to latest stable version (≥ 1.0). Add `langchain4j-open-ai` as the default provider. | — |
| 0.3 | Define `LlmService` interface | Central API interface: `ChatResponse chat(String userMessage)`, `TokenStream chatStreaming(String userMessage)`, `void configure(LlmProviderConfig config)`. Lives in the langchain module. | 0.1 |
| 0.4 | Implement `LlmProviderConfig` record | Immutable config: provider enum (OPENAI, ANTHROPIC, OLLAMA, GEMINI), apiKey, modelName, temperature, maxTokens, baseUrl (for Ollama). Serializable to/from JSON using the project's existing Map-based serializer. | 0.1 |
| 0.5 | Implement `DefaultLlmService` | Wraps LangChain4j `ChatModel` and `StreamingChatModel`. Instantiates the correct provider based on `LlmProviderConfig`. Manages `ChatMemory` (MessageWindowChatMemory with configurable window size). | 0.3, 0.4 |
| 0.6 | Write unit tests for `DefaultLlmService` | Test config switching, chat memory windowing, error handling for missing API key. Mock the underlying `ChatModel` using LangChain4j's test utilities. | 0.5 |

### Phase 1 — AI Chat Panel (UC-1)

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 1.1 | Create `AiChatPanel` JavaFX component | StackPane-based panel: text input field at bottom, scrollable message list above. Messages rendered as styled `VBox` children (user vs assistant). No CSS — programmatic styling via `Theme`. | 0.1 |
| 1.2 | Integrate streaming response rendering | Connect `TokenStream` callbacks (`onPartialResponse`) to append text token-by-token into the active assistant message bubble. Handle `onError` with inline error display. | 0.5, 1.1 |
| 1.3 | Create `AiChatContentFactory` | Implements `ContentFactory` so the chat panel can be created as a `DockLeaf`. Register with `DockManager.setContentFactory()`. | 1.1 |
| 1.4 | Create `AiChatStateAdapter` | Implements `ContentStateAdapter` for session persistence. Saves/restores: conversation history, provider config, panel scroll position. Register via ServiceLoader. | 1.3 |
| 1.5 | Add chat panel to samples app | New `AiChatSample` in `papiflyfx-docking-samples` catalog. Shows the chat panel docked alongside a code editor. | 1.3 |
| 1.6 | Theme integration | Bind chat panel colors to `DockManager.themeProperty()`. Ensure dark/light theme switching updates message bubbles, input field, and scrollbar. | 1.1 |

### Phase 2 — Code Editor AI Features (UC-2, UC-8)

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 2.1 | Add "Explain Selection" action to `CodeEditor` | New keyboard shortcut (e.g., Ctrl+Shift+E). Extracts selected text + surrounding context, sends to `LlmService`, displays result in a popup or side panel. | 0.5 |
| 2.2 | Add "Refactor Selection" action | Sends selected code with a refactoring prompt. Shows a diff between original and suggested code. User accepts or dismisses. | 0.5, 2.1 |
| 2.3 | Implement inline ghost-text completion | On typing pause (debounced ~300ms), send current line + context to `LlmService` streaming endpoint. Render suggestion as dimmed text after the caret. Tab to accept, Esc to dismiss. Cancel in-flight requests on new input. | 0.5 |
| 2.4 | Add completion toggle to editor toolbar | Button or shortcut to enable/disable inline completions. Persisted in editor state via `CodeEditorStateAdapter`. | 2.3 |
| 2.5 | Write FxTests for code editor AI actions | Headless TestFX tests: trigger explain/refactor actions with known input, verify popup appears. Mock `LlmService` responses. | 2.1, 2.2 |

### Phase 3 — GitHub AI Features (UC-3, UC-4, UC-9)

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 3.1 | Add "Generate Commit Message" to GitHub toolbar | Button next to commit input. Reads `git diff --staged` via JGit, sends to `LlmService` with a system prompt for commit message style. Populates the commit message field. | 0.5 |
| 3.2 | Add "Generate PR Description" to PR creation flow | When user clicks "Create PR", optionally invoke LLM with `git log` + `git diff base...HEAD`. Pre-fill the PR title and body fields. | 0.5 |
| 3.3 | AI code review on diff | New "Review" button in GitHub toolbar. Sends the current branch diff to the LLM with a review-focused system prompt. Displays review comments as annotations in the code editor gutter via `MarkerModel`. | 0.5 |
| 3.4 | Write tests for GitHub AI features | Unit tests for prompt construction (verify diff is correctly formatted for the LLM). Mock `LlmService` to verify expected tool calls. | 3.1, 3.2 |

### Phase 4 — RAG & Semantic Search (UC-5, UC-6)

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 4.1 | Add `langchain4j-embeddings` dependency | Add embedding model dependency (e.g., `langchain4j-embeddings-all-minilm-l6-v2` for local or provider-based). Add `InMemoryEmbeddingStore` for development. | 0.2 |
| 4.2 | Implement `ProjectIndexer` | Scans project directory, loads source files as `Document`s, splits into `TextSegment`s (by method/class for Java, by section for markdown), embeds and stores. Runs on background thread. | 4.1 |
| 4.3 | Implement `SemanticCodeSearchController` | Accepts a natural-language query, embeds it, retrieves top-K relevant segments from the embedding store. Returns results with file path, line range, and relevance score. | 4.2 |
| 4.4 | Add semantic search UI to `CodeEditor` | New search mode toggle in `SearchController`: regex vs semantic. Semantic mode sends query to `SemanticCodeSearchController`, displays results in a result list that navigates to file+line on click. | 4.3 |
| 4.5 | Create RAG Q&A panel | Dockable panel (like AI Chat) but backed by `ContentRetriever` + `RetrievalAugmentor`. Answers are grounded in project files. Shows source references as clickable links. | 4.2, 1.1 |
| 4.6 | Incremental re-indexing | Watch file system for changes (via `WatchService`), re-index only modified files. Avoid re-embedding the entire project on every change. | 4.2 |

### Phase 5 — Hugo AI Features (UC-7)

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 5.1 | Add "AI Draft" action to Hugo toolbar | Button that opens a prompt dialog. User describes what to write. LLM generates Hugo-flavored markdown. Result opens in a new `CodeEditor` tab. | 0.5 |
| 5.2 | Add "Expand Selection" action | Select partial markdown in the editor, invoke LLM to expand/elaborate. Streaming result replaces the selection. | 0.5, 5.1 |

### Phase 6 — Settings & Multi-Provider (UC-10)

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 6.1 | Create `LlmSettingsPanel` | Dockable settings panel: provider dropdown, API key field (masked), model name combo, temperature slider, max tokens spinner. All fields bound to `LlmProviderConfig`. | 0.4 |
| 6.2 | Persist settings via docking session | `LlmSettingsStateAdapter` saves/restores provider config. API keys stored separately (not in plain-text session JSON) — integrate with the GitHub module's PAT token store pattern. | 6.1, 0.4 |
| 6.3 | Add Ollama (local) provider support | Add `langchain4j-ollama` dependency. Auto-detect running Ollama instance. List available models in the settings dropdown. | 6.1 |
| 6.4 | Add Anthropic provider support | Add `langchain4j-anthropic` dependency. Support Claude models in provider config. | 6.1 |
| 6.5 | Provider health check | On settings save, ping the configured provider with a minimal request. Show green/red indicator in the settings panel. | 6.1 |

### Phase 7 — LangChain4j Tools Integration

| #   | Task | Description | Depends On |
|-----|------|-------------|------------|
| 7.1 | Define docking-aware `@Tool` methods | Create tool classes the LLM can call: `OpenFileTool` (opens a file in the editor), `SearchCodeTool` (runs grep/semantic search), `GetFileContentTool` (reads file content), `ListFilesTool` (lists project files). | 0.5, 4.3 |
| 7.2 | Register tools with AI Services | Wire tool classes into the `AiServices.builder()` so the chat panel's LLM can autonomously open files, search code, and navigate the project. | 7.1, 1.1 |
| 7.3 | Tool execution feedback in chat UI | When the LLM invokes a tool, show a collapsible "Tool call: OpenFile(path=...)" block in the chat panel so the user sees what actions were taken. | 7.2 |
| 7.4 | Write tests for tool invocations | Mock `ChatModel` to return tool-call responses. Verify that `OpenFileTool` triggers `DockManager` to create the correct `DockLeaf`. | 7.1 |

---

## Priority & Sequencing

```
Phase 0 (foundation) ──→ Phase 1 (chat panel) ──→ Phase 7 (tools)
        │                                               │
        ├──→ Phase 2 (code editor AI) ─────────────────┘
        ├──→ Phase 3 (github AI)
        ├──→ Phase 4 (RAG & search)  ──→ Phase 7 (tools use search)
        ├──→ Phase 5 (hugo AI)
        └──→ Phase 6 (settings/multi-provider)
```

**Recommended order:** 0 → 6 → 1 → 2 → 3 → 4 → 5 → 7

Start with Phase 0 (module skeleton) and Phase 6 (settings) so all subsequent phases have a configured, working LLM connection. Phase 1 (chat) provides immediate user value and a test harness for the LLM integration. Phases 2–5 are independent and can be parallelized. Phase 7 (tools) comes last as it composes features from earlier phases.