# Plan — Docking Ribbon 2 (Decoupling + Hardening)

**Priority:** P1 (High)  
**Lead Agent:** @core-architect  
**Required Reviewers:** @ui-ux-designer, @feature-dev, @qa-engineer, @ops-engineer, @spec-steward  
**Compatibility Stance:** Compatibility is not a concern; breaking changes are acceptable.  
**Workflow:** research complete -> phased implementation -> validation

## Goal

Ship a second ribbon iteration that prioritizes architectural boundaries, runtime stability, and extension safety over compatibility. Existing contracts and persisted ribbon/session shapes may be changed directly.

## Baseline

Iteration 1 delivered a working ribbon shell with provider SPI, contextual tabs, adaptive sizing, and persistence. The follow-up work focuses on architectural debt and long-term maintainability identified in `research.md`.

## Scope

### In scope

1. Remove UI-framework leakage from shared ribbon command contracts.
2. Replace node-casting provider coupling with typed contextual capability resolution.
3. Introduce stable command registry and ID-first QAT management.
4. Add explicit tab/group merge conflict diagnostics and deterministic policies.
5. Improve adaptive layout strategy for lower churn/flicker risk.
6. Clarify reduction priority semantics and update docs/examples.
7. Strengthen persistence extension model and serializer validation.
8. Expand tests for new invariants and regression risks.
9. Execute intentionally breaking cleanup where it reduces complexity.

### Out of scope

1. End-user ribbon customization UI.
2. Full keytip/screentip/gallery feature set.
3. Major visual redesign of ribbon look-and-feel.
4. Migration of all content modules in one sweep beyond touched provider contracts.

## Impacted modules

| Module | Planned responsibility |
| --- | --- |
| `papiflyfx-docking-api` | command/context contract refactor, SPI documentation updates |
| `papiflyfx-docking-docks` | manager/runtime refactor, adaptive layout improvements, persistence extensions |
| `papiflyfx-docking-github` | provider migration to capability-based context + command lifecycle updates |
| `papiflyfx-docking-hugo` | provider migration to capability-based context + command lifecycle updates |
| `papiflyfx-docking-samples` | sample host updates for new command/context plumbing |
| `spec/**` | plan/progress/validation tracking and API usage docs |

## Key invariants

1. Compatibility with iteration-1 ribbon APIs/session payloads is not required.
2. Ribbon provider onboarding remains ServiceLoader-based and modular.
3. No command should become unreachable due to adaptive collapse behavior.
4. Contextual tabs must remain deterministic for the same context inputs.
5. Broken downstream contracts are acceptable if explicitly documented.

## Phased execution

### Phase 1 — API contracts

1. Introduce UI-neutral command state abstractions.
2. Add typed ribbon context capability model and deprecate raw-node lookup path.
3. Document merge/priority semantics with unambiguous naming and examples.

### Phase 2 — Runtime command architecture

1. Add `CommandRegistry` in ribbon runtime with stable command IDs.
2. Refactor `RibbonManager` to materialize tab structure separately from command registry.
3. Convert QAT runtime state to ID-first model with robust restore behavior.

### Phase 3 — Layout and rendering efficiency

1. Refactor adaptive layout policy to reduce repeated control reconstruction.
2. Add layout instrumentation hooks for deterministic test assertions.
3. Add SVG-capable icon loading path with graceful fallback.

### Phase 4 — Persistence extension generalization

1. Introduce namespaced extension payload model in dock session data.
2. Replace prior ribbon payload shape as needed; migration bridge is optional.
3. Add serializer shape validation helpers and malformed-input tolerance coverage.

### Phase 5 — Provider migration and test closure

1. Update GitHub and Hugo providers to typed context capabilities and stable command lifecycle.
2. Expand unit/Fx tests for:
   - collision diagnostics
   - context capability resolution
   - QAT restore with hidden contextual tabs
   - adaptive layout churn limits
   - extension payload round-trips
3. Update docs/README snippets for contributors.

## Acceptance criteria

- [ ] API contracts are UI-neutral at the ribbon SPI boundary.
- [ ] Providers no longer require `ACTIVE_CONTENT_NODE` casting for action resolution.
- [ ] QAT restore works for persisted command IDs regardless of tab visibility at restore time.
- [ ] Duplicate tab/group ID conflicts produce explicit diagnostics.
- [ ] Adaptive layout passes regression suite with no command loss and improved stability.
- [ ] Session serialization supports extensible payloads without requiring backward compatibility.
- [ ] GitHub/Hugo providers pass updated contract and behavior tests.
- [ ] Specs/docs describe new contribution model and migration guidance.

## Validation strategy

1. Compile targeted modules:
   - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks,papiflyfx-docking-github,papiflyfx-docking-hugo,papiflyfx-docking-samples -am compile`
2. Run headless tests:
   - `./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-github,papiflyfx-docking-hugo,papiflyfx-docking-samples -am -Dtestfx.headless=true test`
3. Full package verification:
   - `./mvnw clean package`
4. Manual checks in samples app:
   - contextual-tab transitions
   - QAT persistence with provider availability changes
   - resize behavior from wide to narrow and back

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| API changes disrupt existing providers | Treat this as planned breakage; document exact migration steps instead of preserving legacy paths |
| Persistence model replacement discards old session restore path | Publish explicit cutover notes and test only the new schema |
| Layout refactor causes UI regressions | Add deterministic Fx tests plus manual sample validation |
| Capability model too narrow for future modules | Start with core capabilities and support namespaced extension keys |

## Handoff notes

Lead Agent: `@core-architect`  
Task Scope: ribbon architectural hardening and contract decoupling  
Impacted Modules: `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, `papiflyfx-docking-samples`, `spec/**`  
Files Changed: `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/plan.md`  
Required Reviewer: `@spec-steward`
