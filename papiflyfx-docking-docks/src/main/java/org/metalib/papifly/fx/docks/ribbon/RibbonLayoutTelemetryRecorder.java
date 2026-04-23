package org.metalib.papifly.fx.docks.ribbon;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-visible telemetry recorder for deterministic ribbon layout assertions.
 */
final class RibbonLayoutTelemetryRecorder implements RibbonLayoutTelemetry {

    private final List<Event> events = new ArrayList<>();

    @Override
    public void tabRebuild(String tabId, RebuildReason reason) {
        events.add(new TabRebuildEvent(tabId, reason));
    }

    @Override
    public void groupRebuild(String groupId, RebuildReason reason) {
        events.add(new GroupRebuildEvent(groupId, reason));
    }

    @Override
    public void controlRebuild(String controlId, RebuildReason reason) {
        events.add(new ControlRebuildEvent(controlId, reason));
    }

    @Override
    public void collapseTransition(String groupId, RibbonGroupSizeMode from, RibbonGroupSizeMode to) {
        events.add(new CollapseTransitionEvent(groupId, from, to));
    }

    @Override
    public void nodeCacheHit(CacheKind kind, String id) {
        events.add(new NodeCacheHitEvent(kind, id));
    }

    @Override
    public void nodeCacheMiss(CacheKind kind, String id) {
        events.add(new NodeCacheMissEvent(kind, id));
    }

    @Override
    public void providerFailure(String providerId, RuntimeException exception) {
        events.add(new ProviderFailureEvent(providerId, exception));
    }

    @Override
    public void tabIdCollision(
        String tabId,
        String retainedLabel,
        int retainedOrder,
        String ignoredLabel,
        int ignoredOrder
    ) {
        events.add(new TabIdCollisionEvent(tabId, retainedLabel, retainedOrder, ignoredLabel, ignoredOrder));
    }

    @Override
    public void commandIdCollision(String commandId, String retainedLabel, String ignoredLabel) {
        events.add(new CommandIdCollisionEvent(commandId, retainedLabel, ignoredLabel));
    }

    List<Event> events() {
        return List.copyOf(events);
    }

    void clear() {
        events.clear();
    }

    List<TabRebuildEvent> tabRebuilds() {
        return events.stream()
            .filter(TabRebuildEvent.class::isInstance)
            .map(TabRebuildEvent.class::cast)
            .toList();
    }

    List<GroupRebuildEvent> groupRebuilds() {
        return events.stream()
            .filter(GroupRebuildEvent.class::isInstance)
            .map(GroupRebuildEvent.class::cast)
            .toList();
    }

    List<ControlRebuildEvent> controlRebuilds() {
        return events.stream()
            .filter(ControlRebuildEvent.class::isInstance)
            .map(ControlRebuildEvent.class::cast)
            .toList();
    }

    List<CollapseTransitionEvent> collapseTransitions() {
        return events.stream()
            .filter(CollapseTransitionEvent.class::isInstance)
            .map(CollapseTransitionEvent.class::cast)
            .toList();
    }

    List<NodeCacheHitEvent> cacheHits() {
        return events.stream()
            .filter(NodeCacheHitEvent.class::isInstance)
            .map(NodeCacheHitEvent.class::cast)
            .toList();
    }

    List<NodeCacheMissEvent> cacheMisses() {
        return events.stream()
            .filter(NodeCacheMissEvent.class::isInstance)
            .map(NodeCacheMissEvent.class::cast)
            .toList();
    }

    List<ProviderFailureEvent> providerFailures() {
        return events.stream()
            .filter(ProviderFailureEvent.class::isInstance)
            .map(ProviderFailureEvent.class::cast)
            .toList();
    }

    List<TabIdCollisionEvent> tabIdCollisions() {
        return events.stream()
            .filter(TabIdCollisionEvent.class::isInstance)
            .map(TabIdCollisionEvent.class::cast)
            .toList();
    }

    List<CommandIdCollisionEvent> commandIdCollisions() {
        return events.stream()
            .filter(CommandIdCollisionEvent.class::isInstance)
            .map(CommandIdCollisionEvent.class::cast)
            .toList();
    }

    sealed interface Event permits
        TabRebuildEvent,
        GroupRebuildEvent,
        ControlRebuildEvent,
        CollapseTransitionEvent,
        NodeCacheHitEvent,
        NodeCacheMissEvent,
        ProviderFailureEvent,
        TabIdCollisionEvent,
        CommandIdCollisionEvent {
    }

    record TabRebuildEvent(String tabId, RebuildReason reason) implements Event {
    }

    record GroupRebuildEvent(String groupId, RebuildReason reason) implements Event {
    }

    record ControlRebuildEvent(String controlId, RebuildReason reason) implements Event {
    }

    record CollapseTransitionEvent(String groupId, RibbonGroupSizeMode from, RibbonGroupSizeMode to) implements Event {
    }

    record NodeCacheHitEvent(CacheKind kind, String id) implements Event {
    }

    record NodeCacheMissEvent(CacheKind kind, String id) implements Event {
    }

    record ProviderFailureEvent(String providerId, RuntimeException exception) implements Event {
    }

    record TabIdCollisionEvent(
        String tabId,
        String retainedLabel,
        int retainedOrder,
        String ignoredLabel,
        int ignoredOrder
    ) implements Event {
    }

    record CommandIdCollisionEvent(String commandId, String retainedLabel, String ignoredLabel) implements Event {
    }
}
