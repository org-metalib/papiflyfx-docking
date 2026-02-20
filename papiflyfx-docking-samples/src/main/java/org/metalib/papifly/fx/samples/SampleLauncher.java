package org.metalib.papifly.fx.samples;

import javafx.application.Application;

/**
 * Plain-main trampoline for launching {@link SamplesApp} from IDE run configurations
 * without module-path issues.
 */
public class SampleLauncher {

    public static void main(String[] args) {
        Application.launch(SamplesApp.class, args);
    }
}
