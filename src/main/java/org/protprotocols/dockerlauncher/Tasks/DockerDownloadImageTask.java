package org.protprotocols.dockerlauncher.Tasks;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import javafx.concurrent.Task;
import org.protprotocols.dockerlauncher.Controller.DlgLoadImageController;

public class DockerDownloadImageTask extends Task {
    private final String imageName;
    private final DlgLoadImageController controller;


    public DockerDownloadImageTask(String imageName, DlgLoadImageController controller) {
        this.imageName = imageName;
        this.controller = controller;
    }

    @Override
    protected Object call() {
        try (DockerClient docker = DefaultDockerClient.fromEnv().build()) {
            docker.pull(imageName);
            controller.dockerImageDownloadComplete();
        } catch (Exception e) {
            controller.dockerImageDownloadFailed(e);
        }

        return null;
    }
}
