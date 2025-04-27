package com.wikipediasearch.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.*;

public class WebCrawler {

    // Configuration
    private static final int MAX_PAGES_TO_CRAWL = 10;
    private static final int DELAY_MS = 1000;  //politeness
    private static final String WIKIPEDIA_PREFIX = "https://en.wikipedia.org/wiki/";

    // data structures
    private Queue<String> urlsToVisit; // URLs waiting to be crawled
    private Set<String> visitedUrls;   // URLs already crawled or attempted
    private Map<String, String> crawledPages; // Stores results: URL -> Page Text

    // Constructor
    public WebCrawler() {
        urlsToVisit = new LinkedList<>();
        visitedUrls = new HashSet<>();
        crawledPages = new HashMap<>();
    }


    public Map<String, String> crawl(List<String> seedUrls) {
        // Add initial seeds to the queue
        for (String seed : seedUrls) {
            if (isValidUrl(seed)) {
                urlsToVisit.add(seed);
            }
        }

        // Loop while queue has URLs and page limit not reached
        while (!urlsToVisit.isEmpty() && visitedUrls.size() < MAX_PAGES_TO_CRAWL) {
            String currentUrl = urlsToVisit.poll(); // Get next URL from queue

            // Skip if already visited or outside scope
            if (visitedUrls.contains(currentUrl)) continue;
            if (!currentUrl.startsWith(WIKIPEDIA_PREFIX)) continue;

            try {
                // Mark as visited *before* fetching
                visitedUrls.add(currentUrl);
                System.out.println("Crawling (" + visitedUrls.size() + "/" + MAX_PAGES_TO_CRAWL + "): " + currentUrl);

                // Fetch the page content using Jsoup
                Connection.Response response = Jsoup.connect(currentUrl)
                        .userAgent("assignmentCrawler/1.0")
                        .timeout(5000)
                        .execute();

                // Skip if content is not HTML
                if (!response.contentType().toLowerCase().contains("text/html")) continue;

                // Parse HTML
                Document doc = response.parse();
                // Extract text from the body
                String pageText = doc.body().text();
                // Store URL and extracted text
                crawledPages.put(currentUrl, pageText);

                // Find all links on the page
                Elements linksOnPage = doc.select("a[href]");
                // Process each link
                for (Element link : linksOnPage) {
                    String absUrl = link.absUrl("href"); // Get absolute URL
                    // If link is valid and not seen, add to queue
                    if (isValidLink(absUrl)) {
                        urlsToVisit.add(absUrl);
                    }
                }

                // Wait before next request (politeness)
                Thread.sleep(DELAY_MS);

            } catch (IOException | InterruptedException | IllegalArgumentException e) {
                System.err.println("Error or skip for URL " + currentUrl + ": " + e.getMessage());
            }
        }

        System.out.println("\nCrawling complete. Visited " + visitedUrls.size() + " unique pages.");
        System.out.println("Stored content for " + crawledPages.size() + " pages.");
        return crawledPages;
    }

    /** Checks basic URL validity (starts with http). */
    private boolean isValidUrl(String url) {
        return url != null && !url.isEmpty() && url.startsWith("http");
    }

    // Checks if a link meets crawling criteria (Wikipedia scope, no fragments, not special page, not visited).

    private boolean isValidLink(String url) {
        if (!isValidUrl(url)) return false;
        if (!url.startsWith(WIKIPEDIA_PREFIX)) return false;
        if (url.contains("#")) return false;
        // Ignore non-article namespaces (Talk, User, File, etc.)
        if (url.matches(".*/wiki/(Talk|User|Special|File|Wikipedia|Help|Template|Portal|Category):.*")) return false;
        if (visitedUrls.contains(url)) return false;           // Ignore if already visited

        return true;
    }

    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler();
        List<String> seeds = Arrays.asList(
                "https://en.wikipedia.org/wiki/Pharaoh",
                "https://en.wikipedia.org/wiki/List_of_pharaohs"
        );
        Map<String, String> results = crawler.crawl(seeds);

        System.out.println("\n--- Crawled Pages (" + results.size() + ") ---");
        results.keySet().forEach(System.out::println); // Concise way to print keys
    }
}