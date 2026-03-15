package org.example;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ReportExporter {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReportExporter() {}

    public static void exportCsv(String dbUrl) {
        CrawlerLogger.info("[Report] Writing CSV → " + CrawlerConfig.CSV_OUTPUT_PATH);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(
                     "SELECT url, depth, seen, visited, status_code, " +
                     "content_type, links_found, error_msg, crawled_at " +
                     "FROM links ORDER BY id");
             PrintWriter pw  = new PrintWriter(
                     new FileWriter(CrawlerConfig.CSV_OUTPUT_PATH))) {

            pw.println("url,depth,seen,visited,status_code,content_type," +
                       "links_found,error_msg,crawled_at");
            while (rs.next()) {
                pw.printf("\"%s\",%d,%d,%d,%d,\"%s\",%d,\"%s\",\"%s\"%n",
                        escape(rs.getString("url")),
                        rs.getInt("depth"),
                        rs.getInt("seen"),
                        rs.getInt("visited"),
                        rs.getInt("status_code"),
                        escape(rs.getString("content_type")),
                        rs.getInt("links_found"),
                        escape(rs.getString("error_msg")),
                        escape(rs.getString("crawled_at")));
            }
            CrawlerLogger.info("[Report] CSV written.");
        } catch (Exception e) {
            CrawlerLogger.error("[Report] CSV export failed: " + e.getMessage());
        }
    }

    public static void exportHtml(String dbUrl) {
        CrawlerLogger.info("[Report] Writing HTML → " + CrawlerConfig.HTML_OUTPUT_PATH);
        CrawlerStats s = CrawlerStats.getInstance();
        String generated = LocalDateTime.now().format(FMT);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement  st   = conn.createStatement();
             PrintWriter pw  = new PrintWriter(
                     new FileWriter(CrawlerConfig.HTML_OUTPUT_PATH))) {

            pw.println("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                    <meta charset="UTF-8">
                    <title>Web Crawler Report</title>
                    <style>
                      body  { font-family: Arial, sans-serif; margin: 24px;
                              background: #f5f5f5; color: #222; }
                      h1    { color: #2c3e50; }
                      .stats{ display:flex; gap:16px; flex-wrap:wrap; margin-bottom:24px; }
                      .card { background:#fff; border-radius:8px; padding:16px 24px;
                              box-shadow:0 1px 4px rgba(0,0,0,.15); min-width:160px; }
                      .card h3{ margin:0 0 6px; font-size:.85rem; color:#888; }
                      .card p { margin:0; font-size:1.6rem; font-weight:700; color:#2c3e50; }
                      table { border-collapse:collapse; width:100%;
                              background:#fff; border-radius:8px; overflow:hidden;
                              box-shadow:0 1px 4px rgba(0,0,0,.15); }
                      th    { background:#2c3e50; color:#fff; padding:10px 12px;
                              text-align:left; font-size:.85rem; }
                      td    { padding:8px 12px; font-size:.82rem;
                              border-bottom:1px solid #eee; word-break:break-all; }
                      tr:hover td { background:#f0f4f8; }
                      .ok   { color:#27ae60; font-weight:700; }
                      .err  { color:#e74c3c; font-weight:700; }
                    </style>
                    </head>
                    <body>
                    """);

            pw.println("<h1>🕷 Web Crawler Report</h1>");
            pw.println("<p>Generated: " + generated + "</p>");

            pw.println("<div class=\"stats\">");
            card(pw, "Pages Visited",   s.getPagesVisited());
            card(pw, "Links Found",     s.getLinksFound());
            card(pw, "Errors",          s.getErrors());
            card(pw, "Elapsed (s)",     s.elapsedSeconds());
            card(pw, "Downloaded (KB)", s.getBytesDownloaded() / 1024);
            pw.println("</div>");

            pw.println("""
                    <table>
                    <thead>
                      <tr>
                        <th>#</th><th>URL</th><th>Depth</th><th>Seen</th>
                        <th>Status</th><th>Content-Type</th>
                        <th>Links Found</th><th>Error</th><th>Crawled At</th>
                      </tr>
                    </thead>
                    <tbody>
                    """);

            ResultSet rs = st.executeQuery(
                    "SELECT rowid, url, depth, seen, visited, status_code, " +
                    "content_type, links_found, error_msg, crawled_at " +
                    "FROM links ORDER BY id");
            int row = 0;
            while (rs.next()) {
                row++;
                int code = rs.getInt("status_code");
                String cls = (code >= 200 && code < 300) ? "ok" : "err";
                pw.printf("""
                        <tr>
                          <td>%d</td>
                          <td><a href="%s" target="_blank">%s</a></td>
                          <td>%d</td><td>%d</td>
                          <td class="%s">%d</td>
                          <td>%s</td><td>%d</td>
                          <td>%s</td><td>%s</td>
                        </tr>%n""",
                        row,
                        esc(rs.getString("url")),
                        truncate(esc(rs.getString("url")), 70),
                        rs.getInt("depth"),
                        rs.getInt("seen"),
                        cls, code,
                        esc(rs.getString("content_type")),
                        rs.getInt("links_found"),
                        esc(rs.getString("error_msg")),
                        esc(rs.getString("crawled_at")));
            }

            pw.println("</tbody></table></body></html>");
            CrawlerLogger.info("[Report] HTML written (" + row + " rows).");
        } catch (Exception e) {
            CrawlerLogger.error("[Report] HTML export failed: " + e.getMessage());
        }
    }


    private static void card(PrintWriter pw, String label, long value) {
        pw.printf("""
                <div class="card"><h3>%s</h3><p>%d</p></div>%n""", label, value);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;");
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
