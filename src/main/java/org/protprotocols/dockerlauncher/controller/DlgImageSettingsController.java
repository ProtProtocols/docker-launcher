package org.protprotocols.dockerlauncher.controller;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.protprotocols.dockerlauncher.events.DockerLauncherEventTypes;
import org.protprotocols.dockerlauncher.gui.DockerLauncherGuiApplication;
import org.protprotocols.dockerlauncher.tasks.ListenContainerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DlgImageSettingsController extends DialogController {
    private final Logger log = LoggerFactory.getLogger(DlgImageSettingsController.class);


    @FXML public ChoiceBox protocolList;
    @FXML public TextField workingDirectoryPath;
    @FXML public Button btnNext;
    @FXML public TextArea statusTextArea;
    @FXML public Button btnBrowseWorkdir;
    @FXML public Hyperlink containerURL;
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

                primaryStage.getScene().setCursor(Cursor.DEFAULT);

                containerURL.setText("");

                return;
            }

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

            final HostConfig hostConfig = HostConfig.builder()
                    .portBindings(portBindings)
                    .appendBinds(HostConfig.Bind
                            .from(workingDirectoryPath.getText())
                            .to("/data")
                            .readOnly(false)
                            .build())
                    .build();

            // Create container with exposed ports
            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .hostConfig(hostConfig)
                    // TODO: get selected image
                    .image(protocolList.getValue().toString()).exposedPorts(exposedPorts)
                    //.cmd("sh", "-c", "while :; do sleep 1; done")
                    .build();

            final ContainerCreation creation = docker.createContainer(containerConfig);
            runningContainerId = creation.id();

            // Show that docker is running
            statusTextArea.clear();
            statusTextArea.appendText("Starting docker container....\n");
            statusTextArea.appendText("  Image is " + protocolList.getValue().toString() + "\n");
            statusTextArea.appendText("  Working directory set to " + workingDirectoryPath.getText() + "\n");
            statusTextArea.appendText("  Using port " + String.valueOf(port) + "\n\n");

            // disable all controls
            btnBrowseWorkdir.setDisable(true);
            protocolList.setDisable(true);

            // Start container
            docker.startContainer(runningContainerId);

            // change the button label
            btnNext.setText("Stop container");

            ListenContainerTask task = new ListenContainerTask(runningContainerId);
            task.addEventHandler(DockerLauncherEventTypes.CONTAINER_LOG, event -> {
                statusTextArea.appendText(event.getLogMessage());
            });
            containerListenerThread = new Thread(task);
            containerListenerThread.start();

            // open the website with a short delay
            // TODO: check that the server is running
            TimeUnit.SECONDS.sleep(1);
            String dockerUrl = "http://localhost:" + String.valueOf(port);
            log.info("Openging web browser for " + dockerUrl);

            boolean browserLaunched = false;
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    log.info("Using desktop.browse");
                    desktop.browse(new URI(dockerUrl));
                    browserLaunched = true;
                }
            }
            if (!browserLaunched) {
                HostServices hostServices = new DockerLauncherGuiApplication().getHostServices();
                hostServices.showDocument(dockerUrl);

            }

            containerURL.setText("http://localhost:" + String.valueOf(port));

            // Exec command inside running container with attached STDOUT and STDERR
            /*
            final String[] command = {"sh", "-c", "ls"};
            final ExecCreation execCreation = docker.execCreate(
                    id, command, DockerClient.ExecCreateParam.attachStdout(),
                    DockerClient.ExecCreateParam.attachStderr());
            final LogStream output = docker.execStart(execCreation.id());
            final String execOutput = output.readFully();
            */
        } catch (Exception e) {
            statusTextArea.appendText("  Failed to launch docker image\n");
            statusTextArea.appendText(e.getMessage() + "\n");
        } finally {
            primaryStage.getScene().setCursor(Cursor.DEFAULT);
        }
    }

    private static boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    public void onBrowseWorkingDirectory(ActionEvent actionEvent) {
        // create the file chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select working directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File directory = directoryChooser.showDialog(primaryStage);

        if (directory != null) {
            workingDirectoryPath.setText(directory.getAbsolutePath());
            btnNext.setDisable(false);
        } else {
            btnNext.setDisable(true);
        }
    }
}
