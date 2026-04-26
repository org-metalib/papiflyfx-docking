# Docks Ribbon Collapse/Expand Icon

**Lead Agent:** @ui-ux-designer
**Owning Module:** `papiflyfx-docking-docks`
**Reviewers:** @core-architect, @qa-engineer, @spec-steward

## Summary

The ribbon header collapse/expand command rendered as changing text (`Collapse` / `Expand`). This makes the header feel heavier than the surrounding compact toolbar actions and causes the button width to shift when the state changes.

Ribbon 8 converts that control to a fixed-size icon button while preserving the existing minimized behavior, tooltip, and accessible name.

This slice also fixes the minimized tab body regression found during manual review: selecting a ribbon tab while minimized now shows the selected tab's command groups instead of an empty panel.

## Acceptance Criteria

1. The ribbon collapse/expand button renders as an icon-only control.
2. The control keeps a stable compact footprint in both states.
3. The action remains accessible through explicit accessible text and tooltip copy.
4. Existing ribbon minimized session behavior remains unchanged.
5. A focused regression test covers icon-only rendering and state copy.
6. Selecting a tab while minimized renders the selected tab's command panel with its buttons.
