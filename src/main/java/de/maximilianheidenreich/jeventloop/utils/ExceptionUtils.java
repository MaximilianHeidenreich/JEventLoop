package de.maximilianheidenreich.jeventloop.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {

    /**
     * Converts the full stacktrace into a string.
     * @param exception
     *          The exception from which to get the stacktrace
     * @return
     *          The full stacktrace as a string
     */
    public static String getStackTraceAsString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        String converted = sw.toString();
        try {
            sw.close();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return converted;
    }

}
