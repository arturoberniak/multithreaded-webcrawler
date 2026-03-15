package org.example;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.SQLException;

public class CrawlerThread implements Runnable {

    private static final ThreadLocal<Long> lastRequestTime =
            ThreadLocal.withInitial(() -> 0L);

    @Override
    public void run() {
        DBConn db = new DBConn();
        try {
            db.connect();

            String[] claimed = db.claimNextUnvisited();
            if (claimed == null) {
                return;
            }

            String url   = claimed[0];
            int    depth = Integer.parseInt(claimed[1]);

            if (!RobotsCache.getInstance().isAllowed(url)) {
                CrawlerLogger.warn("[" + thread() + "] Blocked by robots.txt: " + url);
                CrawlerStats.getInstance().incrementRobotsBlocked();
                db.updateVisitResult(url, 0, "", 0, "robots.txt disallowed");
                return;
            }

            if (depth > CrawlerConfig.MAX_DEPTH) {
                CrawlerStats.getInstance().incrementDepthBlocked();
                return;
            }

            if (CrawlerStats.getInstance().getPagesVisited() >= CrawlerConfig.MAX_URLS) {
                CrawlerLogger.info("[" + thread() + "] URL cap reached.");
                return;
            }

            rateLimit();

            CrawlerLogger.info("[" + thread() + "] [depth=" + depth + "] " + url);

            Connection.Response response;
            try {
                response = Jsoup.connect(url)
                        .userAgent(CrawlerConfig.USER_AGENT)
                        .timeout(CrawlerConfig.HTTP_TIMEOUT_MS)
                        .ignoreContentType(true)
                        .followRedirects(true)
                        .execute();
            } catch (Exception e) {
                CrawlerLogger.error("[" + thread() + "] Fetch failed: " + url
                        + " – " + e.getMessage());
                CrawlerStats.getInstance().incrementErrors();
                db.updateVisitResult(url, 0, "", 0, e.getMessage());
                return;
            }

            int    statusCode   = response.statusCode();
            String contentType  = response.contentType();
            byte[] bodyBytes    = response.bodyAsBytes();
            CrawlerStats.getInstance().addBytes(bodyBytes.length);

            int linksFound = 0;
            if (contentType != null && contentType.contains("text/html")) {
                Document doc = response.parse();
                Elements links = doc.select("a[href]");
                linksFound = links.size();
                int childDepth = depth + 1;

                for (Element link : links) {
                    String href = link.absUrl("href");
                    if (isValidUrl(href)) {
                        try {
                            db.insertRow(href, childDepth);
                        } catch (SQLException ex) {
                            CrawlerLogger.error("[" + thread()
                                    + "] DB insert failed: " + ex.getMessage());
                        }
                    }
                }
            }

            db.updateVisitResult(url, statusCode, contentType, linksFound, "");
            CrawlerStats.getInstance().incrementPagesVisited();
            CrawlerStats.getInstance().addLinksFound(linksFound);

        } catch (Exception e) {
            CrawlerLogger.error("[" + thread() + "] Unexpected: " + e.getMessage());
            CrawlerStats.getInstance().incrementErrors();
        } finally {
            db.disconnect();
        }
    }


    private void rateLimit() {
        long last = lastRequestTime.get();
        long now  = System.currentTimeMillis();
        long wait = CrawlerConfig.RATE_LIMIT_MS - (now - last);
        if (wait > 0) {
            try { Thread.sleep(wait); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime.set(System.currentTimeMillis());
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.isBlank()
                && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private String thread() {
        return Thread.currentThread().getName();
    }
}
