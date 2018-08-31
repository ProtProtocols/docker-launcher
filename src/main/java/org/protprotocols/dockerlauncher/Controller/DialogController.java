package org.protprotocols.dockerlauncher.Controller;

import javafx.stage.Stage;

public abstract class DialogController {
    protected Stage primaryStage;

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
