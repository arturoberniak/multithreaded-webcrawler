# Web Crawler

A multi-threaded Java web crawler with SQLite storage and comprehensive reporting.

## Features

- Multi-threaded crawling with configurable thread pool
- SQLite database for URL tracking and results
- Respects robots.txt rules
- Rate limiting per thread
- Live dashboard during crawl
- CSV and HTML report generation
- Configurable depth and URL limits

## Requirements

- Java 21
- Maven

## Dependencies

- SQLite JDBC 3.40.0.0
- Jsoup 1.15.3

## Building

```bash
mvn clean package
```

## Running

```bash
java -cp target/classes:~/.m2/repository/org/xerial/sqlite-jdbc/3.40.0.0/sqlite-jdbc-3.40.0.0.jar:~/.m2/repository/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar org.example.WebCrawler [startUrl]
```

Default start URL: https://example.com

## Configuration

Edit `CrawlerConfig.java` to customize:
- Thread pool size
- Maximum crawl depth
- Maximum URLs to visit
- Rate limiting
- Timeouts
- Output file paths

## Output

- `crawler.db` - SQLite database with all crawl results
- `crawler_report.csv` - CSV export of results
- `crawler_report.html` - HTML report with statistics
- `crawler.log` - Detailed log file

## Architecture

- `WebCrawler` - Main entry point and orchestration
- `CrawlerThread` - Worker thread for fetching and parsing pages
- `DBConn` - SQLite database operations
- `CrawlerStats` - Thread-safe statistics tracking
- `RobotsCache` - robots.txt caching and parsing
- `ReportExporter` - CSV and HTML report generation
- `CrawlerLogger` - Thread-safe logging to file and console
- `CrawlerConfig` - Central configuration constants
