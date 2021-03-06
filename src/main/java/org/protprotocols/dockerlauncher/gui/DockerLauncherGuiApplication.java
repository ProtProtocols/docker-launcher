package org.protprotocols.dockerlauncher.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.protprotocols.dockerlauncher.controller.DlgLoadImageController;
import org.protprotocols.dockerlauncher.util.Constants;
import org.protprotocols.dockerlauncher.util.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class DockerLauncherGuiApplication extends Application {
    private final static Logger log = LoggerFactory.getLogger(DockerLauncherGuiApplication.class);

    public void start(Stage primaryStage) throws Exception {
        primaryStage.setMaximized(false);
        primaryStage.initStyle(StageStyle.UNIFIED);

        // close the application when it's done
        // TODO: add function to gracefully stop everything
        primaryStage.setOnHiding(W -> System.exit(1));

        String[] resolutions = {"32", "64", "128", "256"};
        for (String resolution : resolutions) {
            primaryStage.getIcons().add(new Image(
                    DockerLauncherGuiApplication.class.getClassLoader().getResourceAsStream(
                            "icons/icon_" + resolution + ".png")));
        }

        showLoadImage(primaryStage);
    }

    private void showLoadImage(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Constants.FXML_DLG_LOAD_IMAGE));
        Parent root = loader.load();

        DlgLoadImageController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        // create the application window
        Scene scene = new Scene(root, Settings.getSceneWidth(), Settings.getSceneHeight());
        scene.getStylesheets().addAll(Constants.DEFAULT_CSS, Settings.getCss());
        primaryStage.setTitle("ProtProtocols docker-launcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        try {
            // set the look and feel
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());

            // set the css to use
            double dpi = Screen.getPrimary().getDpi();
            log.debug("Detected resolution: " + String.valueOf(dpi) + " dpi");

            if (dpi >= Constants.HIGH_DPI) {
                Settings.setCss(Constants.HIGH_RES_CSS);
                Settings.setSceneHeight(Constants.HIGH_RES_HEIGHT);
                Settings.setSceneWidth(Constants.HIGH_RES_WIDTH);
            } else {
                Settings.setCss(Constants.LOW_RES_CSS);
                Settings.setSceneHeight(Constants.LOW_RES_HEIGHT);
                Settings.setSceneWidth(Constants.LOW_RES_WIDTH);
            }
        } catch (Exception e) {
            log.warn("Failed to set look and feel: " + e.getMessage());
        }
        launch(args);
    }


}
