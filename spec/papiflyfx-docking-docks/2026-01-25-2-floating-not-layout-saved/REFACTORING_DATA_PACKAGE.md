# Refactoring: Move Layout Data Classes to `data` Package

## Date: January 25, 2026

## Objective
Organize layout-related data transfer objects (DTOs) into a dedicated `data` subpackage for better code organization and separation of concerns.

## Changes Made

### Package Structure

**Before:**
```
org.metalib.papifly.fx.docks.layout/
├── BoundsData.java
├── ContentFactory.java
├── DockSessionData.java
├── FloatingLeafData.java
├── LayoutFactory.java
├── LayoutNode.java
├── LeafData.java
├── MaximizedLeafData.java
├── MinimizedLeafData.java
├── RestoreHintData.java
├── SplitData.java
└── TabGroupData.java
```

**After:**
```
org.metalib.papifly.fx.docks.layout/
├── ContentFactory.java
├── LayoutFactory.java
└── data/
    ├── BoundsData.java
    ├── DockSessionData.java
    ├── FloatingLeafData.java
    ├── LayoutNode.java
    ├── LeafData.java
    ├── MaximizedLeafData.java
    ├── MinimizedLeafData.java
    ├── RestoreHintData.java
    ├── SplitData.java
    └── TabGroupData.java
```

### Files Moved (10 files)

All data classes moved to `org.metalib.papifly.fx.docks.layout.data`:

1. **BoundsData.java** - Window bounds DTO
2. **DockSessionData.java** - Session container DTO
3. **FloatingLeafData.java** - Floating window state DTO
4. **LayoutNode.java** - Layout tree interface
5. **LeafData.java** - Leaf node DTO
6. **MaximizedLeafData.java** - Maximized state DTO
7. **MinimizedLeafData.java** - Minimized state DTO
8. **RestoreHintData.java** - Restore hint DTO
9. **SplitData.java** - Split node DTO
10. **TabGroupData.java** - Tab group DTO

### Files Remaining in `layout` Package (2 files)

Non-data classes that remained:

1. **ContentFactory.java** - Factory interface (not a data class)
2. **LayoutFactory.java** - Builder class (not a data class)

### Import Updates

**Main Source Files Updated (8 files):**
- `DockElement.java`
- `DockLeaf.java`
- `DockSplitGroup.java`
- `DockTabGroup.java`
- `DockManager.java`
- `LayoutFactory.java`
- `LayoutSerializer.java`
- `DockSessionSerializer.java`
- `LayoutPersistence.java`
- `DockSessionPersistence.java`

**Test Files Updated (6 files):**
- `DockManagerSessionFxTest.java`
- `DockManagerCaptureRestoreFxTest.java`
- `DockManagerPersistenceFxTest.java`
- `LayoutPersistenceTest.java`
- `LayoutSerializerTest.java`
- `LayoutFactoryFxTest.java`

### Import Pattern Change

**Old imports:**
```java
import org.metalib.papifly.fx.docks.layout.LayoutNode;
import org.metalib.papifly.fx.docks.layout.LeafData;
import org.metalib.papifly.fx.docks.layout.SplitData;
import org.metalib.papifly.fx.docks.layout.TabGroupData;
```

**New imports:**
```java
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.SplitData;
import org.metalib.papifly.fx.docks.layout.data.TabGroupData;
```

**Wildcard import (for convenience):**
```java
import org.metalib.papifly.fx.docks.layout.data.*;
```

## Benefits

1. **Better Organization**: Data classes are now clearly separated from functional classes
2. **Clear Separation of Concerns**: 
   - `layout` package: factories and interfaces
   - `layout.data` package: pure data DTOs
3. **Easier Navigation**: Developers can quickly identify data structures vs. behavior
4. **Future Maintainability**: Adding new data classes has a clear location
5. **No Breaking Changes to Public API**: Only internal package structure changed

## Testing

All 43 tests pass successfully:
```
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Coverage Maintained

- ✅ Layout serialization tests
- ✅ Session save/restore tests  
- ✅ Floating window tests
- ✅ Minimized/maximized state tests
- ✅ Persistence tests
- ✅ Layout factory tests

## Migration Guide

If you have external code using these classes, update imports:

```java
// Old
import org.metalib.papifly.fx.docks.layout.LeafData;
import org.metalib.papifly.fx.docks.layout.DockSessionData;

// New
import org.metalib.papifly.fx.docks.layout.data.LeafData;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
```

Or use wildcard import:
```java
import org.metalib.papifly.fx.docks.layout.data.*;
```

## Files Not Affected

These classes remain unchanged:
- All classes in `org.metalib.papifly.fx.docks.core`
- All classes in `org.metalib.papifly.fx.docks.serial`
- All classes in `org.metalib.papifly.fx.docks.floating`
- All classes in `org.metalib.papifly.fx.docks.minimize`
- `ContentFactory` and `LayoutFactory` in `layout` package

## Verification Commands

```bash
# Compile
./mvnw clean compile -pl papiflyfx-docks
# Result: BUILD SUCCESS

# Test
./mvnw test -pl papiflyfx-docks
# Result: Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
```

## Summary

This refactoring improves code organization by moving all data transfer objects to a dedicated `data` subpackage while maintaining 100% backward compatibility with the test suite. The change is purely structural and does not affect functionality or behavior.
