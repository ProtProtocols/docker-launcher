package org.protprotocols.dockerlauncher.tasks;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.RegistryAuth;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.protprotocols.dockerlauncher.controller.DlgLoadImageController;

public class DockerDownloadImageTask extends Task<Void> {
    private final String imageName;
    private final DlgLoadImageController controller;


    public DockerDownloadImageTask(String imageName, DlgLoadImageController controller) {
        this.imageName = imageName;
        this.controller = controller;
    }

    @Override
    protected Void call() {
        try (DockerClient docker = DefaultDockerClient.fromEnv().build()) {
            docker.pull(imageName, RegistryAuth.builder().build());
            updateProgress(100, 100);
            updateMessage("Download complete");
            Platform.runLater(() -> controller.dockerImageDownloadComplete());
        } catch (Exception e) {
            Platform.runLater(() -> controller.dockerImageDownloadFailed(e));
        }

        return null;
    }
}
