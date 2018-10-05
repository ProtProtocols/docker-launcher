package org.protprotocols.dockerlauncher.tasks;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Version;
import javafx.concurrent.Task;
import org.protprotocols.dockerlauncher.controller.DlgLoadImageController;
import org.protprotocols.dockerlauncher.util.LoggerHelperFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This tasks connects to the docker service
 * the first time to test if Docker is available
 * and to test whether the image is already installed.
 */
public class DockerInitialConnectionTask extends Task<List<String>> {
    private final static Logger log = LoggerFactory.getLogger(DockerInitialConnectionTask.class);
    private final String imageName;

    public DockerInitialConnectionTask(String imageName) {
        this.imageName = imageName;
    }

    @Override
    protected List<String> call() throws Exception {
        try {
            updateMessage("Testing Docker connection...\n");
            // make sure docker is running and responding
            try (DockerClient docker = DefaultDockerClient.fromEnv().build()) {
                final String pingResponse = docker.ping();
                StringBuilder logMessages = new StringBuilder();

                if (pingResponse.equals("OK")) {
                    logMessages.append("Successfully connected to docker deamon.\n");
                } else {
                    updateMessage("Error: Failed to ping docker daemon.\n");
                    log.error("Failed to ping docker deamon. Ping response = " + pingResponse);
                    return null;
                }

                // get the docker version
                final Version version = docker.version();
                logMessages.append("  Version: " + version.version() + "\n");
                logMessages.append("  API Version: " + version.apiVersion() + "\n");
                logMessages.append("  OS: " + version.os() + " (" + version.kernelVersion() + ")\n");

                updateMessage(logMessages.toString());

                // get all labels
                List<String> installedProtocols = DlgLoadImageController.getInstalledProtocols(docker, imageName);

                if (installedProtocols.size() == 0) {
                    updateMessage("Protocol image not installed.\n  Please click the button to download it.");
                } else {
                    updateMessage("Protocol image is available\n");
                }

                return installedProtocols;
            } catch (DockerException exception) {
                log.error(exception.toString());
                updateMessage("Error:\n");
                if (exception.toString().contains("Permission denied")) {
                    updateMessage("  Missing permission to connect to docker service.\nPlease run this application as super user.");
                } else {
                    updateMessage("  Failed to connect to docker service.\nPlease make sure Docker is installed and running.\n\n" +
                            "To install Docker visit https://store.docker.com\nOnce Docker is installed and running, please re-start this application.\n");
                }

                // add special windows message
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    updateMessage("\nIf Docker is already running, make sure to enable the option \"Expose " +
                            "daemon on tcp://localhost:2375\" in your Docker settings.");
                }

                return null;
            }
        }
        catch (Exception e) {
            // any other exception
            updateMessage("Error: " + e.getMessage());
            log.error("Error: " + e.getMessage());
            LoggerHelperFunctions.logStackTrace(log, e);
            failed();
            return null;
        }
    }
}
