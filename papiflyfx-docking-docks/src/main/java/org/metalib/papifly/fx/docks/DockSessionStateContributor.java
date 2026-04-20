package org.metalib.papifly.fx.docks;

import org.metalib.papifly.fx.docks.layout.data.DockSessionData;

/**
 * Optional extension hook for enriching dock sessions with module-specific
 * payloads and restoring that payload after layout/session restore.
 */
public interface DockSessionStateContributor {

    /**
     * Enriches the captured session before serialization.
     *
     * @param session captured dock session
     * @return session to persist; implementations should return the provided
     *     instance when no changes are required
     */
    default DockSessionData captureSessionState(DockSessionData session) {
        return session;
    }

    /**
     * Restores module-specific state after the core dock session restore
     * finishes.
     *
     * @param session restored dock session
     */
    default void restoreSessionState(DockSessionData session) {
    }
}
