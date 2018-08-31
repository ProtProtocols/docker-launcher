package org.protprotocols.dockerlauncher.Tasks;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ListenContainerTask extends Task {
    private final static Logger log = Logger.getLogger(ListenContainerTask.class.getName());
    private final TextArea textArea;
    private final String containerId;

    public ListenContainerTask(TextArea textArea, String containerId) {
        this.textArea = textArea;
        this.containerId = containerId;
    }

    @Override
    protected Object call() throws Exception {
        log.info("Starting to listen for container output...");
        int nPreviousLines = 0;

        try (DockerClient docker = DefaultDockerClient.fromEnv().build()) {
            while (true) {
                if (isCancelled() || isDone()) {
                    log.info("Listening task cancelled");
                    return null;
                }

                TimeUnit.MILLISECONDS.sleep(300);

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

                    if (filteredLog.length > 0) {
                        textArea.appendText(String.join("\n", filteredLog));
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Listening thread failed: " + e.getMessage());
        }

        return null;
    }
}
