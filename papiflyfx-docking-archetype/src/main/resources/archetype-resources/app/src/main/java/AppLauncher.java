package ${package};

/**
 * Plain main() trampoline required because JavaFX Application classes
 * cannot be launched directly when the module path is not configured.
 */
public class AppLauncher {
    public static void main(String[] args) {
        App.main(args);
    }
}
