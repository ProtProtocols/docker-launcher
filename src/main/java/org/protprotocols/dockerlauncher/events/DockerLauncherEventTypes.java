package org.protprotocols.dockerlauncher.events;

import javafx.event.EventType;

public class DockerLauncherEventTypes {
    private DockerLauncherEventTypes() {

    }

    public static EventType<ContainerLogEvent> CONTAINER_LOG = new EventType<ContainerLogEvent>("CONTANER_LOG");
}
