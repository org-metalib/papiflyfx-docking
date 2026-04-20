package org.metalib.papifly.fx.docks.ribbon;

import org.metalib.papifly.fx.docks.DockSessionStateContributor;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.RibbonSessionData;

import java.util.Objects;

final class RibbonSessionStateContributor implements DockSessionStateContributor {

    private final Ribbon ribbon;

    RibbonSessionStateContributor(Ribbon ribbon) {
        this.ribbon = Objects.requireNonNull(ribbon, "ribbon");
    }

    @Override
    public DockSessionData captureSessionState(DockSessionData session) {
        if (session == null) {
            return null;
        }
        RibbonSessionData ribbonState = ribbon.captureSessionState();
        return session.withRibbon(ribbonState);
    }

    @Override
    public void restoreSessionState(DockSessionData session) {
        if (session == null || session.ribbon() == null) {
            return;
        }
        ribbon.restoreSessionState(session.ribbon());
    }
}
