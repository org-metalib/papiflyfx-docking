## JavaFX UI Architecture: Docks, Code, Settings, GitHub, Hugo, Media

### Rendering Strategy

| Aspect | Docks | Code | Settings | GitHub | Hugo | Media |
|--------|-------|------|----------|--------|------|-------|
| **Base class** | `BorderPane`, custom `Pane` | `StackPane` → `Region` + `Canvas` | `BorderPane` → standard controls | `HBox` → standard controls | `BorderPane` + `WebView` | `StackPane` → per-type viewers |
| **Rendering** | Scene graph (Regions, HBox, StackPane) | Canvas immediate-mode (virtualized) | Scene graph (ListView, TextField, CheckBox) | Scene graph + CSS stylesheets | Scene graph + WebView engine | Scene graph + selective Canvas for icons |
| **Why** | Layout structure is relatively static | Must render thousands of lines efficiently | Form-based UI with standard controls | Toolbar with popups/dialogs — standard controls suffice | HTML content rendered by WebEngine | Diverse media types; Canvas only for decorative glyphs |
| **Overlay** | `OverlayCanvas` (Canvas) for drag hints | `StackPane` layers for search/goto overlays | None | `Popup` for ref dropdown, `Dialog` for modals | Placeholder label over WebView | `TransportBar` auto-hide overlay on video |

**Key insight**: Code is the only module that uses Canvas as its primary rendering surface. Media uses Canvas selectively for transport/zoom button icons. All other modules rely on the JavaFX scene graph. GitHub is unique in loading external CSS stylesheets alongside inline styles. Hugo delegates content rendering entirely to WebView/WebEngine.

---

### Component Composition

| | Docks | Code | Settings | GitHub | Hugo | Media |
|--|-------|------|----------|--------|------|-------|
| **Main node** | `DockManager` (BorderPane) | `CodeEditor` (StackPane) | `SettingsPanel` (BorderPane) | `GitHubToolbar` (HBox) | `HugoPreviewPane` (BorderPane) | `MediaViewer` (StackPane) |
| **Content model** | `DockElement` composite tree (recursive) | `Document` text buffer + `LineIndex` | `SettingDefinition<T>` records | `GitHubToolbarViewModel` (observable properties) | `WebEngine` (HTML DOM) | `MediaState` record + per-type viewers |
| **Children** | `DockTabGroup` / `DockSplitGroup` / `DockLeaf` | `Viewport` + `GutterView` + overlays | `SettingsCategoryList` + `SettingControl<T>` | `RefPill`, `GitRefPopup`, chip strip, action bar | `HugoPreviewToolbar` + `WebView` + `HugoPreviewStatusBar` | `ImageViewer` / `VideoViewer` / `AudioViewer` / `SvgViewer` / `EmbedViewer` / `ErrorViewer` |
| **Layout** | Custom `layoutChildren()` in SplitContainer | Canvas virtual layout (only visible lines) | Standard JavaFX layout managers | Standard HBox/VBox layout | BorderPane (top/center/bottom) | StackPane swaps active viewer by media type |

---

### Input Handling

| | Docks | Code | Settings | GitHub | Hugo | Media |
|--|-------|------|----------|--------|------|-------|
| **Pattern** | Centralized `DragManager` + per-component handlers | **Multi-controller** decomposition | Standard JavaFX control events | ViewModel bindings + event filters + dialog callbacks | Constructor-injected `Runnable` callbacks | Inline `wire*()` methods per viewer |
| **Controllers** | `DragManager`, `HitTester` | `InputController`, `PointerController`, `NavigationController`, `EditController` + coordinators | None (inline listeners) | `GitRefPopupController` mediator + `CommandRunner` async executor | None (inline handlers) | None (inline handlers) |
| **Complexity** | Medium (drag-drop, divider resize) | Very high (keyboard, mouse, multi-caret, scrollbars) | Low (form inputs, button clicks) | Medium (popup keyboard nav, async git ops, modal dialogs) | Low (button clicks, WebEngine listeners) | Medium (pan/zoom, keyboard shortcuts, scrub throttling) |
| **Event routing** | Scene-level filter on `rootPane` → DragManager | `setOnKeyPressed/Typed/MousePressed/...` on editor | Property change listeners on controls | Event filters on popup + keyboard routing in RefPill | `setOnAction` lambdas on toolbar buttons | `setOnScroll/MousePressed/Dragged` + `addEventFilter(KeyEvent)` |

---

### State Management

| | Docks | Code | Settings | GitHub | Hugo | Media |
|--|-------|------|----------|--------|------|-------|
| **Observable props** | `activeTabIndex`, `dividerPosition`, `themeProperty`, `rootElement` | `cursorLine`, `scrollOffset`, `wordWrap`, `languageId` | Dirty flags + storage get/put | `currentBranch`, `dirty`, `busy`, `authenticated`, `aheadCount`, computed `BooleanBinding`s | `themeProperty` + volatile fields (`cliAvailable`, `boundPort`, `lastRelativePath`) | `themeProperty`, `volume`, `zoomLevel`, `PlaybackState` enum |
| **Persistence** | `DockSessionData` → JSON (Map-based codec) | `EditorStateData` → via `ContentStateAdapter` | `JsonSettingsStorage` (3 scopes: APP/WORKSPACE/SESSION) | `captureState()` → `Map<String, Object>` (remote URL, local path, owner, repo) | `HugoPreviewState` record → `Map<String, Object>` via codec | `MediaState` record → `Map<String, Object>` via `MediaStateCodec` |
| **State granularity** | Layout tree + floating/minimized/maximized | Caret, scroll, folds, language, file path | Key-value pairs per scope | Repository connection info | Site root, relative path, drafts flag | Playback position, volume, muted, zoom, pan |

