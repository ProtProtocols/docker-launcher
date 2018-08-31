package org.protprotocols.dockerlauncher.Controller;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.Version;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.protprotocols.dockerlauncher.Tasks.DockerDownloadImageTask;
import org.protprotocols.dockerlauncher.util.Constants;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DlgLoadImageController extends DialogController {
    private final static Logger LOGGER = Logger.getLogger(DlgLoadImageController.class.getName());
    @FXML private TextArea statusTextArea;
    @FXML private Button btnLoadDockerImage;
    @FXML private Button btnNext;

    private Thread imageDownloadThread;
    private List<String> installedProtocols;

    @FXML
    public void initialize() throws Exception {
        // connect to docker
        final DockerClient docker = DefaultDockerClient.fromEnv().build();

        // make sure docker is running and responding
        try {
            final String pingResponse = docker.ping();

            if (pingResponse.equals("OK")) {
                statusTextArea.appendText("Successfully connected to docker deamon.\n");
            } else {
                statusTextArea.appendText("Error: Failed to ping docker daemon.\n");
                LOGGER.severe("Failed to ping docker deamon. Ping response = " + pingResponse);
                return;
            }

            // get the docker version
            final Version version = docker.version();
            statusTextArea.appendText("  Version: " + version.version() + "\n");
            statusTextArea.appendText("  API Version: " + version.apiVersion() + "\n");
            statusTextArea.appendText("  OS: " + version.os() + " (" + version.kernelVersion() + ")\n");

            // get the list of available images
            statusTextArea.appendText("\nGetting list of available images...\n");


            // get all labels
            updateInstalledProtocols(docker);

            if (installedProtocols.size() == 0) {
                statusTextArea.appendText("Protocol image not found.\n  Please click the button to download it.");
                btnLoadDockerImage.disableProperty().setValue(false);
            } else {
                statusTextArea.appendText("Found the following images:\n");
                installedProtocols.forEach(s -> statusTextArea.appendText("  " + s + "\n"));

                setNextStepPossible();
            }

        } catch (DockerException exception) {
            LOGGER.severe(exception.toString());
            statusTextArea.appendText("Error:\n");
            if (exception.toString().contains("Permission denied")) {
                statusTextArea.appendText("  Missing permission to connect docker service.\nPlease run this application as super user.");
            } else {
                statusTextArea.appendText("  Failed to connect to docker service.\nPlease make sure Docker is installed and running.\n\n" +
                        "To install Docker visit https://store.docker.com\nOnce Docker is installed and running, please re-start this application.\n");
            }


        }

        docker.close();
    }

    private void updateInstalledProtocols(DockerClient docker) throws DockerException, InterruptedException {
        final List<Image> images = docker.listImages();

        installedProtocols = images.stream()
                .filter(image -> image.repoTags() != null)
                .map(image -> image.repoTags().stream().filter(s -> s.contains("veitveit/isolabeledprotocol")).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private void setNextStepPossible() {
        btnNext.setDisable(false);
        statusTextArea.appendText("\nClick the \"Next\" button to continue...");
    }

    @FXML
    protected void onLoadDockerImageClicked(ActionEvent actionEvent) throws Exception {
        statusTextArea.appendText("\nDownloading protocol image from Docker Hub...\n");
        btnLoadDockerImage.disableProperty().setValue(true);
        btnLoadDockerImage.setText("Downloading...");


        // get the image
        LOGGER.info("Starting download of docker image...");

        DockerDownloadImageTask task = new DockerDownloadImageTask("veitveit/isolabeledprotocol:latest", this);
        imageDownloadThread = new Thread(task);
        imageDownloadThread.start();
    }

    public void dockerImageDownloadComplete() throws DockerCertificateException, DockerException, InterruptedException {
        statusTextArea.appendText("  Image download complete.");
        setNextStepPossible();
        updateInstalledProtocols(DefaultDockerClient.fromEnv().build());
    }

    public void dockerImageDownloadFailed(Exception e) {
        statusTextArea.appendText("  Image download failed.\n");
        statusTextArea.appendText("  " + e.getMessage() + "\n");
    }

    @FXML
    public void onNextBtnClicked(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Constants.FXML_DLG_IMAGE_SETTINGS));
        Parent root = loader.load();

        DlgImageSettingsController controller = loader.getController();
        controller.setInstalledProtocols(installedProtocols);
        controller.setPrimaryStage(primaryStage);

        // create the application window
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
