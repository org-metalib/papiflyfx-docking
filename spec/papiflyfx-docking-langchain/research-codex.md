# PapiflyFX Docking LangChain4j Tasks (Codex)

## Objective

Define practical LangChain4j integration work for PapiflyFX Docking. The result should feel native to the existing docking framework, reuse current `ContentFactory` and `ContentStateAdapter` patterns, and stay provider-agnostic at the project boundary.

## Working Assumptions

1. The implementation should introduce a new module named `papiflyfx-docking-langchain`.
2. LangChain4j should be treated as infrastructure, not as a feature by itself. User-facing value should come from dockable assistants and feature-specific actions.
3. All model calls must run off the JavaFX thread and support cancellation.
4. Secrets must not be stored in docking layout/session JSON. Persist only model profile ids and non-secret settings.
5. v1 should focus on text workflows first. Rich media or multimodal workflows can be planned as follow-up work.

## Use Cases

1. **Workspace assistant dock**
   A dockable chat pane that understands the current workspace, active leaf, and selected module so a developer can ask architecture and implementation questions without leaving the application.

2. **Code editor assistant**
   When a `CodeEditor` is active, the assistant can explain selected code, suggest refactors, draft unit tests, summarize compiler errors, and propose small edits for review.

3. **Project tree assistant**
   When a `TreeView` is active, the assistant can summarize project structure, identify likely files to inspect next, and answer questions about workspace organization.

4. **GitHub workflow assistant**
   When `GitHubToolbar` is active, the assistant can summarize branch status, explain likely impact of local changes, draft commit messages, and draft pull request titles and descriptions.

5. **Hugo content assistant**
   When `HugoPreviewPane` is active, the assistant can draft page content, generate front matter ideas, summarize preview issues, and suggest edits based on site structure and current page context.

6. **Spec and documentation assistant**
   The assistant can read files under `spec/` and repository `README.md` files to summarize decisions, convert research into task lists, and keep planning documents consistent.

7. **Media metadata assistant**
   When `MediaViewer` is active, the assistant can generate tags, titles, descriptions, and other metadata from available file names and metadata. Deeper audio/video understanding should be considered future work.

8. **Prompted action palette**
   The UI should expose repeatable actions such as `Explain Selection`, `Draft PR`, `Summarize Site`, and `Summarize Spec` so the feature is useful without requiring open-ended chat every time.

9. **Conversation persistence**
   AI dock state should survive layout persistence. Restored docks should recover conversation metadata, selected context providers, and prompt history without leaking secrets.

10. **Sample-driven adoption**
    `SamplesApp` should include at least one end-to-end LangChain4j demo showing how the assistant attaches to other PapiflyFX components.

## Recommended v1 Scope

1. A generic dockable AI assistant pane.
2. Context providers for `CodeEditor`, `TreeView`, `GitHubToolbar`, `HugoPreviewPane`, and `spec/` documents.
3. Read-only or preview-first actions. Any write operation should require explicit user confirmation.
4. One model-provider binding at first, behind a provider abstraction that allows more bindings later.
5. Deterministic tests using fake model responses.

## Task List

### 1. Foundation and Module Design

1. Create a new Maven module `papiflyfx-docking-langchain` and add it to the root `pom.xml`.
2. Add parent-managed properties and dependency management entries for LangChain4j and any selected provider bindings.
3. Decide module boundaries so AI support does not force unrelated feature modules to depend directly on LangChain4j unless necessary.
4. Define a package layout for API, UI, core orchestration, provider bindings, context extraction, actions, persistence, and security.
5. Define a small SPI for:
   - model profiles
   - chat/completion service
   - context providers
   - prompt actions
   - conversation persistence
   - tool execution policy
6. Define a threading model for background execution, streaming updates, cancellation, timeout handling, and UI-safe callbacks.

### 2. Docking-Native AI Pane

7. Implement an `AiAssistantPane` JavaFX component for chat, action execution, and context inspection.
8. Implement an `AiAssistantFactory` using the existing `ContentFactory` pattern so the pane can be created inside `DockManager`.
9. Implement an `AiAssistantStateAdapter` using the existing `ContentStateAdapter` pattern.
10. Add UI states for idle, running, streaming, cancelled, failed, and completed responses.
11. Add explicit controls for retry, cancel, clear conversation, and context selection.
12. Keep the UI useful even when no provider is configured by showing configuration guidance instead of failing silently.

