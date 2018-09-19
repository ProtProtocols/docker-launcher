package org.protprotocols.dockerlauncher.util;

import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LoggerHelperFunctions {
    private LoggerHelperFunctions() {

    }

    /**
     * Prints an Exception's stack trace to the "debug" of the passed logger
     * @param logger
     * @param e
     */
    public static void logStackTrace(Logger logger, Exception e) {
        if (!logger.isDebugEnabled()) {
            return;
        }

        // get the stack trace as string
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        logger.debug(writer.toString());
    }
}
