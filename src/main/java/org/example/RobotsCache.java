package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RobotsCache {

    private static final RobotsCache INSTANCE = new RobotsCache();

    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    private RobotsCache() {}

    public static RobotsCache getInstance() { return INSTANCE; }

    public boolean isAllowed(String rawUrl) {
        if (!CrawlerConfig.RESPECT_ROBOTS_TXT) return true;
        try {
            URI uri  = new URI(rawUrl);
            String host  = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            String path  = uri.getPath() == null ? "/" : uri.getPath();

            List<String> disallowed = cache.computeIfAbsent(host, this::fetchDisallowed);

            for (String prefix : disallowed) {
                if (path.startsWith(prefix)) {
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            return true;
        }
    }


    private List<String> fetchDisallowed(String hostUrl) {
        List<String> disallowed = new ArrayList<>();
        String robotsUrl = hostUrl + "/robots.txt";
        try {
            URL url = new URL(robotsUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(4_000);
            conn.setReadTimeout(4_000);
            conn.setRequestProperty("User-Agent", CrawlerConfig.USER_AGENT);
            conn.connect();

            boolean applicable = false;
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.toLowerCase().startsWith("user-agent:")) {
                        String agent = line.substring(11).trim();
                        applicable = agent.equals("*")
                                || CrawlerConfig.USER_AGENT.toLowerCase()
                                        .contains(agent.toLowerCase());
                    } else if (applicable && line.toLowerCase().startsWith("disallow:")) {
                        String path = line.substring(9).trim();
                        if (!path.isEmpty()) disallowed.add(path);
                    }
                }
            }
            CrawlerLogger.info("[Robots] Loaded " + disallowed.size()
                    + " rule(s) for " + hostUrl);
        } catch (Exception e) {
            CrawlerLogger.warn("[Robots] Could not fetch " + robotsUrl
                    + " – " + e.getMessage());
        }
        return disallowed;
    }
}
