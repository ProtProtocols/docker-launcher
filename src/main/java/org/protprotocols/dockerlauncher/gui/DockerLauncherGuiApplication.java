package org.protprotocols.dockerlauncher.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.protprotocols.dockerlauncher.Controller.DlgLoadImageController;
import org.protprotocols.dockerlauncher.util.Constants;

import java.io.IOException;
import java.util.logging.Logger;

public class DockerLauncherGuiApplication extends Application {
    private final static Logger LOGGER = Logger.getLogger(DockerLauncherGuiApplication.class.getName());

    public void start(Stage primaryStage) throws Exception {
        primaryStage.setMaximized(false);
        primaryStage.initStyle(StageStyle.UNIFIED);

        // close the application when it's done
        // TODO: add function to gracefully stop everything
        primaryStage.setOnHiding(W -> System.exit(1));

        // TODO: Add icons
        /**
        String[] resolutions = {"32", "64", "128"};
        for (String resolution : resolutions) {
            primaryStage.getIcons().add(new Image(
                    DockerLauncherGuiApplication.class.getClassLoader().getResourceAsStream(
                            "images/application-icon_" + resolution + ".png")));
        }
         */

        showLoadImage(primaryStage);
    }

    private void showLoadImage(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Constants.FXML_DLG_LOAD_IMAGE));
        Parent root = loader.load();

        DlgLoadImageController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        // create the application window
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("ProtProtocols Protocol Launcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }


}
