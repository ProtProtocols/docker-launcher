package org.protprotocols.dockerlauncher.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to fetch GitHubReleases
 */
public class GithubReleaseFetcher {
    private static Logger log = LoggerFactory.getLogger(GithubReleaseFetcher.class);
    /**
     * Fetches the available releases from the defined url
     * @param releaseUrl The URL to fetch the releases from.
     * @return A list of Strings containing the release tags.
     */
    public static List<String> fetchReleases(String releaseUrl) throws IOException {
        log.debug("Fetching releases from " + releaseUrl);

        // get the latest release
        URL url = new URL(releaseUrl);
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

        return availableReleases;
    }
}
