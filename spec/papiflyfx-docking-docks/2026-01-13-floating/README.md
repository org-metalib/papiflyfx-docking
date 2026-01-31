# Floating / Minimize / Maximize (papiflyfx-docks)

This directory contains a focused specification and implementation plan for adding:

- **Floating**: undock a `DockLeaf` into its own JavaFX `Stage` and allow docking it back.
- **Minimize**: hide a leaf into a "minimized area" (restore later).
- **Maximize**: temporarily expand a leaf to fill the entire dock area (toggle restore).

## Current Status (as of 2026-01-13)

**Core implementation complete (Phases A-D).**

| Phase | Description | Status |
|-------|-------------|--------|
| A | Infrastructure & API | ✅ Complete |
| B | UI Controls (title bar buttons) | ✅ Complete |
| C | Minimized UI (MinimizedBar) | ✅ Complete |
| D | Maximize Toggle | ✅ Complete |
| E | Persistence (session DTO) | ⏳ Pending |

### What's Working

- **Float button**: Opens panel in separate decorated window
- **Minimize button**: Panel slides to bottom bar, click to restore
- **Maximize button**: Panel fills dock area, double-click title bar toggles
- **Close floating window**: Docks panel back (prevents data loss)
- **Precise restore**: Minimized panels return to original tab position

### New Files

```
floating/
  FloatingDockWindow.java      # Hosts leaf in JavaFX Stage
  FloatingWindowManager.java   # Manages all floating windows

minimize/
  RestoreHint.java             # Stores restore location
  MinimizedStore.java          # Stores minimized leaves
  MinimizedBar.java            # Auto-hiding bottom bar UI
```

### Run Demo

```bash
mvn javafx:run -pl papiflyfx-docks
```

## Documents

- `IMPLEMENTATION_PLAN.md` — phased plan, acceptance criteria, proposed APIs, and UX review
- `PROGRESS_REPORT.md` — detailed implementation progress and technical decisions
