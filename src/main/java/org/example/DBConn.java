package org.example;

import java.sql.*;

public class DBConn {

    static final String DB_URL = "jdbc:sqlite:crawler.db";
    private Connection connection;

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        connection.setAutoCommit(true);
        try (Statement st = connection.createStatement()) {
            st.setQueryTimeout(30);
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
        }
    }

    public void disconnect() {
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public void createTables() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS links (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    url          TEXT    NOT NULL UNIQUE,
                    depth        INTEGER NOT NULL DEFAULT 0,
                    seen         INTEGER NOT NULL DEFAULT 1,
                    visited      INTEGER NOT NULL DEFAULT 0,
                    status_code  INTEGER NOT NULL DEFAULT 0,
                    content_type TEXT    NOT NULL DEFAULT '',
                    links_found  INTEGER NOT NULL DEFAULT 0,
                    error_msg    TEXT    NOT NULL DEFAULT '',
                    crawled_at   TEXT    NOT NULL DEFAULT ''
                )
                """;
        try (Statement st = connection.createStatement()) {
            st.setQueryTimeout(30);
            st.executeUpdate(sql);
            st.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_visited ON links(visited)");
            st.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_depth   ON links(depth)");
        }
    }

    public void dropTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.setQueryTimeout(30);
            st.executeUpdate("DROP TABLE IF EXISTS links");
        }
    }

    public void insertRow(String url, int depth) throws SQLException {
        String sql = """
                INSERT INTO links (url, depth, seen, visited)
                VALUES (?, ?, 1, 0)
                ON CONFLICT(url) DO UPDATE SET seen = seen + 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, url);
            ps.setInt(2, depth);
            ps.executeUpdate();
        }
    }

    public void insertRow(String url) throws SQLException {
        insertRow(url, 0);
    }

    public String[] claimNextUnvisited() throws SQLException {
        String sql = """
                UPDATE links
                SET    visited = 1
                WHERE  id = (
                           SELECT id FROM links
                           WHERE  visited = 0
                             AND  depth   <= ?
                           ORDER  BY depth ASC
                           LIMIT  1
                       )
                RETURNING url, depth
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, CrawlerConfig.MAX_DEPTH);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            rs.getString("url"),
                            String.valueOf(rs.getInt("depth"))
                    };
                }
            }
        }
        return null;
    }

    public void updateVisitResult(String url, int statusCode, String contentType,
                                   int linksFound, String errorMsg) throws SQLException {
        String sql = """
                UPDATE links
                SET    status_code  = ?,
                       content_type = ?,
                       links_found  = ?,
                       error_msg    = ?,
                       crawled_at   = datetime('now')
                WHERE  url = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, statusCode);
            ps.setString(2, contentType == null ? "" : contentType);
            ps.setInt(3, linksFound);
            ps.setString(4, errorMsg    == null ? "" : errorMsg);
            ps.setString(5, url);
            ps.executeUpdate();
        }
    }

    public int pendingCount() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) AS cnt FROM links " +
                "WHERE visited = 0 AND depth <= ?")) {
            ps.setInt(1, CrawlerConfig.MAX_DEPTH);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        }
    }

    public boolean allVisited() throws SQLException {
        return pendingCount() == 0;
    }
}
