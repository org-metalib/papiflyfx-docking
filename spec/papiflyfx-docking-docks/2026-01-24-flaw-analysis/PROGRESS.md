# Flaw Analysis Remediation Progress

Date: 2026-01-24

## Completed
- High 1: layout/contentFactory persistence now preserves `contentFactoryId` on leaves and uses the configured `ContentFactory` during restore.
- High 2: tab-bar hit testing uses actual tab nodes and insertion indices are clamped before insertion to prevent bad reorders/IOOB.
- Medium 3: drag release events now consume when a drag actually occurred to avoid stray click actions.
- Medium 4: theme listeners are removed when split groups or floating windows are disposed; floating window tab groups are disposed on close/unfloat to avoid leaks.
- Medium 5: floating a maximized leaf now restores it first; re-floating a floating leaf at coordinates repositions instead of detaching.
- Medium 6: `DockTabGroup.getTabs()` returns an unmodifiable view; internal tab reorders use a dedicated `moveLeaf` helper.
- Low 7: deprecated `OverlayCanvas.showDropHint(Bounds)` now uses a safe drop zone and guards nulls to prevent NPEs.
- Low 8: floating restore hints map is cleared during dispose to avoid stale entries.
- Tests: `./mvnw -pl papiflyfx-docks test` passes locally.

## Remaining
- None.

## Notes
- Test suite is JavaFX/TestFX; headless settings remain opt-in via pom properties.
