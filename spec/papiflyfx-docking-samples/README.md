# PapiflyFX Docking Samples Specification

This directory contains the design and specification documents for **papiflyfx-docking-samples**, a standalone runnable JavaFX application that demonstrates the full capability surface of the docking framework.

## Documents

- [**spec.md**](spec.md): Core architectural specification, sample catalog, and acceptance criteria.
- [**implementation.md**](implementation.md): Phased delivery plan, milestones, and validation strategy.

## Overview

`papiflyfx-docking-samples` is not a library — it is consumed by developers learning the API and by CI as a smoke-test harness. It covers two top-level sample categories:

1. **Docks samples** — layout docking configurations (split, tabs, floating, minimize, session persistence)
2. **Docking code samples** — code editor panels per supported language:
   1. Markdown Code editor
   2. Java Code editor
   3. Javascript Code editor
   4. Json Code editor

## Target Module

This spec targets creation of a separate Maven module:
- `papiflyfx-docking-samples`

Integration boundary:
- `papiflyfx-docking-samples` depends on `papiflyfx-docking-docks` and `papiflyfx-docking-code`.
- Neither `papiflyfx-docking-docks` nor `papiflyfx-docking-code` depends on `papiflyfx-docking-samples`.
- The module is added to the root aggregator `pom.xml` but is never published to Maven Central.

## Module Structure

```
papiflyfx-docking-samples/
├── pom.xml
└── src/
    ├── main/java/org/metalib/papifly/fx/samples/
    │   ├── SamplesApp.java               # JavaFX Application, top-level stage
    │   ├── SampleLauncher.java           # main() entry point (module-safe trampoline)
    │   ├── catalog/
    │   │   └── SampleCatalog.java        # registry of all sample descriptors
    │   ├── docks/
    │   │   ├── BasicSplitSample.java     # horizontal/vertical split
    │   │   ├── NestedSplitSample.java    # multi-level nested splits
    │   │   ├── TabGroupSample.java       # tab groups with close/float/minimize
    │   │   ├── FloatingSample.java       # detach to floating window
    │   │   ├── MinimizeSample.java       # minimize panel to bar and restore
    │   │   └── PersistSample.java        # save/restore JSON session
    │   └── code/
    │       ├── MarkdownEditorSample.java
    │       ├── JavaEditorSample.java
    │       ├── JavaScriptEditorSample.java
    │       └── JsonEditorSample.java
    └── test/java/org/metalib/papifly/fx/samples/
        └── SamplesSmokeTest.java         # headless TestFX, launches each sample
```

## Run

```bash
mvn javafx:run -pl papiflyfx-docking-samples
```

## Headless smoke test

```bash
mvn -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test
```