package org.protprotocols.dockerlauncher.tasks;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.protprotocols.dockerlauncher.events.ContainerLogEvent;
import org.protprotocols.dockerlauncher.events.DockerLauncherEventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ListenContainerTask extends Task<Void> {
    private final static Logger log = LoggerFactory.getLogger(ListenContainerTask.class);
    private final String containerId;

    public ListenContainerTask(String containerId) {
        this.containerId = containerId;
    }

    @Override
    protected Void call() {
        log.debug("Starting to listen for container output...");
        int nPreviousLines = 0;

        try (DockerClient docker = DefaultDockerClient.fromEnv().build()) {
            while (true) {
                if (isCancelled() || isDone()) {
                    log.debug("Listening task cancelled");
                    return null;
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    log.debug("Listening task cancelled");
                    return null;
                }

                try (LogStream stream = docker.logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
                    final String logs;
                    logs = stream.readFully();
                    String[] filteredLog = Arrays.stream(logs.split("\n")).filter(s -> !s.toLowerCase().contains("head")).toArray(String[]::new);

                    // remove all lines that we already head
                    int nLength = filteredLog.length;

                    if (nLength <= nPreviousLines) {
                        continue;
                    }

                    filteredLog = Arrays.copyOfRange(filteredLog, nPreviousLines, nLength);
                    nPreviousLines = nLength;

                    final String logMessage = String.join("\n", filteredLog) + "\n";

                    if (filteredLog.length > 0) {
                        Platform.runLater(() ->
                                fireEvent(new ContainerLogEvent(DockerLauncherEventTypes.CONTAINER_LOG, logMessage)));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Listening thread failed: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
