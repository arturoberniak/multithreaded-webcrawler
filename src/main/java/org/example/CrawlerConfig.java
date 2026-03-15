package org.example;

public final class CrawlerConfig {

    private CrawlerConfig() {}

    public static final int THREAD_POOL_SIZE = 6;
    public static final int CORE_POOL_SIZE = 2;
    public static final int MAX_DEPTH = 3;
    public static final int MAX_URLS = 500;
    public static final int HTTP_TIMEOUT_MS = 6_000;
    public static final String USER_AGENT =
            "Mozilla/5.0 (compatible; AdvancedCrawlerBot/2.0; +https://github.com/example/crawler)";
    public static final long RATE_LIMIT_MS = 500;
    public static final boolean RESPECT_ROBOTS_TXT = true;
    public static final long DASHBOARD_REFRESH_MS = 3_000;
    public static final String CSV_OUTPUT_PATH = "crawler_report.csv";
    public static final String HTML_OUTPUT_PATH = "crawler_report.html";
    public static final String LOG_FILE_PATH = "crawler.log";
    public static final int MAX_IDLE_ROUNDS = 5;
    public static final long POLL_INTERVAL_MS = 2_000;
}
