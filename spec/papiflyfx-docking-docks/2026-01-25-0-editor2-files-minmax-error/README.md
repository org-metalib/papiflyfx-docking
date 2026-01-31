#

## Problem

With initial `editor2-files.json` layout, when I maximize "Editor2" tab and then restore it. The tab content is not visibele nor it accessible. When I click on "Files" table and then click back on "Editor2" tab the "Editor2" content gets restore. 

## Analysis

- The `editor2-files.json` layout starts with the "Editor 2" tab active inside the same `DockTabGroup` as "Files".
- On maximize, `DockManager.maximizeLeaf` removes the "Editor 2" leaf from its original `DockTabGroup`. 
  The group's `removeLeaf` logic shifts `activeTabIndex` to the remaining tab (now "Files"), and `updateContent()` renders the "Files" content.
- On restore, `DockManager.restoreMaximized` re-inserts the leaf with `DockTabGroup.addLeaf(index, leaf)`.
  This sets `activeTabIndex` to the same numeric value (still `0`), so the `activeTabIndex` listener does not fire and `updateContent()` is not called.
- Result: the tab bar shows "Editor 2" as active again, but the content area still contains the previously rendered "Files" 
  content (or appears blank if that node was moved), until the user clicks another tab and back, which triggers `updateContent()`.

## Proposed solution

- Force a content refresh after re-inserting the leaf during restore.
- Concrete fix option (localized): in `DockManager.restoreMaximized`, after `tabGroup.addLeaf(...)`, 
  call `tabGroup.setActiveTab(leaf)` and `tabGroup.refreshActiveTabContent()` to rebuild the content area even when the active index value does not change.
- Alternative fix (systemic): in `DockTabGroup.addLeaf(int index, DockLeaf leaf)`, detect when the active index remains the same and call 
  `updateContent()` (or `refreshActiveTabContent()`) so the content area matches the new active leaf.
