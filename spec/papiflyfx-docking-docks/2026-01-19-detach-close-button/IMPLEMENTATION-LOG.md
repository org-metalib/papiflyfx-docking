# Implementation Log: Detach Close Button from Window Controls

**Date:** 2026-01-19
**Specification:** `2026-01-19-detach-close-button.md`

## Summary

Moved the close button from the right-side window control group to be positioned immediately after the dock title in `DockPane`, creating a consistent UX pattern that matches the tab close button behavior in `DockTabGroup`.

## Changes Made

### File: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockPane.java`

#### 1. Updated Class Javadoc (line 18)

**Before:**
```java
* Button order: [Icon] [Title] ─────────── [Float/Dock] [Minimize] [Maximize] [Close]
```

**After:**
```java
* Button order: [Icon] [Title] [Close] ─────────── [Float/Dock] [Minimize] [Maximize]
```

#### 2. Restructured Title Bar Layout (lines 65-91)

**Before:**
```java
// Button container with proper spacing
HBox buttonContainer = new HBox(4);
buttonContainer.setAlignment(Pos.CENTER_RIGHT);
buttonContainer.getChildren().addAll(floatButton, minimizeButton, maximizeButton, closeButton);

// Spacer to push buttons to the right
Region spacer = new Region();
HBox.setHgrow(spacer, Priority.ALWAYS);

// Icon container
HBox iconContainer = new HBox();
iconContainer.setAlignment(Pos.CENTER_LEFT);
leaf.metadataProperty().addListener((obs, oldVal, newVal) -> {
    iconContainer.getChildren().clear();
    if (newVal != null && newVal.icon() != null) {
        iconContainer.getChildren().add(newVal.icon());
    }
});

titleBar = new HBox(4, iconContainer, titleLabel, spacer, buttonContainer);
```

**After:**
```java
// Icon container
HBox iconContainer = new HBox();
iconContainer.setAlignment(Pos.CENTER_LEFT);
leaf.metadataProperty().addListener((obs, oldVal, newVal) -> {
    iconContainer.getChildren().clear();
    if (newVal != null && newVal.icon() != null) {
        iconContainer.getChildren().add(newVal.icon());
    }
});

// Title group: icon + title + close button (close button always near title)
HBox titleGroup = new HBox(4);
titleGroup.setAlignment(Pos.CENTER_LEFT);
titleGroup.getChildren().addAll(iconContainer, titleLabel, closeButton);

// Window control group: float, minimize, maximize only
HBox windowControlGroup = new HBox(4);
windowControlGroup.setAlignment(Pos.CENTER_RIGHT);
windowControlGroup.getChildren().addAll(floatButton, minimizeButton, maximizeButton);

// Spacer to push window controls to the right
Region spacer = new Region();
HBox.setHgrow(spacer, Priority.ALWAYS);

titleBar = new HBox(4, titleGroup, spacer, windowControlGroup);
```

#### 3. Added `setCloseButtonVisible` Method (lines 391-397)

Added a new method for controlling close button visibility, consistent with existing visibility methods for other buttons:

```java
/**
 * Sets the visibility of the close button.
 */
public void setCloseButtonVisible(boolean visible) {
    closeButton.setVisible(visible);
    closeButton.setManaged(visible);
}
```

## Visual Result

### Before
```
┌─────────────────────────────────────────────────────────┐
│ [Icon] Chart Panel                    [Float][Min][Max][X] │
└─────────────────────────────────────────────────────────┘
```

### After
```
┌─────────────────────────────────────────────────────────┐
│ [Icon] Chart Panel [X]                    [Float][Min][Max] │
└─────────────────────────────────────────────────────────┘
```

## Build Verification

```
mvn compile
...
[INFO] BUILD SUCCESS
```

All 31 source files in `papiflyfx-docks` compiled successfully with no errors.

## Testing Recommendations

1. Run `mvn javafx:run -pl papiflyfx-docks` to launch the demo application
2. Verify close button appears after title in single dock panels
3. Verify close button click still closes the panel correctly
4. Verify tab close buttons in `DockTabGroup` remain unchanged (they already have close buttons next to tab titles)
5. Verify drag behavior on title bar still works
6. Verify double-click to maximize still works
7. Test with both dark and light themes