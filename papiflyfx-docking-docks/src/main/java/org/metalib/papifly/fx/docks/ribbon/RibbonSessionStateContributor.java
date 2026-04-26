package org.metalib.papifly.fx.docks.ribbon;

import org.metalib.papifly.fx.docks.DockSessionExtensionCodec;
import org.metalib.papifly.fx.docks.DockSessionStateContributor;
import org.metalib.papifly.fx.docks.layout.data.RibbonSessionData;

import java.util.Objects;

final class RibbonSessionStateContributor implements DockSessionStateContributor<RibbonSessionData> {

    static final String EXTENSION_NAMESPACE = "ribbon";

    private static final RibbonSessionCodec CODEC = new RibbonSessionCodec();

    private final Ribbon ribbon;

    RibbonSessionStateContributor(Ribbon ribbon) {
        this.ribbon = Objects.requireNonNull(ribbon, "ribbon");
    }

    @Override
    public String extensionNamespace() {
        return EXTENSION_NAMESPACE;
    }

    @Override
    public DockSessionExtensionCodec<RibbonSessionData> codec() {
        return CODEC;
    }

    @Override
    public RibbonSessionData captureSessionState() {
        return ribbon.captureSessionState();
    }

    @Override
    public void restoreSessionState(RibbonSessionData sessionState) {
        ribbon.restoreSessionState(sessionState);
    }
}
