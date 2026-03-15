package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CrawlerLogger {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static PrintWriter fileWriter;

    static {
        try {
            fileWriter = new PrintWriter(
                    new FileWriter(CrawlerConfig.LOG_FILE_PATH, true), true);
        } catch (IOException e) {
            System.err.println("[Logger] Cannot open log file: " + e.getMessage());
        }
    }

    private CrawlerLogger() {}

    public static synchronized void info(String msg) {
        log("INFO ", msg);
    }

    public static synchronized void warn(String msg) {
        log("WARN ", msg);
    }

    public static synchronized void error(String msg) {
        log("ERROR", msg);
    }


    private static void log(String level, String msg) {
        String line = String.format("[%s] [%s] %s",
                LocalDateTime.now().format(FMT), level, msg);
        System.out.println(line);
        if (fileWriter != null) {
            fileWriter.println(line);
        }
    }

    public static synchronized void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}
