package org.protprotocols.dockerlauncher.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * Event issues when a container log changes.
 */
public class ContainerLogEvent extends Event {
    private final String logMessage;
    public ContainerLogEvent(EventType<? extends Event> eventType, String logMessage) {
        super(eventType);
        this.logMessage = logMessage;
    }

    public String getLogMessage() {
        return logMessage;
    }
}
