# Ribbon Provider Authoring Guide

**Status:** current for Ribbon 5 Phase 2  
**Primary audience:** feature modules that contribute tabs through `papiflyfx-docking-api`  
**Runtime host:** `papiflyfx-docking-docks` discovers providers through `ServiceLoader`

## Lead And Review

- Lead for shared contracts: `@core-architect`
- Lead for feature-provider implementations: `@feature-dev`
- Required reviewers for new providers: `@feature-dev`, `@qa-engineer`; add `@core-architect` when a shared API/SPI changes
- Build/sample scaffold changes also require `@ops-engineer`

## Dependency Boundary

Feature modules should depend on `papiflyfx-docking-api` for provider contracts. Do not import `org.metalib.papifly.fx.docks.ribbon.*` from feature production code; those classes are runtime implementation details.

## Provider Registration

1. Implement `org.metalib.papifly.fx.api.ribbon.RibbonProvider`.
2. Return a stable provider id from `id()`, preferably `<module>.ribbon.provider`.
3. Register the implementation in:

```text
src/main/resources/META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider
```

The descriptor contains one implementation class name per line.

## Stable Ids

Use stable dotted identifiers:

- Provider id: `<module>.ribbon.provider`
- Tab id: `<module>` or `<module>-<context>`
- Group id: `<module>-<group>`
- Command id: `<module>.ribbon.<action>`

Hosts persist selected tab ids and Quick Access Toolbar command ids. Changing a published tab or command id breaks session continuity for that provider-owned surface. Duplicate tab and command ids are first-wins and should emit runtime diagnostics, so collisions must be treated as provider defects.

## Tab, Group, And Command Construction

A provider returns `RibbonTabSpec` values from `getTabs(RibbonContext context)`.

- Keep tab and group lists deterministic for a given context.
- Use `contextual=true` only for tabs that depend on active content.
- Use `visibleWhen` for tab visibility, not for command enablement.
- Use `PapiflyCommand` for executable actions and choose stable labels/tooltips/icons.
- Provider command state may be recomputed on every `getTabs(context)` call; the runtime canonicalizes command identity by id and projects later `enabled` and `selected` snapshots onto the canonical command.

Do not cache JavaFX controls in providers. Providers describe command surfaces; the runtime owns materialization.

## Icon Conventions

Prefer `RibbonIconHandle.of("octicon:<name>")` for Octicon-backed commands. Resource icons should use a stable module-owned path and should be treated as provider API once published because commands may appear in the Quick Access Toolbar.

Always supply enough command text for accessible names. Icon-only rendering is a runtime presentation choice; providers still own meaningful labels and tooltips.

## Capability Lookup

Executable integrations belong in `RibbonContext#capability(Class)`.

Supported contribution rules:

1. Root-node action interface: the active content root may implement a public feature action interface such as `HugoRibbonActions`.
2. Explicit contribution: the active content root may implement `RibbonCapabilityContributor` and return one or more capability entries, for example a nested controller under `HugoRibbonActions.class`.

The explicit contract is preferred when the JavaFX root is only a view and behavior lives in a controller. The root-node convention remains supported for simple content and existing providers.

Providers must degrade gracefully when a capability is absent: tabs may stay visible, but commands should be disabled and callbacks should no-op when no action object exists.

Floating windows use the same rules. When the floating leaf is active, `DockManager` builds the ribbon context from that floating content, sets `RibbonContextAttributes.FLOATING_KEY` to `true`, and exposes the same capabilities as docked content.

## Contextual Metadata

Metadata belongs in attributes, preferably through typed keys:

- `RibbonContextAttributes.CONTENT_DOMAIN_KEY`: owning feature domain, such as `hugo`, `github`, `code`, `tree`, or `media`
- `RibbonContextAttributes.CONTENT_KIND_KEY`: content kind, such as `markdown`, `java`, `image`, or a provider-owned value such as `hugo.markdown`
- `RibbonContextAttributes.CONTENT_FACTORY_ID_KEY`: factory id for restored content
- `RibbonContextAttributes.DOCK_TITLE_KEY`: active dock title
- `RibbonContextAttributes.FLOATING_KEY`: whether the active leaf is floating

The raw string constants remain compatible for existing code. New provider code should use typed key overloads such as `context.attribute(RibbonContextAttributes.CONTENT_DOMAIN_KEY)`.

Active content may publish explicit metadata by implementing `RibbonAttributeContributor`. Providers should prefer explicit metadata before falling back to legacy title, path, factory-id, or type-key heuristics.

Provider-owned keys should use a stable dotted namespace such as `code.editor.language`.

## Quick Access Toolbar

The Quick Access Toolbar persists command ids, not command objects. A contextual command id may remain pinned while its tab is hidden; the runtime resolves it again when the provider contributes that command in a later context.

Provider responsibilities:

- Keep QAT-eligible command ids stable.
- Keep labels/icons semantically stable for a command id.
- Do not reuse an id for a different action.
- Ensure hidden contextual commands can reappear with the same id and compatible command metadata.

## Minimal Provider Shape

```java
public final class ExampleRibbonProvider implements RibbonProvider {
    @Override
    public String id() {
        return "example.ribbon.provider";
    }

    @Override
    public List<RibbonTabSpec> getTabs(RibbonContext context) {
        Optional<ExampleActions> actions = context.capability(ExampleActions.class);
        PapiflyCommand refresh = new PapiflyCommand(
            "example.ribbon.refresh",
            "Refresh",
            "Refresh the active example content",
            RibbonIconHandle.of("octicon:sync"),
            RibbonIconHandle.of("octicon:sync"),
            new MutableBoolState(actions.map(ExampleActions::canRefresh).orElse(false)),
            null,
            () -> actions.ifPresent(ExampleActions::refresh)
        );

        return List.of(new RibbonTabSpec(
            "example",
            "Example",
            100,
            true,
            ribbonContext -> ribbonContext.attribute(RibbonContextAttributes.CONTENT_DOMAIN_KEY)
                .map("example"::equals)
                .orElse(false),
            List.of(new RibbonGroupSpec(
                "example-actions",
                "Actions",
                0,
                0,
                null,
                List.of(new RibbonButtonSpec(refresh))
            ))
        ));
    }
}
```

## Test Checklist

Add focused provider tests before relying on sample coverage.

- Provider structure: tab ids, group ids, command ids, labels, order, and contextual flags.
- No-capability state: commands are disabled and callbacks do not throw.
- Capability state: commands enable and route to the action object.
- Explicit metadata: contextual tabs use `CONTENT_DOMAIN_KEY` or `CONTENT_KIND_KEY` when present.
- Legacy fallback: existing providers that keep path/title/factory heuristics have table-style tests for each fallback branch.
- Floating context: a mounted `RibbonDockHost` resolves the active floating leaf, `FLOATING_KEY` is true, and provider capabilities update command state.
- QAT: contextual command ids survive while hidden and resolve again when visible.
- Diagnostics: duplicate ids or provider failures are covered where the provider introduces collision risk.

## Archetype Scaffold Decision

Docs are sufficient for Ribbon 5 Phase 2 because the API/runtime path is additive and existing sample modules already demonstrate mounted ribbon hosts. A generated-app scaffold remains useful, but it should be handled by `@ops-engineer` in a later Phase 3/5 task so archetype templates can include a minimal provider, ServiceLoader descriptor, and generated-app assertion without coupling that work to the core API change.
