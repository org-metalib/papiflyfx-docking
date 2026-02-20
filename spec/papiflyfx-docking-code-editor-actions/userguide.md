# PapiflyFX Code Editor — Keyboard & Mouse Reference

## Keyboard Shortcuts

### Editing

| Action | Windows / Linux | macOS |
|---|---|---|
| Undo | `Ctrl+Z` | `Cmd+Z` |
| Redo | `Ctrl+Y` or `Ctrl+Shift+Z` | `Cmd+Shift+Z` |
| Copy | `Ctrl+C` | `Cmd+C` |
| Cut | `Ctrl+X` | `Cmd+X` |
| Paste | `Ctrl+V` | `Cmd+V` |
| Select all | `Ctrl+A` | `Cmd+A` |
| Backspace | `Backspace` | `Delete` |
| Delete forward | `Delete` | `Fn+Delete` |
| New line | `Enter` | `Enter` |

### Caret Navigation

| Action | Windows / Linux | macOS |
|---|---|---|
| Move left / right | `Left` / `Right` | `Left` / `Right` |
| Move up / down | `Up` / `Down` | `Up` / `Down` |
| Line start | `Home` | `Home` |
| Line end | `End` | `End` |
| Move by word left | `Ctrl+Left` | `Alt+Left` |
| Move by word right | `Ctrl+Right` | `Alt+Right` |
| Document start | `Ctrl+Home` | `Cmd+Up` |
| Document end | `Ctrl+End` | `Cmd+Down` |

### Selection

| Action | Windows / Linux | macOS |
|---|---|---|
| Select left / right | `Shift+Left` / `Shift+Right` | `Shift+Left` / `Shift+Right` |
| Select up / down | `Shift+Up` / `Shift+Down` | `Shift+Up` / `Shift+Down` |
| Select to line start | `Shift+Home` | `Shift+Home` |
| Select to line end | `Shift+End` | `Shift+End` |
| Select word left | `Ctrl+Shift+Left` | `Alt+Shift+Left` |
| Select word right | `Ctrl+Shift+Right` | `Alt+Shift+Right` |
| Select to document start | `Ctrl+Shift+Home` | `Cmd+Shift+Up` |
| Select to document end | `Ctrl+Shift+End` | `Cmd+Shift+Down` |

### Word Deletion

| Action | Windows / Linux | macOS |
|---|---|---|
| Delete word left | `Ctrl+Backspace` | `Alt+Delete` |
| Delete word right | `Ctrl+Delete` | `Alt+Fn+Delete` |

### Line Operations

| Action | Windows / Linux | macOS |
|---|---|---|
| Delete line | `Ctrl+Shift+K` | `Cmd+Shift+K` |
| Move line up | `Alt+Up` | `Alt+Up` |
| Move line down | `Alt+Down` | `Alt+Down` |
| Duplicate line up | `Alt+Shift+Up` | `Alt+Shift+Up` |
| Duplicate line down | `Alt+Shift+Down` | `Alt+Shift+Down` |
| Join lines | `Ctrl+J` | `Cmd+J` |

### Search & Navigation

| Action | Windows / Linux | macOS |
|---|---|---|
| Find | `Ctrl+F` | `Cmd+F` |
| Go to line | `Ctrl+G` | `Cmd+G` |
| Close search | `Escape` | `Escape` |

## Mouse Actions

| Action | Gesture |
|---|---|
| Place caret | Click |
| Range selection | Click and drag |
| Extend selection | `Shift+Click` |

## Notes

- **Word boundaries** follow the `[A-Za-z0-9_]` word-character class. Underscores are treated as part of a word (e.g. `foo_bar` is one word). Punctuation and whitespace act as boundaries.
- **Line operations with selection** — when a selection spans multiple lines, delete/move/duplicate line commands act on the entire range of selected lines.
- **Single-step undo** — each line operation (delete, move, duplicate, join) is recorded as a single undo step.
- **Cross-line word navigation** — moving word-left at column 0 jumps to the end of the previous line; moving word-right at end of line jumps to the start of the next line.
