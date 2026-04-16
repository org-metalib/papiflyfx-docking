# PapiflyFX Docking Archetype Review Summary

**Date**: 2026-04-15  
**Lead**: @spec-steward  
**Primary reviewer lens**: @ops-engineer

## Scope

Reviewed:

- `papiflyfx-docking-bom`
- `papiflyfx-docking-archetype`
- design and delivery docs under `spec/papiflyfx-docking-archetype`

## Current Implementation Snapshot

The implementation is real and mostly aligned with the original plan:

- `papiflyfx-docking-bom` exists as a child `pom` module and manages the published PapiflyFX framework artifacts through `dependencyManagement`.
- `papiflyfx-docking-archetype` exists as a `maven-archetype` module and ships:
  - `META-INF/maven/archetype-metadata.xml`
  - a generated root `pom.xml`
  - an `app` module template with `App.java`, `AppLauncher.java`, and `AppTest.java`
  - generated `AGENTS.md`, `CLAUDE.md`, `.github/copilot-instructions.md`, and `spec/agents/README.md`
  - a generated CI workflow and Maven wrapper properties
- The Java source templates substitute package names correctly.
- The BOM import, JavaFX dependency management, platform profiles, headless TestFX defaults, and Surefire configuration are present in the generated root/app POM templates.

## Validation Performed

### Module-level validation

Command:

```bash
./mvnw -pl papiflyfx-docking-bom,papiflyfx-docking-archetype -am test -Dtestfx.headless=true
```

Result:

- `BUILD SUCCESS`
- This validates the reactor build for the BOM and archetype modules.
- It does **not** validate generated-project correctness.

### Install + archetype packaging validation

Command:

```bash
./mvnw -pl papiflyfx-docking-bom,papiflyfx-docking-archetype -am install \
  -Dmaven.repo.local=/tmp/papiflyfx-archetype-m2
```

Result:

- `BUILD SUCCESS`
- The archetype JAR is produced and installed into a temporary local Maven repo.
- Maven logs a warning during archetype integration-test execution:

```text
No Archetype IT projects: root 'projects' directory not found.
```

### End-to-end generation validation

Generate:

```bash
mvn -o -Dmaven.repo.local=/tmp/papiflyfx-archetype-m2 \
  org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeCatalog=local \
  -DarchetypeGroupId=org.metalib.papifly.docking \
  -DarchetypeArtifactId=papiflyfx-docking-archetype \
  -DarchetypeVersion=0.0.17-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=papiflyfx-archetype-review-app \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=com.example.reviewapp \
  -DinteractiveMode=false
```

Validate generated project:

```bash
cd /tmp/papiflyfx-archetype-review-app
mvn -o -Dmaven.repo.local=/tmp/papiflyfx-archetype-m2 -q validate
```

Result:

- archetype generation succeeds
- generated project validation fails immediately:

```text
Child module /private/tmp/papiflyfx-archetype-review-app/papiflyfx-archetype-review-app-app
of /private/tmp/papiflyfx-archetype-review-app/pom.xml does not exist
```

## Findings

### 1. High: generated root POM is invalid

The archetype descriptor generates the child module into `app/`, but the root POM template hardcodes `${rootArtifactId}-app` in `<modules>`.

Evidence:

- `papiflyfx-docking-archetype/src/main/resources/archetype-resources/pom.xml`
  - line 14: `<module>${rootArtifactId}-app</module>`
- `papiflyfx-docking-archetype/src/main/resources/META-INF/maven/archetype-metadata.xml`
  - line 61: `<module id="${rootArtifactId}-app" dir="app" name="${rootArtifactId}-app">`
- generated project:
  - root `pom.xml` contains both `papiflyfx-archetype-review-app-app` and `app`
  - only `app/` exists on disk

Impact:

- a freshly generated project is not buildable
- the current archetype cannot be considered complete

### 2. Medium: generated Markdown docs lose headings because of Velocity comment parsing

The filtered Markdown templates use raw `##` headings. Velocity treats `##` as a line comment, so the headings disappear in generated output.

Observed in generated files:

- `README.md`
- `AGENTS.md`
- `CLAUDE.md`
- `spec/agents/README.md`

Impact:

- generated documentation loses structure and becomes much harder to read
- the problem will recur until the templates escape literal `#` characters or use a different heading style

### 3. Medium: documentation and examples describe the wrong module path

Repository docs and generated docs assume the app module directory is `${rootArtifactId}-app` / `my-app-app`, while the archetype actually creates the module under `app/`.

Evidence:

- `papiflyfx-docking-archetype/README.md`
  - line 33: `./mvnw -pl my-app-app javafx:run`
  - lines 56-63: generated structure shows `my-app-app/`
  - line 114: tells the user to edit `my-app-app/pom.xml`
- `papiflyfx-docking-archetype/src/main/resources/archetype-resources/README.md`
  - line 45: `./mvnw -pl ${rootArtifactId}-app javafx:run`
  - line 67: shows `${rootArtifactId}-app/` as the directory

Impact:

- even after the module-path defect is fixed, the docs remain misleading unless the naming is normalized

### 4. Medium: archetype integration-test coverage is missing

`mvn install` reports:

```text
No Archetype IT projects: root 'projects' directory not found.
```

Impact:

- the broken generated root POM escaped because no generated-project regression test exists
- the archetype should add `src/test/resources/projects/basic/` with at least one happy-path verification

### 5. Low: generated README assumes a `LICENSE` file that is not generated

The generated `README.md` links to `LICENSE`, but the archetype does not currently generate a `LICENSE` file.

Impact:

- minor documentation defect
- not a build blocker

## What Looks Good

- BOM module shape is sound and consistent with the design docs.
- The archetype module builds, packages, and installs correctly.
- Java package substitution works for source and test classes.
- The generated `app/pom.xml` is internally coherent once the root module-path issue is fixed.
- The CI workflow, wrapper properties, and agent-doc set are present and structurally reasonable.

## Recommended Execution Order

1. Fix the generated root module path mismatch.
   - Keep the physical module directory as `app/`.
   - Update the root template to reference `app` in `<modules>`.

2. Normalize docs around directory vs artifact identity.
   - Directory: `app/`
   - Child artifactId: `${rootArtifactId}-app` if you want a unique Maven coordinate
   - Commands should use either `-pl app` or `-pl :${rootArtifactId}-app`, but one convention should be documented consistently.

3. Fix Markdown template rendering.
   - Escape literal heading markers in filtered templates, or replace `##`/`###` headings with a Velocity-safe Markdown style.

4. Add an archetype integration test.
   - Add `src/test/resources/projects/basic/`
   - Validate generated structure and at least `validate` phase success

5. Rerun install, generate, validate, and only then refresh README/progress docs.

## Suggested Acceptance Criteria

- `./mvnw -pl papiflyfx-docking-bom,papiflyfx-docking-archetype -am install` succeeds
- `mvn ... archetype:generate` creates a project with exactly one root module path: `app`
- generated Markdown docs preserve headings
- `mvn validate` succeeds in the generated project
- archetype ITs run as part of `./mvnw -pl papiflyfx-docking-archetype verify`
