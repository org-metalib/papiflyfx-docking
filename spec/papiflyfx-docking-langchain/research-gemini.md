# 

# LangChain4j Integration for PapiflyFX Docking

This document outlines the vision and technical roadmap for integrating LangChain4j into the PapiflyFX Docking framework.

## Use Cases

### 1. AI-Powered Chat & Assistant Dock
* **Description:** A dedicated dockable panel providing a chat interface.
* **Details:** Allows users to interact with LLMs (OpenAI, Ollama, Anthropic) directly within the application. The assistant can be "aware" of the active dockable content.

### 2. Semantic Code & Content Analysis
* **Description:** Contextual actions for `papiflyfx-docking-code`.
* **Details:** Right-click actions on code editors to "Explain Code," "Suggest Refactor," or "Generate Unit Tests" using LangChain4j `AiServices`.

### 3. Natural Language UI Control (The "Magic Bar")
* **Description:** Controlling the docking layout via text commands.
* **Details:** Users can type commands like "Open the console at the bottom," "Close all floating windows," or "Switch to Debugging layout." The LLM maps these to framework API calls via Tool Calling.

### 4. RAG-Powered Project Explorer
* **Description:** Intelligent search in `papiflyfx-docking-tree`.
* **Details:** Indexing the local file system or project structure into a Vector Store. Users can ask, "Where is the logic for user authentication?" and the UI will highlight or open the relevant file in the tree.

### 5. Multi-modal Media Metadata (for `papiflyfx-docking-media`)
* **Description:** Automated descriptions for media assets.
* **Details:** Use vision/audio models to generate summaries or tags for media files currently displayed in media docks.

---

## Task List

### Phase 1: Foundation (Module Setup)
* [ ] **Create Module:** Initialize `papiflyfx-docking-langchain` module in the Maven/Gradle project.
* [ ] **Dependency Management:** Add `langchain4j`, `langchain4j-open-ai` (or Ollama), and `langchain4j-javafx` (if applicable) to the POM.
* [ ] **Core Service Provider:** Implement a `PapiflyAiService` to manage model configurations and API keys globally within the framework.

### Phase 2: UI Components
* [ ] **Base AI Dock:** Create `AiChatDock`—a reusable UI component with a message history (ListView/VBox) and input field.
* [ ] **Markdown Rendering:** Integrate a lightweight Markdown renderer for AI responses in the dock.
* [ ] **Loading States:** Implement non-blocking JavaFX `Task` wrappers for LLM calls to keep the UI responsive.

### Phase 3: Framework Integration (Tooling)
* [ ] **Docking Actions as Tools:** Annotate core docking methods (e.g., `dock(Node, Position)`, `float(Node)`)
  with `@Tool` so an LLM agent can manipulate the layout.
* [ ] **Context Injection:** Create a mechanism to automatically inject the "Active Dock Context" (text content, file name,
  or metadata) into the system prompt.

### Phase 4: RAG & Storage
* [ ] **Local Vector Store Integration:** Add support for a local embedding store (like Lucene or Chroma) to index project
  files.
* [ ] **Ingestion Pipeline:** Build a background service that watches the `papiflyfx-docking-tree` and updates the vector
  index as files change.

### Phase 5: Samples & Docs
* [ ] **Demo App:** Update `papiflyfx-docking-samples` with an "AI-Enabled IDE" demo.
* [ ] **Documentation:** Write the `README.md` for the new module explaining how to plug in custom models.