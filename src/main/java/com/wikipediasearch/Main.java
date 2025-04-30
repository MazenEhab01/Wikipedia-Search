package com.wikipediasearch;

// Import necessary classes from sub-packages
import com.wikipediasearch.crawler.WebCrawler;
import com.wikipediasearch.invertedIndex.Index5;
import com.wikipediasearch.invertedIndex.Index5.SearchResult;
import com.wikipediasearch.invertedIndex.SourceRecord; // May not be directly needed, but good practice
import com.wikipediasearch.invertedIndex.Posting;    // May not be directly needed
import com.wikipediasearch.invertedIndex.DictEntry;   // May not be directly needed

import java.util.*;
// Removed unused stream import: import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        System.out.println("--- Starting Wikipedia Search Engine ---");

        // ============================================================
        // STEP 1: Run the Web Crawler
        // ============================================================
        System.out.println("Phase 1: Crawling websites...");
        WebCrawler crawler = new WebCrawler();

        // ***** CORRECTED: Use EXACT seed URLs from assignment spec *****
        List<String> seedUrls = Arrays.asList(
                "https://en.wikipedia.org/wiki/List_of_pharaohs",
                "https://en.wikipedia.org/wiki/Pharaoh"
        );

        // Crawl starting from the seeds
        Map<String, String> crawledPages = crawler.crawl(seedUrls); // Max 10 pages constraint is inside crawler

        System.out.println("Crawling complete. Successfully processed " + crawledPages.size() + " pages.");
        if (crawledPages.isEmpty()) {
            System.err.println("Error: No pages were crawled successfully. Indexing cannot proceed. Exiting.");
            return; // Exit if crawling failed completely
        }

        // ============================================================
        // STEP 2: Build the Inverted Index
        // ============================================================
        System.out.println("\nPhase 2: Building index from crawled data...");
        Index5 index = new Index5(); // Index5 now uses the external calculator classes internally
        index.buildIndex(crawledPages);

        if (index.getNumberOfDocuments() == 0) {
            System.err.println("Error: Index built, but contains 0 documents. Cannot search. Exiting.");
            return;
        }
        System.out.println("Index built successfully: " + index.getIndexSize() + " terms, " + index.getNumberOfDocuments() + " documents.");


        // --- Optional: Print dictionary sample ---
//         index.printDictionary();

        // --- Optional: Print Document Vectors (TF-IDF) for Debugging ---

//        System.out.println("\n--- Printing Document TF-IDF Vectors (Sample) ---");
//        int totalDocs = index.getNumberOfDocuments();
//        int vectorsToPrint = Math.min(totalDocs, 5); // Limit printout
//        for (int docId = 0; docId < vectorsToPrint; docId++) {
//            SourceRecord sr = index.getSourceRecord(docId);
//            String docIdentifier = (sr != null) ? sr.getUrl() : "Unknown Doc ID";
//            System.out.println("\nDocument Vector for: " + docIdentifier + " (ID: " + docId + ")");
//            System.out.printf("  Pre-calculated Magnitude (Norm): %.6f%n", index.getDocumentMagnitude(docId));
//
//            Map<String, Double> vector = index.getDocumentTfIdfVector(docId);
//            if (vector.isEmpty()) {
//                System.out.println("  <Vector is empty or docId not found>");
//            } else {
//                System.out.println("  Vector contains " + vector.size() + " non-zero term components (showing top 20):");
//                List<Map.Entry<String, Double>> sortedVector = new ArrayList<>(vector.entrySet());
//                // Sort by TF-IDF score descending for more informative view
//                sortedVector.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
//
//                int termPrintCount = 0;
//                int termPrintLimit = 20;
//                for (Map.Entry<String, Double> entry : sortedVector) {
//                     if (termPrintCount >= termPrintLimit) {
//                        System.out.println("    ... (limiting term printout)");
//                        break;
//                    }
//                    System.out.printf("    Term: '%-15s'  TF-IDF: %.6f%n", entry.getKey(), entry.getValue());
//                    termPrintCount++;
//                }
//            }
//        }
//         if (totalDocs > vectorsToPrint) System.out.println("... (Limit reached)");
//        System.out.println("--- End of Document Vector Printout ---");

        // ============================================================
        // STEP 3: Ranked Querying
        // ============================================================
        System.out.println("\nPhase 3: Ranked Search (TF-IDF & Cosine Similarity). Type 'exit' to quit.");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            int choice = -1;
            System.out.print("\nSearch query(0), Boolean And Query(1): ");
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter 0 or 1.");
                continue; // Skip to next iteration
            }
            String queryInput = scanner.nextLine();
            if(choice == 0){
                if (queryInput == null) { // Handle potential null input if Scanner has issues
                    System.out.println("Received null input, exiting.");
                    break;
                }
                String trimmedQuery = queryInput.trim();
                if (trimmedQuery.equalsIgnoreCase("exit")) {
                    break; // Exit the loop
                }
                if (trimmedQuery.isEmpty()) {
                    System.out.println("Query cannot be empty.");
                    continue;
                }

                // Perform ranked search using Index5 method
                List<SearchResult> rankedResults = index.findQueryRanked(trimmedQuery);

                System.out.println("\nRanked Search Results for '" + trimmedQuery + "' (" + rankedResults.size() + " relevant docs found):");

                if (rankedResults.isEmpty()) {
                    System.out.println("  <No relevant documents found for this query>");
                } else {
                    // Display the top 10 results (or fewer if less than 10 found)
                    int resultsToShow = Math.min(rankedResults.size(), 10);
                    System.out.println("  --- Top " + resultsToShow + " Results ---");
                    for (int i = 0; i < resultsToShow; i++) {
                        SearchResult result = rankedResults.get(i);
                        System.out.printf("  Rank %2d: %s%n", (i + 1), result); // Use result.toString()
                    }
                    if (rankedResults.size() > resultsToShow) {
                        System.out.println("  ... (showing top " + resultsToShow + " of " + rankedResults.size() + ")");
                    }
                }
                System.out.println("----------------------------------------");
            }
            else{

            }
            scanner.close();
            System.out.println("\n--- Exiting Search Engine ---");
            }


    } // End of main method
} // End of Main class