package org.protprotocols.dockerlauncher.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Only checks whether a new version is available
 */
public class CheckNewVersionTask extends Task<String> {
    private final Logger log = LoggerFactory.getLogger(CheckNewVersionTask.class);
    private final Properties properties;

    public CheckNewVersionTask(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected String call() throws Exception {
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
        } else {
            return null;
        }
    }
}
