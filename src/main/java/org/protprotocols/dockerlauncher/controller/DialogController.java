package org.protprotocols.dockerlauncher.controller;

import javafx.stage.Stage;

import java.io.IOException;
import java.util.Properties;

public abstract class DialogController {
    protected Stage primaryStage;
    protected final Properties properties;

    protected DialogController() {
        properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("docker_launcher.properties"));
        }
        catch (IOException e) {
            // this should never happen
            throw new IllegalStateException(e);
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
