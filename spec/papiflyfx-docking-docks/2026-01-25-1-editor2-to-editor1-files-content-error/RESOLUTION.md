# Resolution: Files Content Not Visible After Dragging Editor2 to Editor1

## Issue Summary
When dragging "Editor 2" tab to "Editor 1" tab group from the `editor2-files.json` layout, the "Files" content became inaccessible. The tab was visible but its content area remained empty.

## Root Cause
The bug occurred in `DockTabGroup.removeLeaf()`:

1. "Editor 2" (index 0, active) and "Files" (index 1) were in the same tab group
2. When "Editor 2" was removed, "Files" shifted from index 1 to index 0
3. The code called `setActiveTab(Math.max(0, activeTabIndex.get() - 1))` which became `setActiveTab(0)`
4. Since `activeTabIndex` was already 0, the value didn't change
5. The change listener on `activeTabIndex` didn't fire, so `updateContent()` was never called
6. The content area remained empty until another tab change triggered a refresh

## Solution Implemented
Modified `DockTabGroup.removeLeaf()` to detect when the active index value doesn't change but the tab at that index has changed, and explicitly call `refreshActiveTabContent()`:

```java
public void removeLeaf(DockLeaf leaf) {
    int index = tabs.indexOf(leaf);
    if (index >= 0) {
        int previousActiveIndex = activeTabIndex.get();
        tabs.remove(index);
        leaf.setParent(null);

        // Adjust active tab index
        if (tabs.isEmpty()) {
            activeTabIndex.set(-1);
        } else if (index <= activeTabIndex.get()) {
            int newActiveIndex = Math.max(0, activeTabIndex.get() - 1);
            setActiveTab(newActiveIndex);
            
            // If the active index value hasn't changed but the tab at that index has changed,
            // force a content refresh since the listener won't fire
            if (newActiveIndex == previousActiveIndex && !tabs.isEmpty()) {
                refreshActiveTabContent();
            }
        }

        if (onTabClose != null) {
            onTabClose.accept(leaf);
        }
    }
}
```

## Testing
Added a new test `removeFirstActiveTab_displaysRemainingTabContent()` in `DockTabGroupFxTest` that:
1. Creates a tab group with two tabs with distinct content
2. Sets the first tab as active
3. Removes the first tab
4. Verifies the remaining tab's content is visible in the scene graph

All 40 existing tests continue to pass.

## Files Modified
- `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java`
- `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/core/DockTabGroupFxTest.java`

## Impact
This is a **systemic fix** that ensures content is always properly displayed when removing tabs, particularly when:
- The first tab (index 0) is active and gets removed
- Any tab removal causes a different tab to occupy the same index position

The fix prevents similar issues from occurring in other drag-and-drop scenarios where tabs are moved between groups.
