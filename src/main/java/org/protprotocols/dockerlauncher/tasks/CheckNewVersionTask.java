package org.protprotocols.dockerlauncher.tasks;

import javafx.concurrent.Task;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.protprotocols.dockerlauncher.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        List<String> availableReleases = GithubReleaseFetcher.fetchReleases(
                properties.getProperty(Constants.PROPERTY_APPLICATION_RELEASE_URL));

        DefaultArtifactVersion runningVersion = new DefaultArtifactVersion(currentVersion);
        List<DefaultArtifactVersion> availableVersions = availableReleases.stream()
                .map(DefaultArtifactVersion::new)
                .sorted()
                .collect(Collectors.toList());

        if (availableVersions.get(availableVersions.size() - 1).compareTo(runningVersion) > 0) {
            return availableVersions.get(availableVersions.size() - 1).toString();
        } else {
            return null;
        }
    }
}