### 3. Provider Configuration and Security

13. Add a provider abstraction so the application does not hard-code one model vendor into public APIs.
14. Implement the first provider binding for text generation through LangChain4j.
15. Add settings support for endpoint, model name, temperature, max tokens, and streaming toggle.
16. Add API key or token storage through a dedicated secret store abstraction.
17. Persist only non-secret profile configuration through docking state or application settings.
18. Add connectivity validation and readable error mapping for bad credentials, timeouts, and rate limits.

### 4. Context Extraction

19. Implement an active-leaf context resolver that inspects the current dock content and produces structured context for the assistant.
20. Add a `CodeEditor` context provider that captures file path, selection, caret position, visible text window, and optional diagnostics.
21. Add a `TreeView` context provider that captures selected node, visible hierarchy, and nearby siblings.
22. Add a `GitHubToolbar` context provider that captures repository url, branch, dirty status, and diff summary metadata.
23. Add a `HugoPreviewPane` context provider that captures site directory, current route, preview status, and recent errors.
24. Add a `MediaViewer` context provider that captures file path and metadata only; do not require multimodal inference in v1.
25. Add a repository/spec context provider that can read `spec/`, module `README.md`, and selected workspace files for planning-oriented prompts.

### 5. Prompt Actions and Tooling

26. Implement a reusable action registry for high-value prompts tied to the active context.
27. Ship initial actions:
   - explain selected code
   - propose refactor
   - generate unit-test outline
   - summarize repository status
   - draft commit message
   - draft pull request description
   - summarize Hugo page
   - draft Hugo front matter
   - summarize spec document
   - convert notes into tasks
28. Define tool boundaries clearly. v1 should allow safe reads of active content, selected files, and repository metadata without arbitrary command execution.
29. For any action that suggests code or file changes, show a preview that the user can review before applying manually.
30. If automated apply is added later, integrate it as an explicit follow-up phase rather than bundling it into v1.

### 6. Integration With Existing Modules

31. Add entry points from `CodeEditor` to open the assistant with the current selection preloaded.
32. Add entry points from `TreeView` to ask questions about the selected node or subtree.
33. Add entry points from `GitHubToolbar` to generate commit and PR drafts from the current repository state.
34. Add entry points from `HugoPreviewPane` to draft or revise content using page and site context.
35. Add a documentation workflow that turns `spec/` inputs into summaries and task lists for planning work.
36. Decide whether the feature-specific hooks belong in `papiflyfx-docking-langchain` or in small optional bridge classes inside each feature module.

### 7. Persistence and Session Recovery

37. Define a conversation state schema and version it from day one.
38. Persist conversation metadata, last-used actions, selected model profile, and attached context sources.
39. Ensure restored sessions degrade gracefully when referenced files, repositories, or site paths are no longer available.
40. Keep all secret material outside the persisted state payload.

### 8. Samples, Testing, and Validation

41. Add a LangChain4j integration demo to `papiflyfx-docking-samples`.
42. Add unit tests for context extraction, prompt construction, state codecs, provider settings, and error mapping.
43. Add deterministic tests around streaming, cancellation, retry, and timeout behavior using fake providers.
44. Add focused TestFX coverage for `AiAssistantPane`, context switching, and persistence/restore behavior.
45. Add manual validation scenarios for code, tree, GitHub, Hugo, and spec-driven workflows.

### 9. Documentation

46. Add a `README.md` for `papiflyfx-docking-langchain` describing the architecture, provider setup, and extension points.
47. Document security rules for API keys, state persistence, and tool execution.
48. Document the supported use cases, the initial non-goals, and the expected extension path for new providers and actions.
49. Add screenshots or short recordings to demonstrate the assistant in a docked workflow.

## Suggested Implementation Order

1. Foundation and module design.
2. Docking-native AI pane.
3. Provider configuration and security.
4. Context extraction for `CodeEditor` and `spec/`.
5. Prompt actions for code and planning workflows.
6. GitHub and Hugo integrations.
7. Persistence and session recovery.
8. Samples, tests, and documentation.

## Open Decisions

1. Should the AI integration stay in one module, or should optional bridge modules be created to keep feature dependencies tighter?
2. Is v1 read-only plus preview, or should it also support user-confirmed patch application in the code editor?
3. Which provider should be implemented first for local development and testability?
4. Should repository/spec context use simple file loading first, or should v1 also include embeddings and retrieval over workspace content?
