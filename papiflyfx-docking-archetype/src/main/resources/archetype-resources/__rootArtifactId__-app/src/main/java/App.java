#set( $dollar = '$' )
package ${package};

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;

/**
 * Main PapiflyFX application.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        var dockManager = new DockManager();
        var tabGroup = dockManager.createTabGroup();
        dockManager.setRoot(tabGroup);

        var scene = new Scene(dockManager.getRootPane(), 1024, 768);
        stage.setTitle("${artifactId}");
        stage.setScene(scene);
        stage.show();

        dockManager.setOwnerStage(stage);
    }
}
