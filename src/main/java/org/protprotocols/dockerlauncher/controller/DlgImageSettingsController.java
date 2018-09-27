package org.protprotocols.dockerlauncher.controller;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import org.protprotocols.dockerlauncher.events.DockerLauncherEventTypes;
import org.protprotocols.dockerlauncher.gui.DockerLauncherGuiApplication;
import org.protprotocols.dockerlauncher.tasks.ListenContainerTask;
import org.protprotocols.dockerlauncher.tasks.OpenDockerPageTask;
import org.protprotocols.dockerlauncher.util.LoggerHelperFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DlgImageSettingsController extends DialogController {
    private final Logger log = LoggerFactory.getLogger(DlgImageSettingsController.class);


    @FXML public ChoiceBox protocolList;
    @FXML public TextField workingDirectoryPath;
    @FXML public Button btnNext;
    @FXML public TextArea statusTextArea;
    @FXML public Button btnBrowseWorkdir;
    @FXML public Hyperlink containerURL;
    @FXML public TextField outputDirectoryPath;
    private List<String> installedProtocols;
    private Thread containerListenerThread;

    private String runningContainerId;

    public List<String> getInstalledProtocols() {
        return installedProtocols;
    }

    @FXML
    public void initialize() throws Exception {

    }

    public void setInstalledProtocols(List<String> installedProtocols) {
        this.installedProtocols = installedProtocols;

        if (installedProtocols != null) {
            log.info(String.valueOf(installedProtocols.size()) + " protocols installed");
            protocolList.setItems(FXCollections.observableArrayList(installedProtocols));
            // select the first protocol
            if (installedProtocols.size()> 0) {
                protocolList.setValue(installedProtocols.get(0));
            }
        } else {
            log.error("No protocols set");
        }
    }

    @FXML
    public void onStartImageClicked(ActionEvent actionEvent) throws Exception {
        // set the cursor to wait
        primaryStage.getScene().setCursor(Cursor.WAIT);

        try (DockerClient docker = DefaultDockerClient.fromEnv().build()){
            // TODO: move to different functions
            if (btnNext.getText().equals("Stop container")) {
                docker.killContainer(runningContainerId);
                containerListenerThread.stop();
                btnNext.setText("Start container");
                btnBrowseWorkdir.setDisable(false);
                protocolList.setDisable(false);

                if (workingDirectoryPath.getText().equals("-- DISABLED --")) {
                    btnNext.setDisable(true);
                    workingDirectoryPath.setText("");
                }

                primaryStage.getScene().setCursor(Cursor.DEFAULT);

                containerURL.setText("");

                return;
            }
            // Show that docker is starting
            statusTextArea.clear();
            statusTextArea.appendText("Starting docker container....\n");
            statusTextArea.appendText("  Image is " + protocolList.getValue().toString() + "\n");
            statusTextArea.appendText("  Working directory set to " + workingDirectoryPath.getText() + "\n");

            // launch the image
            int port = launchDockerImage(docker);

            statusTextArea.appendText("  Using port " + String.valueOf(port) + "\n\n");

            // disable all controls
            btnBrowseWorkdir.setDisable(true);
            protocolList.setDisable(true);

            // change the button label
            btnNext.setText("Stop container");

            // get the container output
            ListenContainerTask task = new ListenContainerTask(runningContainerId);
            task.addEventHandler(DockerLauncherEventTypes.CONTAINER_LOG, event -> {
                statusTextArea.appendText(event.getLogMessage());
            });
            containerListenerThread = new Thread(task);
            containerListenerThread.start();

            // open the website with a short delay using a separate task
            String hostAddress = getHostAddress(docker);
            String dockerUrl = "http://" + hostAddress + ":" + String.valueOf(port);

            OpenDockerPageTask openDockerPageTask = new OpenDockerPageTask(dockerUrl);
            Thread openThread = new Thread(openDockerPageTask);
            openThread.setDaemon(true);
            openThread.start();

            containerURL.setText(dockerUrl);
        } catch (Exception e) {
            log.error("Failed to launched docker image: " + e.getMessage());
            statusTextArea.appendText("  Failed to launch docker image:\n");

            // check if we now why it failed
            if (e.getMessage().toLowerCase().contains("driver failed programming external connectivity")) {
                statusTextArea.appendText("  Docker failed to forward port.\n" +
                        "Please restart the Docker service to solve this issue.\n" +
                        "If this does not help, restart your computer as well.\n");
            }
            else {
                statusTextArea.appendText("  " + e.getMessage() + "\n");
                // only save the stack trace if we don't know what's going on
                LoggerHelperFunctions.logStackTrace(log, e);
            }

        } finally {
            primaryStage.getScene().setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Determines the host address to use to access the running image.
     * @param docker
     * @return The host address as String.
     * @throws Exception
     */
    private String getHostAddress(DockerClient docker) throws Exception {
        // this only needs to be adapted when using Docker Toolbox
        final Version dockerVersion = docker.version();

        // docker-machine ip
        if (dockerVersion.kernelVersion().contains("boot2docker")) {
            Info systemInfo = docker.info();
            log.debug("Adapting ip on DockerToolbox based on system info: " + systemInfo.noProxy());
            return systemInfo.noProxy();
        } else {
            return "localhost";
        }
    }

    /**
     * Launches the Docker image
     */
    private int launchDockerImage(DockerClient docker) throws Exception {
        // Bind container ports to host ports
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();

        // Bind container jupyter port to an automatically allocated available host port.
        List<PortBinding> hostPort = new ArrayList<>();
        int port = 8888;

        while (!isPortAvailable(port)) {
            port++;
        }

        hostPort.add(PortBinding.create("0.0.0.0", String.valueOf(port)));
        portBindings.put("8888", hostPort);
        String[] exposedPorts = {"8888"};

        String machineInputPath = adaptDirectoryPath(workingDirectoryPath.getText(), docker);
        String machineWorkingPath = adaptDirectoryPath(outputDirectoryPath.getText(), docker);

        final HostConfig hostConfig = HostConfig.builder()
                .portBindings(portBindings)
                .appendBinds(HostConfig.Bind
                        .from(machineInputPath)
                        .to("/data")
                        .readOnly(false)
                        .build())
                .appendBinds(HostConfig.Bind
                        .from(machineWorkingPath)
                        .to("/home/biodocker/OUT")
                        .readOnly(false)
                        .build())
                .build();

        // Create container with exposed ports
        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(protocolList.getValue().toString()).exposedPorts(exposedPorts)
                .build();

        try {
            log.debug("Creating docker container...");
            final ContainerCreation creation = docker.createContainer(containerConfig);
            runningContainerId = creation.id();
            // Start container
            log.debug("Starting docker container...");
            docker.startContainer(runningContainerId);
        } catch (Exception e) {
            log.debug("Caught creation Exception: " + e.getMessage());

            if (e.getMessage().toLowerCase().contains("firewall") || e.getMessage().toLowerCase().contains("drive")) {
                log.info("Drive sharing is blocked by a firewall or has not been enabled, launching image without shared drives");
                statusTextArea.appendText("  Drive sharing is blocked by a firewall or has not been enabled.\nLaunching image without shared drives...\n");
                workingDirectoryPath.setText("-- DISABLED --");
                // TODO: add more verbose information about what it means to work without shared drives

                // launch the container without any drive sharing
                final HostConfig noDriveConfig = HostConfig.builder()
                        .portBindings(portBindings)
                        .build();

                // Create container with exposed ports
                final ContainerConfig noDriveContainerConfig = ContainerConfig.builder()
                        .hostConfig(noDriveConfig)
                        .image(protocolList.getValue().toString()).exposedPorts(exposedPorts)
                        .build();

                final ContainerCreation creation = docker.createContainer(noDriveContainerConfig);
                runningContainerId = creation.id();
                // Start container
                docker.startContainer(runningContainerId);
            } else {
                log.debug("Error Message does not point to a firewall issues, re-throwing (" + e.getMessage() + ")");
                throw e;
            }
        }

        return port;
    }

    /**
     * Adapt the path based on the running docker version and OS
     * @param path
     * @param docker
     * @return
     */
    private String adaptDirectoryPath(String path, DockerClient docker) throws Exception {
        final Version dockerVersion = docker.version();

        if (dockerVersion.kernelVersion().contains("boot2docker")) {
            log.debug("Using DockerToolbox - adpating path " + path);
            // make sure the path is under C:\Users
            if (!path.startsWith("C:\\User")) {
                log.error("Invalid working directory selected when running DockerToolbox");
                throw new Exception("Docker Toolbox only supports sharing paths under C:\\Users");
            }

            // adapt the path
            path = path.replaceFirst("C", "c");
            path = path.replaceAll(":", "");
            path = path.replaceAll("\\\\", "/");
            path = "/" + path;

            log.debug("Adapted path to " + path);
        }

        return path;
    }

    private static boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    @FXML
    public void onBrowseWorkingDirectory(ActionEvent actionEvent) {
        // create the file chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select working directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File directory = directoryChooser.showDialog(primaryStage);

        if (directory != null) {
            workingDirectoryPath.setText(directory.getAbsolutePath());
        }

        testNextPossible();
    }

    @FXML
    public void onBrowseOutputDirectory(ActionEvent actionEvent) {
        // create the file chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select output directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File directory = directoryChooser.showDialog(primaryStage);

        if (directory != null) {
            outputDirectoryPath.setText(directory.getAbsolutePath());
        }

        testNextPossible();
    }

    private void testNextPossible() {
        boolean nextImpossible = false;

        if (workingDirectoryPath.getText().length() == 0) {
            nextImpossible = true;
        }
        if (outputDirectoryPath.getText().length() == 0) {
            nextImpossible = true;
        }

        btnNext.setDisable(nextImpossible);
    }

    public void onHyperlinkedClicked(ActionEvent actionEvent) {
        String url = containerURL.getText();

        if (url.trim().startsWith("http")) {
            HostServices hostServices = new DockerLauncherGuiApplication().getHostServices();
            hostServices.showDocument(url);
        }
    }
}
