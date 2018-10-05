package org.protprotocols.dockerlauncher.tasks;

import javafx.concurrent.Task;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fetches the available image versions based on the GitHub tags.
 */
public class GetAvailableImageVersionsTask extends Task<List<String>> {
    private final String releaseUrl;

    public GetAvailableImageVersionsTask(String releaseUrl) {
        this.releaseUrl = releaseUrl;
    }

    @Override
    protected List<String> call() throws Exception {
        // get the available release tags
        List<String> releaseTags = GithubReleaseFetcher.fetchReleases(releaseUrl);

        // add the "release-" prefix
        return(releaseTags.stream().map(version -> "release-" + version).collect(Collectors.toList()));
    }
}
