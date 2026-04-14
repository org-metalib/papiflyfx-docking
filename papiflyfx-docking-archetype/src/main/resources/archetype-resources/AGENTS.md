# ${artifactId} Agent Team

This repository is managed by a team of specialized AI agents. Each agent has a specific domain of expertise.

## Agent Roles & Responsibilities

### 1. Application Developer (@app-dev)
- **Primary Domain**: Application features, UI layout, docking content.
- **Responsibilities**:
  - Implements new dockable content types and application features.
  - Integrates PapiflyFX framework components into the application.
  - Follows JavaFX best practices (programmatic UI, no FXML).
- **Focus Area**: `${rootArtifactId}-app` module.

### 2. Build & Runtime Engineer (@ops-engineer)
- **Primary Domain**: `pom.xml`, dependencies, CI/CD, settings.
- **Responsibilities**:
  - Manages the Maven build and dependency versions.
  - Ensures all modules compile and tests pass in headless mode.
  - Maintains CI/CD workflows and release readiness.
- **Focus Area**: Root `pom.xml`, `.github/workflows/`.

### 3. Spec & Delivery Steward (@spec-steward)
- **Primary Domain**: `spec/`, `README.md`, planning and progress documents.
- **Responsibilities**:
  - Owns task intake for ambiguous or cross-cutting work.
  - Maintains planning artifacts under `spec/`.
  - Keeps documentation aligned with the codebase.
- **Focus Area**: `spec/**`, repository-level docs.

## Agent Operating Model

- Assign exactly one lead agent per task.
- Follow the spec-first workflow for non-trivial work: research, plan, implement, validate.
- Use `spec/agents/README.md` as the shared operating protocol.

## Review Gates

- Changes to build logic or dependencies require `@ops-engineer` review.
- Changes to specs, plans, or docs require `@spec-steward` review.
- New UI features require `@app-dev` review.

## Build & Test

```bash
sdk use java 25.0.1.fx-zulu
./mvnw clean package
./mvnw -pl ${rootArtifactId}-app javafx:run
```
