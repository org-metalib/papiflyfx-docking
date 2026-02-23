package org.metalib.papifly.fx.samples;

import javafx.application.Application;

/**
 * Plain-main trampoline for launching {@link SamplesApp} from IDE run configurations
 * without module-path issues.
 */
public class SampleLauncher {

    /**
     * Creates a sample launcher.
     */
    public SampleLauncher() {
        // Default constructor.
    }

    /**
     * Launches the sample application.
     *
     * @param args launcher arguments
     */
    public static void main(String[] args) {
        Application.launch(SamplesApp.class, args);
    }
}
