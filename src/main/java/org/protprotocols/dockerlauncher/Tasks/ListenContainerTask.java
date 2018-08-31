package org.protprotocols.dockerlauncher.Tasks;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ListenContainerTask extends Task {
    private final static Logger log = LoggerFactory.getLogger(ListenContainerTask.class);
    private final TextArea textArea;
    private final String containerId;

    public ListenContainerTask(TextArea textArea, String containerId) {
        this.textArea = textArea;
        this.containerId = containerId;
    }

    @Override
    protected Object call() throws Exception {
        log.debug("Starting to listen for container output...");
        int nPreviousLines = 0;

        try (DockerClient docker = DefaultDockerClient.fromEnv().build()) {
            while (true) {
                if (isCancelled() || isDone()) {
                    log.debug("Listening task cancelled");
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
            log.warn("Listening thread failed: " + e.getMessage());
        }

        return null;
    }
}
