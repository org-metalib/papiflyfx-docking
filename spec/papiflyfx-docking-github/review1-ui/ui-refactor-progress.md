# GitHub Toolbar UI Refactor Progress

## Status

- Phase 1. Data and command groundwork: completed
- Phase 2. Toolbar layout refactor: completed
- Phase 3. Ref popup implementation: completed
- Phase 4. Action migration and cleanup: completed
- Phase 5. Testing, snapshots, and docs: completed

## Notes

- Added richer ref, chip, status, and popup DTOs.
- Added recent-ref persistence abstraction with a preferences-backed implementation.
- Extended the Git repository layer for tag listing, current-ref resolution, safe update, and richer checkout handling.
- Refactored `GitHubToolbarViewModel` to build a coherent toolbar snapshot and popup state while preserving existing legacy accessors.
- Rebuilt `GitHubToolbar` around repo/ref pills, contextual chips, simplified actions, overflow actions, and a transient status slot.
- Added an anchored ref popup with search, keyboard navigation, submenu support, dirty-checkout safeguards, and recent-ref tracking.
- Rewrote the GitHub FX, view-model, theme, API, recent-store, and JGit tests to match the new contract.
- Generated review snapshots under `spec/papiflyfx-docking-github/review1-ui/`.
- Validation passed with `mvn -pl papiflyfx-docking-github -am -Dtestfx.headless=true test`.
