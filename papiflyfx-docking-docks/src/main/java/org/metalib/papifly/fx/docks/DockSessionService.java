package org.metalib.papifly.fx.docks;

import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.metalib.papifly.fx.docks.layout.data.LayoutNode;

import java.nio.file.Path;

public interface DockSessionService {

    LayoutNode captureLayout();

    DockSessionData captureSession();

    void restoreSession(DockSessionData session);

    String saveSessionToString();

    void restoreSessionFromString(String json);

    void saveSessionToFile(Path path);

    void loadSessionFromFile(Path path);
}
