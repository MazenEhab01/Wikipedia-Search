package com.wikipediasearch;

// ... (keep existing imports)
import com.wikipediasearch.crawler.WebCrawler;
import com.wikipediasearch.invertedIndex.Index5;
import com.wikipediasearch.invertedIndex.Index5.SearchResult;
import com.wikipediasearch.invertedIndex.SourceRecord;
import com.wikipediasearch.invertedIndex.Posting;
import com.wikipediasearch.invertedIndex.DictEntry;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        System.out.println("--- Starting Wikipedia Search Engine ---");

        // ============================================================
        // STEP 1: Run the Web Crawler
        // ============================================================
        System.out.println("Phase 1: Crawling websites...");
        WebCrawler crawler = new WebCrawler();
        List<String> seedUrls = Arrays.asList(
                "https://en.wikipedia.org/wiki/Pharaoh",
                "https.en.wikipedia.org/wiki/List_of_pharaohs",
                "https://en.wikipedia.org/wiki/Ancient_Egypt",
                "https://en.wikipedia.org/wiki/Cleopatra",
                "https://en.wikipedia.org/wiki/Nile"
        );
        Map<String, String> crawledPages = crawler.crawl(seedUrls);
        System.out.println("Crawling complete. Found " + crawledPages.size() + " pages.");
        if (crawledPages == null || crawledPages.isEmpty()) {
            System.err.println("Error: No pages were crawled. Indexing cannot proceed. Exiting.");
            return;
        }

        // ============================================================
        // STEP 2: Build the Inverted Index
        // ============================================================
        System.out.println("\nPhase 2: Building index from crawled data...");
        Index5 index = new Index5();
        index.buildIndex(crawledPages);
        System.out.println("Index built successfully.");

        // --- Optional: Print dictionary sample ---
        // System.out.println("\n--- Printing Dictionary (Sample) ---");
        // index.printDictionary();
        // System.out.println("--- End of Dictionary Printout ---");


        // ============================================================
        // **** MODIFIED: STEP 2.5: Print ALL Document Vectors (TF-IDF) ****
        // ============================================================
        System.out.println("\n--- Printing Document TF-IDF Vectors ---");
        // Removed: printVectorLimit and printedCount variables

        // Get the total number of documents indexed
        int totalDocs = crawledPages.size(); // Or use a getter from Index5 if available

        for (int docId = 0; docId < totalDocs; docId++) {
            // Removed the check: if (printedCount >= printVectorLimit) { ... break; }

            // Ideally, get the URL or identifier for this docId from index.sources
            // Example: SourceRecord sr = index.getSourceRecord(docId); (needs getter in Index5)
            // String docIdentifier = (sr != null) ? sr.getL() : "Unknown Doc ID";
            // System.out.println("\nDocument Vector for: " + docIdentifier + " (ID: " + docId + ")");
            System.out.println("\nDocument Vector for Doc ID: " + docId); // Using ID for now

            Map<String, Double> vector = index.getDocumentTfIdfVector(docId);

            if (vector.isEmpty()) {
                System.out.println("  <Vector is empty or docId not found>");
            } else {
                System.out.println("  Vector contains " + vector.size() + " non-zero term components:");
                // Sort terms alphabetically for consistent display
                List<String> sortedTerms = new ArrayList<>(vector.keySet());
                Collections.sort(sortedTerms);

                int termPrintCount = 0;
                int termPrintLimit = 20; // KEEPING limit on terms *per vector* for readability

                for (String term : sortedTerms) {
                    if(termPrintCount >= termPrintLimit) {
                        System.out.println("    ... (limiting term printout to first " + termPrintLimit + ")");
                        break;
                    }
                    // Print term word and its TF-IDF score
                    System.out.printf("    Term: '%-15s'  TF-IDF: %.6f%n", term, vector.get(term));
                    termPrintCount++;
                }
            }
            // Removed: printedCount++;
        }
        System.out.println("--- End of Document Vector Printout ---");


        // ============================================================
        // STEP 3: Ranked Querying
        // ============================================================
        System.out.println("\nPhase 3: Ranked Search (TF-IDF & Cosine Similarity). Type 'exit' to quit.");
        Scanner scanner = new Scanner(System.in);
        // ... (rest of the query loop remains the same) ...
        while (true) {
            System.out.print("\nEnter search query: ");
            String queryInput = scanner.nextLine();

            if (queryInput == null || queryInput.trim().isEmpty()) {
                System.out.println("Query cannot be empty.");
                continue;
            }
            String trimmedQuery = queryInput.trim();
            if (trimmedQuery.equalsIgnoreCase("exit")) {
                break; // Exit the loop
            }

            // Use the ranked search method from Index5
            List<SearchResult> rankedResults = index.findQueryRanked(trimmedQuery);

            System.out.println("\nRanked Search Results for '" + trimmedQuery + "' (" + rankedResults.size() + " relevant docs found):");

            if (rankedResults.isEmpty()) {
                System.out.println("  <No relevant documents found>");
            } else {
                // Display the top 10 results
                int resultsToShow = Math.min(rankedResults.size(), 10);
                System.out.println("  --- Top " + resultsToShow + " Results ---");
                for (int i = 0; i < resultsToShow; i++) {
                    SearchResult result = rankedResults.get(i);
                    System.out.printf("  Rank %d: %s%n", (i + 1), result);
                }
                if (rankedResults.size() > 10) {
                    System.out.println("  ...");
                }
            }
            System.out.println("----------------------------------------");
        }

        scanner.close();
        System.out.println("\n--- Exiting Search Engine ---");

    } // End of main method

} // End of Main class