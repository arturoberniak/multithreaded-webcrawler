package org.example;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class CrawlerStats {

    private static final CrawlerStats INSTANCE = new CrawlerStats();

    private final AtomicInteger pagesVisited   = new AtomicInteger(0);
    private final AtomicInteger linksFound     = new AtomicInteger(0);
    private final AtomicInteger errors         = new AtomicInteger(0);
    private final AtomicInteger robotsBlocked  = new AtomicInteger(0);
    private final AtomicInteger depthBlocked   = new AtomicInteger(0);
    private final AtomicLong    bytesDownloaded = new AtomicLong(0);
    private final long          startTime      = System.currentTimeMillis();

    private CrawlerStats() {}

    public static CrawlerStats getInstance() { return INSTANCE; }

    public void incrementPagesVisited()         { pagesVisited.incrementAndGet(); }
    public void addLinksFound(int n)            { linksFound.addAndGet(n); }
    public void incrementErrors()               { errors.incrementAndGet(); }
    public void incrementRobotsBlocked()        { robotsBlocked.incrementAndGet(); }
    public void incrementDepthBlocked()         { depthBlocked.incrementAndGet(); }
    public void addBytes(long n)                { bytesDownloaded.addAndGet(n); }

    public int  getPagesVisited()               { return pagesVisited.get(); }
    public int  getLinksFound()                 { return linksFound.get(); }
    public int  getErrors()                     { return errors.get(); }
    public int  getRobotsBlocked()              { return robotsBlocked.get(); }
    public int  getDepthBlocked()               { return depthBlocked.get(); }
    public long getBytesDownloaded()            { return bytesDownloaded.get(); }

    public long elapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1_000;
    }

    public String dashboard(int queueSize) {
        long secs = elapsedSeconds();
        double kbps = secs > 0
                ? (bytesDownloaded.get() / 1024.0) / secs : 0.0;
        return String.format(
                """
                ╔══════════════════════════════════════╗
                ║         WEB CRAWLER  DASHBOARD       ║
                ╠══════════════════════════════════════╣
                ║  Elapsed          : %6d s          ║
                ║  Pages visited    : %6d            ║
                ║  Links found      : %6d            ║
                ║  Queue remaining  : %6d            ║
                ║  Errors           : %6d            ║
                ║  Robots-blocked   : %6d            ║
                ║  Depth-blocked    : %6d            ║
                ║  Downloaded       : %6.1f KB        ║
                ║  Throughput       : %6.1f KB/s      ║
                ╚══════════════════════════════════════╝
                """,
                secs,
                pagesVisited.get(),
                linksFound.get(),
                queueSize,
                errors.get(),
                robotsBlocked.get(),
                depthBlocked.get(),
                bytesDownloaded.get() / 1024.0,
                kbps);
    }
}
