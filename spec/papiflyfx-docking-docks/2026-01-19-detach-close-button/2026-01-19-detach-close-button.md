# Detach Close Button from Window Controls

## Overview

Move the close button from the right-side window control group to be positioned immediately after the dock title, creating a consistent UX pattern across both single docks and tab groups.

## Current State

### DockPane Layout (single dock)
```
┌────────────────────────────────────────────────────────────────┐
│ [Icon] [Title] ───────────────── [Float][Minimize][Maximize][Close] │
└────────────────────────────────────────────────────────────────┘
```

### DockTabGroup Layout (multiple tabs)
```
┌────────────────────────────────────────────────────────────────┐
│ [Tab1][X] [Tab2][X] [Tab3][X] ────── [Float][Minimize][Maximize] │
└────────────────────────────────────────────────────────────────┘
```

**Current Behavior:**
- In `DockPane`: Close button (16x16) is grouped with window control buttons on the right
- In `DockTabGroup`: Each tab has its own small close button (12x12) next to the tab title; window controls (Float/Minimize/Maximize) are on the right without a close button

## Proposed State

### DockPane Layout (single dock)
```
┌────────────────────────────────────────────────────────────────┐
│ [Icon] [Title][X] ─────────────────── [Float][Minimize][Maximize] │
└────────────────────────────────────────────────────────────────┘
```

### DockTabGroup Layout (multiple tabs)
```
┌────────────────────────────────────────────────────────────────┐
│ [Tab1][X] [Tab2][X] [Tab3][X] ────── [Float][Minimize][Maximize] │
└────────────────────────────────────────────────────────────────┘
```
(Tab group layout remains unchanged - already has close buttons with titles)

**Proposed Behavior:**
- Close button is always positioned immediately after the title/tab name
- Window control group contains only: Float, Minimize, Maximize
- Consistent visual pattern: title is always followed by its close button

## Files to Modify

| File | Changes |
|------|---------|
| `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockPane.java` | Restructure title bar layout to place close button after title |

## Implementation Steps

### Step 1: Restructure DockPane Title Bar Layout

**Current layout construction (lines 65-87):**
```java
// Button container with all 4 buttons
HBox buttonContainer = new HBox(4);
buttonContainer.setAlignment(Pos.CENTER_RIGHT);
buttonContainer.getChildren().addAll(floatButton, minimizeButton, maximizeButton, closeButton);

Region spacer = new Region();
HBox.setHgrow(spacer, Priority.ALWAYS);

titleBar = new HBox(4, iconContainer, titleLabel, spacer, buttonContainer);
```

**New layout construction:**
```java
// Title group: icon + title + close button
HBox titleGroup = new HBox(4);
titleGroup.setAlignment(Pos.CENTER_LEFT);
titleGroup.getChildren().addAll(iconContainer, titleLabel, closeButton);

// Window control group: float, minimize, maximize only
HBox windowControlGroup = new HBox(4);
windowControlGroup.setAlignment(Pos.CENTER_RIGHT);
windowControlGroup.getChildren().addAll(floatButton, minimizeButton, maximizeButton);

Region spacer = new Region();
HBox.setHgrow(spacer, Priority.ALWAYS);

titleBar = new HBox(4, titleGroup, spacer, windowControlGroup);
```

### Step 2: Adjust Close Button Styling (Optional)

Consider reducing close button size to match tab close buttons for visual consistency:
- Current: 16x16 with 8x8 X icon
- Tab style: 12x12 with 6x6 X icon

**Option A:** Keep 16x16 for visibility (recommended for accessibility)
**Option B:** Reduce to 12x12 for visual consistency with tabs

### Step 3: Update Button Visibility Methods

Ensure `setCloseButtonVisible()` method (if added) works correctly with the new layout structure.

Current visibility methods in DockPane (lines 369-388):
```java
public void setFloatButtonVisible(boolean visible)
public void setMinimizeButtonVisible(boolean visible)
public void setMaximizeButtonVisible(boolean visible)
```

Add similar method for close button if not already present:
```java
public void setCloseButtonVisible(boolean visible) {
    closeButton.setVisible(visible);
    closeButton.setManaged(visible);
}
```

## Visual Comparison

### Before
```
┌─────────────────────────────────────────────────────────┐
│ [📄] Chart Panel                    [⤢][▁][□][✕]       │
└─────────────────────────────────────────────────────────┘
```

### After
```
┌─────────────────────────────────────────────────────────┐
│ [📄] Chart Panel [✕]                    [⤢][▁][□]      │
└─────────────────────────────────────────────────────────┘
```

## Benefits

1. **Consistency**: Close button always appears next to the title in both DockPane and DockTabGroup tabs
2. **Discoverability**: Users can quickly identify how to close a specific panel
3. **Semantic grouping**: Window controls (float/minimize/maximize) are grouped separately from content-specific actions (close)
4. **Reduced visual clutter**: Fewer buttons in the right-side control group

## Testing

1. Verify close button appears after title in single dock panels
2. Verify close button click still triggers `leaf.requestClose()`
3. Verify tab close buttons in DockTabGroup remain unchanged
4. Verify drag behavior on title bar still works (cursor should show MOVE)
5. Verify double-click to maximize still works
6. Test with themes (dark/light) for proper styling
