#

## Problem

```text
Given initial `editor2-files.json` layout
When I drag "Editor2" tab to "Editor1" tab group
Then "Files" content is not visible nor accessible. 
```

## Analysis

- The `editor2-files.json` layout starts with "Editor 2" and "Files" in the same `DockTabGroup`, with "Editor 2" active.
- Dragging "Editor 2" onto the "Editor 1" tab group runs `DragManager.executeDrop`, which first calls `removeFromParent`
  and therefore `DockTabGroup.removeLeaf` on the source group.
- In `removeLeaf`, when the active tab (index `0`) is removed and the remaining tab shifts into index `0`,
  `setActiveTab(0)` is called, but the `activeTabIndex` value does not change.
- Because `activeTabIndex` does not change, the listener does not fire and `updateContent()` is not called,
  so the source group's content area is never rebuilt for "Files".
- The moved "Editor 2" content is attached to the target group; the original group ends up with an empty content area,
  leaving the "Files" tab visible but its content missing until another tab change triggers a refresh.

## Proposed solution

- Ensure the source tab group refreshes its content when the active tab is removed and the active index stays the same.
- Preferred fix (systemic): in `DockTabGroup.removeLeaf`, after adjusting the active index, call
  `refreshActiveTabContent()` when `index == previousActiveIndex` (or when `index <= previousActiveIndex`)
  and the tab list is not empty. This forces the "Files" content to render even when the active index value is unchanged.
- Alternative fix (localized): in `DragManager.removeFromParent`, after `parent.removeLeaf(leaf)`,
  call `parent.refreshActiveTabContent()` if the parent still has tabs and the removed leaf was active.
