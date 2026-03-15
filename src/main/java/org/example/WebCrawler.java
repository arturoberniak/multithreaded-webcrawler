package org.example;

import java.sql.SQLException;
import java.util.concurrent.*;

public class WebCrawler {

    public static void main(String... args) {
        String url = "https://example.com";
        if (args.length == 1) {
            url = args[0];
        }
        startCrawler(url);
    }


    public static void startCrawler(String startUrl) {
        CrawlerLogger.info("=== Advanced Web Crawler v2.0 ===");
        CrawlerLogger.info("Seed     : " + startUrl);
        CrawlerLogger.info("Threads  : " + CrawlerConfig.THREAD_POOL_SIZE);
        CrawlerLogger.info("MaxDepth : " + CrawlerConfig.MAX_DEPTH);
        CrawlerLogger.info("MaxURLs  : " + CrawlerConfig.MAX_URLS);

        DBConn setupDb = new DBConn();
        try {
            setupDb.connect();
            setupDb.dropTables();
            setupDb.createTables();
            setupDb.insertRow(startUrl, 0);
            CrawlerLogger.info("Database initialised.");
        } catch (SQLException e) {
            CrawlerLogger.error("DB init failed: " + e.getMessage());
            return;
        } finally {
            setupDb.disconnect();
        }

        ExecutorService crawlPool = new ThreadPoolExecutor(
                CrawlerConfig.CORE_POOL_SIZE,
                CrawlerConfig.THREAD_POOL_SIZE,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(CrawlerConfig.THREAD_POOL_SIZE * 2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        ScheduledExecutorService dashboard =
                Executors.newSingleThreadScheduledExecutor();
        dashboard.scheduleAtFixedRate(() -> {
            try {
                DBConn dbc = new DBConn();
                dbc.connect();
                int pending = dbc.pendingCount();
                dbc.disconnect();
                System.out.println(CrawlerStats.getInstance().dashboard(pending));
            } catch (SQLException ignored) {}
        }, CrawlerConfig.DASHBOARD_REFRESH_MS,
           CrawlerConfig.DASHBOARD_REFRESH_MS,
           TimeUnit.MILLISECONDS);

        int idleRounds = 0;

        try {
            while (idleRounds < CrawlerConfig.MAX_IDLE_ROUNDS
                    && CrawlerStats.getInstance().getPagesVisited()
                       < CrawlerConfig.MAX_URLS) {

                DBConn checkDb = new DBConn();
                int pending;
                try {
                    checkDb.connect();
                    pending = checkDb.pendingCount();
                } catch (SQLException e) {
                    CrawlerLogger.error("DB check failed: " + e.getMessage());
                    break;
                } finally {
                    checkDb.disconnect();
                }

                if (pending > 0) {
                    idleRounds = 0;
                    int toSubmit = Math.min(pending, CrawlerConfig.THREAD_POOL_SIZE);
                    for (int i = 0; i < toSubmit; i++) {
                        crawlPool.submit(new CrawlerThread());
                    }
                } else {
                    idleRounds++;
                    CrawlerLogger.info("Idle round " + idleRounds
                            + "/" + CrawlerConfig.MAX_IDLE_ROUNDS);
                }

                TimeUnit.MILLISECONDS.sleep(CrawlerConfig.POLL_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CrawlerLogger.warn("Main thread interrupted.");
        }

        dashboard.shutdownNow();
        crawlPool.shutdown();
        try {
            if (!crawlPool.awaitTermination(30, TimeUnit.SECONDS)) {
                crawlPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            crawlPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            DBConn finalDb = new DBConn();
            finalDb.connect();
            System.out.println(CrawlerStats.getInstance().dashboard(finalDb.pendingCount()));
            finalDb.disconnect();
        } catch (SQLException ignored) {}

        ReportExporter.exportCsv(DBConn.DB_URL);
        ReportExporter.exportHtml(DBConn.DB_URL);

        CrawlerLogger.info("Crawl complete. Results: crawler.db | "
                + CrawlerConfig.CSV_OUTPUT_PATH + " | "
                + CrawlerConfig.HTML_OUTPUT_PATH);
        CrawlerLogger.close();
    }
}
