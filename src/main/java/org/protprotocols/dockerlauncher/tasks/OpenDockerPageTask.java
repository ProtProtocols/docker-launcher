package org.protprotocols.dockerlauncher.tasks;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import org.protprotocols.dockerlauncher.gui.DockerLauncherGuiApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This Task simply waits until the Docker container's (or any other)
 * website becomes available and then opens the website.
 */
public class OpenDockerPageTask extends Task<Void> {
    private final String targetAddress;
    private final static Logger log = LoggerFactory.getLogger(OpenDockerPageTask.class);
    private final static int MAX_ITERATIONS = 10;

    public OpenDockerPageTask(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    @Override
    protected Void call() {
        try {
            int nIterations = 0;
            URL targetUrl = new URL(targetAddress);

            while (nIterations < MAX_ITERATIONS) {
                try {
                    HttpURLConnection request = (HttpURLConnection) targetUrl.openConnection();
                    int code = request.getResponseCode();
                    log.debug("Testing server connection got response " + String.valueOf(code));

                    if (code == HttpURLConnection.HTTP_OK) {
                        HostServices hostServices = new DockerLauncherGuiApplication().getHostServices();
                        hostServices.showDocument(targetAddress);
                        break;
                    }
                }
                catch (IOException e) {
                    // just try again
                }

                // try every 0.5 sec
                nIterations++;
                Thread.sleep(500);
            }
        }
        catch(MalformedURLException e) {
            log.debug("Invalid URL passed to OpenDockerPage task: " + e.getMessage());
            return null;
        }
        catch (InterruptedException e) {
            // stop in case the thread was interrupted
            log.debug("OpenDockerPageTask was interrupted. Exiting.");
            return null;
        }

        return null;
    }
}
