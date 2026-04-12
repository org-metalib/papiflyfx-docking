# Fix Settings Demo UI — Review 0

Settings refactoring broke visual appearance: CSS token variables undefined, hardcoded dark-only colors in SamplesApp.

## Prompts

| File | Agent | Scope | Parallel |
|------|-------|-------|----------|
| `.prompt-0.md` | — | Overview, root cause, agent split | — |
| `.prompt-1.md` | `@ui-ux-designer` | `papiflyfx-docking-settings` — inject tokens, standardize sub-components | Yes (with Agent 2) |
| `.prompt-2.md` | `@feature-dev` | `papiflyfx-docking-samples` — make SamplesApp theme-aware | Yes (with Agent 1) |
| `.prompt-3.md` | `@qa-validator` | Both modules — compile, test, static checklist | After Agents 1+2 |
