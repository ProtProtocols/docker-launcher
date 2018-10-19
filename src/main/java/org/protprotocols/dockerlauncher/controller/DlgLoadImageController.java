package org.protprotocols.dockerlauncher.controller;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.Version;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.protprotocols.dockerlauncher.tasks.CheckNewVersionTask;
import org.protprotocols.dockerlauncher.tasks.DockerDownloadImageTask;
import org.protprotocols.dockerlauncher.tasks.DockerInitialConnectionTask;
import org.protprotocols.dockerlauncher.tasks.GetAvailableImageVersionsTask;
import org.protprotocols.dockerlauncher.util.Constants;
import org.protprotocols.dockerlauncher.util.LoggerHelperFunctions;
import org.protprotocols.dockerlauncher.util.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DlgLoadImageController extends DialogController {
    private final static Logger log = LoggerFactory.getLogger(DlgLoadImageController.class);
    @FXML private ProgressIndicator progressIndicator;
    @FXML private ChoiceBox imageVersionBox;
    @FXML private TextArea statusTextArea;
    @FXML private Button btnLoadDockerImage;
    @FXML private Button btnNext;

    private Thread imageDownloadThread;
    private List<String> installedProtocols;

    @FXML
    public void initialize() {
        log.debug("Detecting OS for error message: " + System.getProperty("os.name") + " - " + System.getProperty("os.version"));
        statusTextArea.appendText("docker-launcher version " + properties.getProperty("version") + "\n\n");

        // check if a new version is available
        CheckNewVersionTask task = new CheckNewVersionTask(properties);
        task.setOnSucceeded(event -> {

            final String version = (String) event.getSource().getValue();
            Platform.runLater(() -> {
                if (version != null) {
                        log.debug(version + " available for download.");
                        statusTextArea.appendText("Version " + version + " available for download\n  To update visit\n" +
                                properties.getProperty("download_url") + "\n-------------------------------------\n\n");
                }
            });
        });
        new Thread(task).start();
        statusTextArea.appendText("Checking for new version...\n");

        // get the available image versions
        GetAvailableImageVersionsTask versionsTask = new GetAvailableImageVersionsTask(
                properties.getProperty(Constants.PORPERTY_IMAGE_RELEASE_URL));
        versionsTask.setOnSucceeded(event -> Platform.runLater(() -> initImageVersions((List<String>) event.getSource().getValue())));
        versionsTask.setOnFailed(event -> Platform.runLater(() -> initImageVersions(null)));
        new Thread(versionsTask).start();
        statusTextArea.appendText("Retrieving available protocol versions...\n");

        // connect to docker
        DockerInitialConnectionTask dockerTask = new DockerInitialConnectionTask(properties.getProperty(Constants.PROPERTY_IMAGE_NAME));
        dockerTask.setOnSucceeded(event -> {
            final List<String> foundProtocols = (List<String>) event.getSource().getValue();
            Platform.runLater(() -> {
                if (foundProtocols == null) {
                    return;
                }

                // enable the download controls
                btnLoadDockerImage.setDisable(false);
                imageVersionBox.setDisable(false);

                // if images are installed, enable the next step
                if (foundProtocols.size() > 0) {
                    setNextStepPossible();
                }

                this.installedProtocols = foundProtocols;
            });
        });
        dockerTask.messageProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            log.debug("Docker message: " +  newValue);
            // display all messages in the statusTextArea
            statusTextArea.appendText(newValue);
        }));
        new Thread(dockerTask).start();
    }

    /**
     * Initializes the drop-down list that shows the image versions
     * @param availableVersions A list of version names
     */
    private void initImageVersions(List<String> availableVersions) {
        if (availableVersions == null) {
            log.warn("Failed to retrieve available versions. Using inbuilt list.");
            // load the versions from the properties as fallback
            availableVersions = Arrays.stream(properties.getProperty(Constants.PROPERTY_IMAGE_VERSIONS).split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        // always add latest
        availableVersions.add("latest");

        imageVersionBox.setItems(FXCollections.observableArrayList(availableVersions));
        log.debug("Selecting version " + availableVersions.get(0));
        imageVersionBox.setValue(availableVersions.get(0));
    }

    public static List<String> getInstalledProtocols(DockerClient docker, String imageName) throws DockerException, InterruptedException {
        final List<Image> images = docker.listImages();

        List<String> installedProtocols = images.stream()
                .filter(image -> image.repoTags() != null)
                .map(image -> image.repoTags().stream().filter(s -> s.contains(imageName)).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return installedProtocols;
    }

    private void setNextStepPossible() {
        btnNext.setDisable(false);
        statusTextArea.appendText("\nClick the \"Next\" button to continue...");
        // scroll to the bottom of the field
        Platform.runLater(() -> statusTextArea.setScrollTop(Double.MAX_VALUE));
    }

    @FXML
    protected void onLoadDockerImageClicked(ActionEvent actionEvent) throws Exception {
        statusTextArea.appendText("\nDownloading protocol image from Docker Hub...\n");

        // get the image
        log.info("Starting download of docker image...");

        String imageName = properties.getProperty(Constants.PROPERTY_IMAGE_NAME) + ":" + imageVersionBox.getValue();

        DockerDownloadImageTask task = new DockerDownloadImageTask(imageName, this);

        imageDownloadThread = new Thread(task);
        imageDownloadThread.start();

        imageDownloadInProgress(true);
    }

    private void imageDownloadInProgress(boolean inProgress) {
        btnLoadDockerImage.setDisable(inProgress);
        imageVersionBox.setDisable(inProgress);
        progressIndicator.setVisible(inProgress);
        if (inProgress) {
            btnNext.setDisable(true);
        }
    }

    public void dockerImageDownloadComplete() {
        imageDownloadInProgress(false);

        try {
            statusTextArea.appendText("  Image download complete.");
            installedProtocols = getInstalledProtocols(DefaultDockerClient.fromEnv().build(), properties.getProperty(Constants.PROPERTY_IMAGE_NAME));
            setNextStepPossible();
        } catch (Exception e) {
            log.error("Failed to update image list\n" + e.getMessage());
            statusTextArea.appendText("  Failed to update list of images.\n");
        }
    }

    public void dockerImageDownloadFailed(Exception e) {
        imageDownloadInProgress(false);

        statusTextArea.appendText("  Image download failed.\n");
        statusTextArea.appendText("  " + e.getMessage() + "\n");
        log.error("Failed to download image.\n" + e.getMessage());
        LoggerHelperFunctions.logStackTrace(log, e);
    }

    @FXML
    public void onNextBtnClicked(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Constants.FXML_DLG_IMAGE_SETTINGS));
        Parent root = loader.load();

        DlgImageSettingsController controller = loader.getController();
        controller.setInstalledProtocols(installedProtocols);
        controller.setPrimaryStage(primaryStage);

        // create the application window
        Scene scene = new Scene(root, Settings.getSceneWidth(), Settings.getSceneHeight());

        scene.getStylesheets().addAll(Constants.DEFAULT_CSS, Settings.getCss());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