---

### Theming

| | Docks | Code | Settings | GitHub | Hugo | Media |
|--|-------|------|----------|--------|------|-------|
| **Theme record** | `Theme` (22 properties) | `CodeEditorTheme` (54 properties) | Reuses docking `Theme` | `GitHubToolbarTheme` (24 properties + dimensions) | Reuses docking `Theme` (chrome only) | Reuses docking `Theme` via `MediaThemeMapper` |
| **Application** | Programmatic `Background`/`Border`/`Font` | Canvas draw calls use theme colors | Inline `-fx-` CSS style strings | Inline CSS variables + external stylesheets | Hardcoded CSS strings + programmatic `Border` | Programmatic `Background`/`Border` + Canvas icon repainting |
| **Binding** | `themeProperty.addListener` → `applyTheme()` | `bindThemeProperty()` → `CodeEditorThemeMapper` → all passes | `themeProperty.addListener` → inline styles | `bindThemeProperty()` → `GitHubToolbarThemeMapper` → CSS vars + stylesheet | `bindThemeProperty()` → `syncThemeBinding()` | `bindThemeProperty()` → propagates to all child viewers |

Code and GitHub each have their own extended theme records mapped from the base `Theme`. Hugo's chrome has a fixed dark-blue palette largely independent of the host theme. Media maps base theme properties to viewer backgrounds and control colors. Settings applies theme via inline CSS strings. Docks uses pure programmatic Background/Border/Font objects.

---

### Extension / Plugin Model

| | Docks | Code | Settings | GitHub | Hugo | Media |
|--|-------|------|----------|--------|------|-------|
| **Extension point** | `ContentFactory` + `ContentStateAdapter` (ServiceLoader) | Language lexers + fold providers via registry | `SettingsCategory` + `SettingsContributor` (ServiceLoader) | `CredentialStore` + `GitRepository` interfaces (DI) | `SettingsCategory` via ServiceLoader | `UrlKind` enum + `ViewerFactory` switch |
| **Integration** | Content modules register factories | Lexers registered per language ID | Modules contribute categories via META-INF/services | ViewModel accepts injected credential store + git repo | META-INF/services for `HugoCategory` | Self-contained; new media types require code change |

---

### Concurrency

| | Docks | Code | Settings | GitHub | Hugo | Media |
|--|-------|------|----------|--------|------|-------|
| **Threading** | FX thread only | `ScheduledExecutorService` (lexing/folding, 35ms debounce) | 150ms timer for dirty-check | `CommandRunner` (ExecutorService + `Platform.runLater`) | `ExecutorService` (server lifecycle) + `synchronized` | `Platform.runLater` for MediaPlayer callbacks |
| **Async pattern** | N/A | Background lexer → `Platform.runLater(tokenMap)` | Timer-based polling | Async git ops → snapshot update on FX thread | Background process management → FX thread state updates | MediaPlayer status listeners |

---

### Similarities (shared across all six modules)

1. **All programmatic JavaFX** — no FXML; GitHub is the only module that loads external CSS stylesheets
2. **`DisposableContent` lifecycle** — all implement cleanup via `dispose()`
3. **Reactive via `ObjectProperty<Theme>`** — live theme switching through `bindThemeProperty()`
4. **Immutable records** for data transfer (`DockData`, `EditorStateData`, `SettingDefinition`, `CurrentRefState`, `HugoPreviewState`, `MediaState`)
5. **Docking integration** — each has a `ContentFactory` + `ContentStateAdapter` for session persistence
6. **No external JSON libraries** — all codecs are hand-rolled `Map<String, Object>` serialization
7. **`captureState()` / `applyState()` pattern** — uniform state snapshot/restore across content modules

### Key Differences

1. **Rendering**: Code is Canvas (performance-critical); Media mixes scene graph + selective Canvas for icons; all others are pure scene graph. Hugo delegates content to WebView/WebEngine
2. **Input complexity**: Code has 6+ specialized controllers; GitHub has ViewModel + popup controller + async command runner; Docks has centralized drag manager; Media has per-viewer inline wiring; Hugo and Settings use simple inline handlers
3. **State scope**: Settings has multi-scope persistence (APP/WORKSPACE/SESSION); all other modules have single session state via `ContentStateAdapter`
4. **Theme depth**: Code extends to 54 properties; GitHub extends to 24 properties + dimensions; Hugo has a largely fixed dark-blue chrome palette; Docks/Settings/Media use the 22-property base theme
5. **Custom layout**: Docks implements its own `layoutChildren()` for split panes; Code virtualizes via Canvas; Media swaps child viewers by type; GitHub/Hugo/Settings delegate to standard JavaFX layout managers
6. **Concurrency model**: Code and Hugo use background `ExecutorService`; GitHub uses `CommandRunner` with async git operations; Media relies on MediaPlayer's built-in async; Settings uses a simple timer; Docks is FX-thread-only
7. **CSS usage**: GitHub is unique in combining external CSS stylesheets with inline CSS variables; Hugo uses hardcoded CSS strings; Settings uses inline `-fx-` styles; Code/Docks/Media avoid CSS entirely
