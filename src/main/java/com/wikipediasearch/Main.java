    package com.wikipediasearch;
    import com.wikipediasearch.crawler.WebCrawler;
    import com.wikipediasearch.invertedIndex.DictEntry;
    import com.wikipediasearch.invertedIndex.Index5;
    import com.wikipediasearch.invertedIndex.Posting;
    import com.wikipediasearch.invertedIndex.SourceRecord;
    import com.wikipediasearch.invertedIndex.Stemmer; // Only if SearchEngine uses it directly

    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.util.Arrays;
    import java.util.List;
    import java.util.Map;
    import java.util.Scanner;
    import java.util.Scanner;

    public class Main {
    public static void main(String[] args) {

        System.out.println("--- Starting Wikipedia Search Engine ---");

        // ============================================================
        // STEP 1: Run the Web Crawler to get page data
        // ============================================================
        System.out.println("Phase 1: Crawling websites...");

        WebCrawler crawler = new WebCrawler(); // Instantiate your crawler

        // Define the starting URLs
        List<String> seedUrls = Arrays.asList(
                "https://en.wikipedia.org/wiki/Pharaoh",
                "https://en.wikipedia.org/wiki/List_of_pharaohs"
        );

        // Execute the crawl and get the results
        Map<String, String> crawledPages = crawler.crawl(seedUrls);

        System.out.println("Crawling complete. Found " + crawledPages.size() + " pages.");

        // Check if crawling was successful
        if (crawledPages.isEmpty()) {
            System.err.println("Error: No pages were crawled. Indexing cannot proceed. Exiting.");
            return; // Exit if no data
        }

        // ============================================================
        // STEP 2: Build the Inverted Index using the revised Index5
        // ============================================================
        System.out.println("\nPhase 2: Building index from crawled data...");

        Index5 index = new Index5(); // Instantiate the revised indexer

        // Build the index using the map from the crawler
        index.buildIndex(crawledPages);

        System.out.println("Index built successfully.");

        // --- CRUCIAL FOR TESTING ---
        // Print the dictionary to visually inspect the index
        System.out.println("\n--- Printing Dictionary (Sample) ---");
        index.printDictionary(); // Let's see what the index looks like
        System.out.println("--- End of Dictionary Printout ---");


        // ============================================================
        // STEP 3: Basic Querying (Boolean AND - for testing index)
        // This uses the EXISTING search capability of Index5 to test it.
        // Later, this section will be replaced/augmented with TF-IDF cosine similarity.
        // ============================================================
        System.out.println("\nPhase 3: Basic Boolean Search Test (type 'exit' to quit).");
        // Using Scanner for cleaner input reading compared to BufferedReader for simple cases
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter Boolean AND search phrase (e.g., 'ancient egypt pharaoh'): ");
            String queryInput = scanner.nextLine();

            if (queryInput == null || queryInput.trim().isEmpty()) {
                System.out.println("Query cannot be empty.");
                continue;
            }
            if (queryInput.trim().equalsIgnoreCase("exit")) {
                break; // Exit the loop
            }

            // Use the boolean AND search method from Index5
            // This tests if terms are indexed and if intersection works
            String booleanResult = index.findQueryBooleanAnd(queryInput);

            System.out.println("\nBoolean Model Result:");
            System.out.println(booleanResult);
            System.out.println("--------------------");
        }

        scanner.close(); // Close the scanner
        System.out.println("\n--- Exiting Search Engine ---");

    } // End of main method

} // End of SearchEngine class
