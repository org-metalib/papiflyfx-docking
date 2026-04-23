package org.metalib.papifly.fx.docks.ribbon;

/**
 * Internal ribbon layout telemetry sink used by tests to assert deterministic
 * cache and adaptive-layout behavior.
 */
interface RibbonLayoutTelemetry {

    RibbonLayoutTelemetry NO_OP = new RibbonLayoutTelemetry() {
    };

    static RibbonLayoutTelemetry noop() {
        return NO_OP;
    }

    default void tabRebuild(String tabId, RebuildReason reason) {
    }

    default void groupRebuild(String groupId, RebuildReason reason) {
    }

    default void controlRebuild(String controlId, RebuildReason reason) {
    }

    default void collapseTransition(String groupId, RibbonGroupSizeMode from, RibbonGroupSizeMode to) {
    }

    default void nodeCacheHit(CacheKind kind, String id) {
    }

    default void nodeCacheMiss(CacheKind kind, String id) {
    }

    default void providerFailure(String providerId, RuntimeException exception) {
    }

    default void tabIdCollision(
        String tabId,
        String retainedLabel,
        int retainedOrder,
        String ignoredLabel,
        int ignoredOrder
    ) {
    }

    default void commandIdCollision(String commandId, String retainedLabel, String ignoredLabel) {
    }

    enum RebuildReason {
        INITIAL,
        STRUCTURAL,
        COLLAPSE,
        THEME
    }

    enum CacheKind {
        TAB,
        GROUP,
        CONTROL
    }
}
