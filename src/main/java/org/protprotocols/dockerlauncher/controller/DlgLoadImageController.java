package org.protprotocols.dockerlauncher.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.Version;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.protprotocols.dockerlauncher.tasks.DockerDownloadImageTask;
import org.protprotocols.dockerlauncher.util.Constants;
import org.protprotocols.dockerlauncher.util.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DlgLoadImageController extends DialogController {
    private final static Logger log = LoggerFactory.getLogger(DlgLoadImageController.class);
    @FXML private ProgressIndicator progressIndicator;
    @FXML private ComboBox imageVersionBox;
    @FXML private TextArea statusTextArea;
    @FXML private Button btnLoadDockerImage;
    @FXML private Button btnNext;

    private Thread imageDownloadThread;
    private List<String> installedProtocols;

    @FXML
    public void initialize() throws Exception {
        log.debug("Detecting OS for error message: " + System.getProperty("os.name"));
        statusTextArea.appendText("docker-launcher version " + properties.getProperty("version") + "\n\n");

        try {
            String newerVersion = isNewVersionAvailable();
            if (newerVersion != null) {
                log.debug(newerVersion + " available for download.");
                statusTextArea.appendText("Version " + newerVersion + " available for download\n  To update visit\n" +
                        properties.getProperty("download_url") + "\n-------------------------------------\n\n");
            }
        }
        catch (Exception e) {
            log.debug("Failed to check for new version: " + e.getMessage());
        }

        // make sure docker is running and responding
        try (DockerClient docker = DefaultDockerClient.fromEnv().build()){
            // inititalize the image version drop-down
            initImageVersions();

            final String pingResponse = docker.ping();

            if (pingResponse.equals("OK")) {
                statusTextArea.appendText("Successfully connected to docker deamon.\n");
            } else {
                statusTextArea.appendText("Error: Failed to ping docker daemon.\n");
                log.error("Failed to ping docker deamon. Ping response = " + pingResponse);
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
            updateInstalledProtocols(docker, properties.getProperty(Constants.PROPERTY_IMAGE_NAME));

            if (installedProtocols.size() == 0) {
                statusTextArea.appendText("Protocol image not found.\n  Please click the button to download it.");
                btnLoadDockerImage.disableProperty().setValue(false);
            } else {
                statusTextArea.appendText("Found the following images:\n");
                installedProtocols.forEach(s -> statusTextArea.appendText("  " + s + "\n"));

                setNextStepPossible();
            }

        } catch (DockerException exception) {
            log.error(exception.toString());
            statusTextArea.appendText("Error:\n");
            if (exception.toString().contains("Permission denied")) {
                statusTextArea.appendText("  Missing permission to connect docker service.\nPlease run this application as super user.");
            } else {
                statusTextArea.appendText("  Failed to connect to docker service.\nPlease make sure Docker is installed and running.\n\n" +
                        "To install Docker visit https://store.docker.com\nOnce Docker is installed and running, please re-start this application.\n");
            }

            // add special windows message
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                statusTextArea.appendText("\nIf Docker is already running, make sure to enable the option \"Expose " +
                        "daemon on tcp://localhost:2375\" in your Docker settings.");
            }

            // disable the remaining controls
            btnLoadDockerImage.setDisable(true);
            imageVersionBox.setDisable(true);
        }
    }

    private String isNewVersionAvailable() throws IOException {
        // check if a new version is available
        String currentVersion = properties.getProperty("version");
        log.debug("Running version " + currentVersion);

        // get the latest release
        URL url = new URL(properties.getProperty("release_url"));
        URLConnection request = url.openConnection();
        request.connect();

        // Convert to a JSON object to print data
        JsonParser jp = new JsonParser(); //from gson
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
        JsonArray releases = root.getAsJsonArray();

        List<String> availableReleases = new ArrayList<>(releases.size());

        for (JsonElement release : releases) {
            log.debug("Found release " + release.getAsJsonObject().get("tag_name"));
            availableReleases.add(release.getAsJsonObject().get("tag_name").getAsString());
        }

        DefaultArtifactVersion runningVersion = new DefaultArtifactVersion(currentVersion);
        List<DefaultArtifactVersion> availableVersions = availableReleases.stream().map(DefaultArtifactVersion::new).sorted().collect(Collectors.toList());

        if (availableVersions.get(availableVersions.size() - 1).compareTo(runningVersion) > 0) {
            return availableVersions.get(availableVersions.size() - 1).toString();
        }

        return null;
    }

    /**
     * Initializes the drop-down list that shows the image versions
     */
    private void initImageVersions() {
        // load the versions from the properties
        List<String> versions = Arrays.stream(properties.getProperty(Constants.PROPERTY_IMAGE_VERSIONS).split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        imageVersionBox.setItems(FXCollections.observableArrayList(versions));
        imageVersionBox.setValue(versions.get(0));
    }

    private void updateInstalledProtocols(DockerClient docker, String imageName) throws DockerException, InterruptedException {
        final List<Image> images = docker.listImages();

        installedProtocols = images.stream()
                .filter(image -> image.repoTags() != null)
                .map(image -> image.repoTags().stream().filter(s -> s.contains(imageName)).findFirst())
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

        // get the image
        log.info("Starting download of docker image...");

        String imageName = properties.getProperty(Constants.PROPERTY_IMAGE_NAME) + ":" + imageVersionBox.getValue();

        DockerDownloadImageTask task = new DockerDownloadImageTask(imageName, this);
        if (progressIndicator.progressProperty().isBound()) {
            progressIndicator.progressProperty().unbind();
        }
        progressIndicator.progressProperty().bind(task.progressProperty());

        // display any message
        task.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                Platform.runLater(() -> statusTextArea.appendText("  " + newValue + "\n"));
        });

        imageDownloadThread = new Thread(task);
        imageDownloadThread.start();

        imageDownloadInProgress(true);
    }

    private void imageDownloadInProgress(boolean inProgress) {
        btnLoadDockerImage.setDisable(inProgress);
        imageVersionBox.setDisable(inProgress);
        progressIndicator.setVisible(inProgress);
    }

    public void dockerImageDownloadComplete() {
        imageDownloadInProgress(false);

        try {
            statusTextArea.appendText("  Image download complete.");
            updateInstalledProtocols(DefaultDockerClient.fromEnv().build(), properties.getProperty(Constants.PROPERTY_IMAGE_NAME));
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
        // get the stack trace as string
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        log.debug(writer.toString());
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
        scene.getStylesheets().addAll(Settings.getCss());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
