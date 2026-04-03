package org.metalib.papifly.fx.docks;

import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.docking.api.Theme;

import java.util.Objects;

public record DockManagerServices(
    DockThemeServiceFactory themeServiceFactory,
    DockFloatingServiceFactory floatingServiceFactory,
    DockMinMaxServiceFactory minMaxServiceFactory,
    DockSessionServiceFactory sessionServiceFactory
) {

    public DockManagerServices {
        Objects.requireNonNull(themeServiceFactory, "themeServiceFactory");
        Objects.requireNonNull(floatingServiceFactory, "floatingServiceFactory");
        Objects.requireNonNull(minMaxServiceFactory, "minMaxServiceFactory");
        Objects.requireNonNull(sessionServiceFactory, "sessionServiceFactory");
    }

    public static DockManagerServices defaults() {
        return new DockManagerServices(
            DefaultDockThemeService::new,
            DefaultDockFloatingService::new,
            DefaultDockMinMaxService::new,
            DefaultDockSessionService::new
        );
    }

    @FunctionalInterface
    public interface DockThemeServiceFactory {
        DockThemeService create(Theme initialTheme, StackPane dockingLayer);
    }

    @FunctionalInterface
    public interface DockFloatingServiceFactory {
        DockFloatingService create(DockManagerContext context);
    }

    @FunctionalInterface
    public interface DockMinMaxServiceFactory {
        DockMinMaxService create(DockManagerContext context, DockFloatingService floatingService);
    }

    @FunctionalInterface
    public interface DockSessionServiceFactory {
        DockSessionService create(
            DockManagerContext context,
            DockTreeService treeService,
            DockFloatingService floatingService,
            DockMinMaxService minMaxService
        );
    }
}
