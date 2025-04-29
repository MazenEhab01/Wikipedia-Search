package com.wikipediasearch.invertedIndex; // Ensure this matches your package

import java.io.BufferedReader;
import java.io.FileReader; // Keep for load/store if adapted later
import java.io.FileWriter; // Keep for load/store if adapted later
import java.io.Writer;     // Keep for load/store if adapted later
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList; // Added for intersect logic adaptation
import java.util.Collections; // Added for intersect logic adaptation
import java.util.Comparator; // Added for intersect logic adaptation


/**
 * Builds and manages an inverted index from web page content.
 * Adapted to work with String document IDs (URLs) and input from a Map<String, String>.
 *
 * @author ehab (adapted by AI)
 */
public class Index5 {

    //--------------------------------------------
    int N = 0; // Total number of documents in the collection
    // Stores document metadata: Key is URL (String), Value is SourceRecord
    public Map<String, SourceRecord> sources;
    // The inverted index: Key is Term (String), Value is DictEntry
    public HashMap<String, DictEntry> index;
    //--------------------------------------------

    /**
     * Constructor to initialize the data structures.
     */
    public Index5() {
        sources = new HashMap<>();
        index = new HashMap<>();
    }

    /**
     * Sets the total number of documents (usually done after crawling).
     * @param n The total number of documents.
     */
    public void setN(int n) {
        N = n;
    }

    //---------------------------------------------

    /**
     * Prints the posting list for a given term.
     * @param p The head of the posting list (linked list) to print.
     */
    public void printPostingList(Posting p) {
        System.out.print("[");
        while (p != null) {
            // Print docId (String URL) and dtf (term frequency in that doc)
            System.out.print("(" + p.docId + "," + p.dtf + ")");
            if (p.next != null) {
                System.out.print(",");
            }
            p = p.next;
        }
        System.out.println("]");
    }

    //---------------------------------------------

    /**
     * Prints the dictionary (inverted index) to the console.
     */
    public void printDictionary() {
        System.out.println("****************************************");
        System.out.println("Dictionary (Inverted Index):");
        System.out.println("****************************************");
        Iterator<Map.Entry<String, DictEntry>> it = index.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DictEntry> pair = it.next();
            DictEntry dd = pair.getValue();
            System.out.print("** Term: '" + pair.getKey() + "' | Docs: " + dd.doc_freq + " | Total Freq: " + dd.term_freq + " | Postings: ");
            printPostingList(dd.pList);
        }
        System.out.println("------------------------------------------------------");
        System.out.println("*** Number of terms = " + index.size());
        System.out.println("*** Number of documents = " + N);
        System.out.println("****************************************");
    }

    //-----------------------------------------------
    //      BUILD INDEX METHODS
    //-----------------------------------------------

    /**
     * Builds the inverted index from a map of crawled pages.
     * Replaces the old file-based buildIndex.
     *
     * @param crawledPages A Map where Key is the URL (String) and Value is the page's text content (String).
     */
    public void buildIndex(Map<String, String> crawledPages) {
        System.out.println("Building index from crawled pages...");
        sources = new HashMap<>(); // Clear previous sources
        index = new HashMap<>();   // Clear previous index
        this.N = crawledPages.size(); // Set total number of documents

        int count = 0;
        for (Map.Entry<String, String> entry : crawledPages.entrySet()) {
            String url = entry.getKey();
            String pageText = entry.getValue();
            count++;

            System.out.println("Indexing document " + count + "/" + N + ": " + url);

            // Create a basic source record (title could be improved if needed)
            // Extract a simple title from the last part of the URL path
            String title = url.substring(url.lastIndexOf('/') + 1).replace('_', ' ');
            SourceRecord sourceRecord = new SourceRecord(url, title);
            sources.put(url, sourceRecord);

            // Process the entire text content of the page
            indexPage(pageText, url);
        }
        System.out.println("Index built successfully from " + N + " documents. Index contains " + index.size() + " unique terms.");
    }

    /**
     * Processes the text content of a single page and updates the inverted index.
     *
     * @param text The full text content of the page.
     * @param docId The unique identifier (URL) of the page.
     */
    public void indexPage(String text, String docId) {
        // Normalize and tokenize the entire text content
        // Replace non-alphanumeric characters (except hyphens within words) with spaces, convert to lower case, split by spaces.
        String[] words = text.replaceAll("(?:[^a-zA-Z0-9 -]|(?<=\\w)-(?!\\S))", " ").toLowerCase().split("\\s+");
        int pageTokenCount = 0; // Count of indexed tokens for this page

        // --- Step 1: Calculate term frequencies for *this page only* ---
        Map<String, Integer> termFreqMap = new HashMap<>();
        for (String word : words) {
            if (word.isEmpty()) continue; // Skip empty strings resulting from split

            if (stopWord(word)) { // Check if it's a stop word
                continue;
            }
            word = stemWord(word); // Apply stemming

            if (word.isEmpty()) continue; // Skip if stemming results in empty word

            pageTokenCount++; // Increment count of actual indexed tokens
            // Update frequency count for this term within this page
            termFreqMap.put(word, termFreqMap.getOrDefault(word, 0) + 1);
        }

        // --- Step 2: Update the document length in the sources map ---
        if (sources.containsKey(docId)) {
            sources.get(docId).setLength(pageTokenCount); // Use the setter
        }

        // --- Step 3: Update the main inverted index using the page's term frequencies ---
        for (Map.Entry<String, Integer> termEntry : termFreqMap.entrySet()) {
            String term = termEntry.getKey();
            int tf = termEntry.getValue(); // Term frequency *in this specific doc*

            // Get or create the dictionary entry for the term
            DictEntry dictEntry = index.computeIfAbsent(term, k -> new DictEntry());

            // Add the posting using the method in DictEntry
            // This method handles adding the Posting node AND updating doc_freq/term_freq
            dictEntry.addPosting(docId, tf);
        }
    }

    //----------------------------------------------------------------------------
    //      HELPER METHODS (StopWord, Stemming)
    //----------------------------------------------------------------------------

    /**
     * Checks if a word is a stop word.
     * (Basic implementation - consider using a more comprehensive list).
     * @param word The word to check (lowercase).
     * @return true if it's a stop word, false otherwise.
     */
    boolean stopWord(String word) {
        // Simple list, can be expanded
        if (word.equals("the") || word.equals("to") || word.equals("be") || word.equals("for") || word.equals("from") || word.equals("in")
                || word.equals("a") || word.equals("into") || word.equals("by") || word.equals("or") || word.equals("and") || word.equals("that")
                || word.equals("it") || word.equals("is") || word.equals("as") || word.equals("at") || word.equals("of")) {
            return true;
        }
        // Also consider very short words as stop words
        if (word.length() < 2) {
            return true;
        }
        return false;
    }

    /**
     * Stems a word using the Porter Stemmer.
     * @param word The word to stem.
     * @return The stemmed word.
     */
    String stemWord(String word) {
        // Uncomment the following lines to enable stemming
        // if (word == null || word.isEmpty()) return word;
        // Stemmer s = new Stemmer();
        // s.addString(word);
        // s.stem();
        // return s.toString();

        // Return original word if stemming is disabled
        return word;
    }

    //----------------------------------------------------------------------------
    //      SEARCH METHODS (Adapted for String docId)
    //----------------------------------------------------------------------------

    /**
     * Finds the intersection of two posting lists (linked lists of Postings).
     * Assumes posting lists are NOT necessarily sorted by docId.
     * This version is less efficient than if lists were sorted.
     *
     * @param pL1 Head of the first posting list.
     * @param pL2 Head of the second posting list.
     * @return A new Posting list containing only docIds present in both input lists.
     *         The dtf in the result is taken from pL1 (arbitrary choice).
     */
    Posting intersect(Posting pL1, Posting pL2) {
        if (pL1 == null || pL2 == null) {
            return null; // Intersection is empty if either list is empty
        }

        Posting answer = null;
        Posting last = null;

        // Store docIds from pL2 in a temporary set for quick lookup
        HashMap<String, Integer> pL2DocIds = new HashMap<>();
        Posting temp = pL2;
        while(temp != null){
            pL2DocIds.put(temp.docId, temp.dtf);
            temp = temp.next;
        }

        // Iterate through pL1 and check for matches in pL2's set
        Posting currentP1 = pL1;
        while (currentP1 != null) {
            if (pL2DocIds.containsKey(currentP1.docId)) {
                // Found a match, add to the answer list
                // Create a new Posting node for the result
                Posting intersectionPosting = new Posting(currentP1.docId, currentP1.dtf); // Use dtf from pL1

                if (answer == null) {
                    answer = intersectionPosting;
                    last = answer;
                } else {
                    last.next = intersectionPosting;
                    last = intersectionPosting;
                }
            }
            currentP1 = currentP1.next;
        }

        return answer;
    }


    /**
     * Finds the intersection of two posting lists (linked lists of Postings).
     * Optimized version assumes posting lists ARE sorted alphabetically by docId (URL).
     *
     * @param pL1 Head of the first sorted posting list.
     * @param pL2 Head of the second sorted posting list.
     * @return A new Posting list containing only docIds present in both input lists.
     */
    Posting intersect_sorted(Posting pL1, Posting pL2) {
        Posting answer = null;
        Posting last = null;

        while (pL1 != null && pL2 != null) {
            int comparison = pL1.docId.compareTo(pL2.docId); // Compare URLs alphabetically

            if (comparison == 0) { // URLs are the same
                // Found a match, add to the answer list
                Posting intersectionPosting = new Posting(pL1.docId, pL1.dtf); // Use dtf from pL1

                if (answer == null) {
                    answer = intersectionPosting;
                    last = answer;
                } else {
                    last.next = intersectionPosting;
                    last = intersectionPosting;
                }
                // Move both pointers forward
                pL1 = pL1.next;
                pL2 = pL2.next;
            } else if (comparison < 0) { // pL1's URL comes before pL2's URL
                pL1 = pL1.next; // Move pL1 forward
            } else { // pL2's URL comes before pL1's URL
                pL2 = pL2.next; // Move pL2 forward
            }
        }
        return answer;
    }


    /**
     * Performs a simple boolean AND search for a phrase.
     * Finds documents containing ALL terms in the phrase.
     * Uses the intersect method.
     *
     * @param phrase The search phrase.
     * @return A String listing the URLs of matching documents, or a "not found" message.
     */
    public String findQueryBooleanAnd(String phrase) {
        System.out.println("Performing Boolean AND search for: '" + phrase + "'");
        String result = "";
        String[] words = phrase.toLowerCase().split("\\s+"); // Split by whitespace, convert to lowercase

        if (words.length == 0 || words[0].isEmpty()) {
            return "Empty query.";
        }

        // Apply stopword removal and stemming to query terms
        ArrayList<String> processedWords = new ArrayList<>();
        for (String word : words) {
            if (stopWord(word)) continue;
            String stemmed = stemWord(word);
            if (!stemmed.isEmpty()) {
                processedWords.add(stemmed);
            }
        }

        if (processedWords.isEmpty()) {
            return "Query contains only stop words or empty terms.";
        }

        String firstWord = processedWords.get(0);

        // Check if the first word exists in the index
        if (!index.containsKey(firstWord)) {
            System.out.println("Term '" + firstWord + "' not found in index.");
            return "No results found (term '" + firstWord + "' not in index).";
        }

        // Start with the posting list of the first word
        // NOTE: For efficiency, it's better to sort words by increasing doc_freq
        //       and start intersection with the shortest lists first. (Not implemented here)
        Posting postingResult = index.get(firstWord).pList;

        // Intersect with the posting lists of the remaining words
        for (int i = 1; i < processedWords.size(); i++) {
            String nextWord = processedWords.get(i);
            if (!index.containsKey(nextWord)) {
                System.out.println("Term '" + nextWord + "' not found in index.");
                return "No results found (term '" + nextWord + "' not in index)."; // If any word isn't found, the AND fails
            }
            if (postingResult == null) break; // If intersection becomes empty, no need to continue

            // Choose the appropriate intersect method based on whether lists are sorted
            // Assuming lists are NOT sorted by default after indexing:
            postingResult = intersect(postingResult, index.get(nextWord).pList);
            // If you modify addPosting to maintain sorted lists, use:
            // postingResult = intersect_sorted(postingResult, index.get(nextWord).pList);

        }

        // Build the result string
        int count = 0;
        StringBuilder resultBuilder = new StringBuilder();
        while (postingResult != null) {
            SourceRecord sr = sources.get(postingResult.docId); // Get source record using String URL
            if (sr != null) {
                resultBuilder.append("\t [").append(++count).append("] ").append(sr.title).append(" (").append(sr.URL).append(")").append("\n");
            } else {
                // Should not happen if sources map is consistent
                resultBuilder.append("\t [").append(++count).append("] ").append("Unknown Source (").append(postingResult.docId).append(")").append("\n");
            }
            postingResult = postingResult.next;
        }

        if (count == 0) {
            return "No documents contain all the specified terms.";
        }

        return "Found " + count + " documents:\n" + resultBuilder.toString();
    }


    //----------------------------------------------------------------------------
    //      PERSISTENCE METHODS (Commented Out - Require Adaptation)
    //----------------------------------------------------------------------------
    /*
    public void store(String storageName) {
        System.out.println("Storing index is currently disabled/requires adaptation for String IDs.");
        // TODO: Adapt this method to handle String keys in sources map
        //       and String docIds in Posting lists if persistence is needed.
        //       Need to decide on a robust format for storing String URLs/IDs,
        //       potentially handling commas or special characters within them.
    }

    public boolean storageFileExists(String storageName) {
        System.out.println("Storage check is currently disabled.");
        return false;
        // TODO: Adapt path and logic if persistence is implemented.
    }

    public void createStore(String storageName) {
         System.out.println("Storage creation is currently disabled.");
        // TODO: Adapt path and logic if persistence is implemented.
    }

    public HashMap<String, DictEntry> load(String storageName) {
         System.out.println("Loading index is currently disabled/requires adaptation for String IDs.");
        // TODO: Adapt this method to handle String keys/IDs and the chosen storage format.
        return null; // Return null or throw exception as it's disabled
    }
    */

    //----------------------------------------------------------------------------
    //      Old file-based buildIndex (Keep commented out or remove)
    //----------------------------------------------------------------------------
    /*
    public void buildIndex(String[] files) { // from disk not from the internet
        System.out.println("WARNING: Using deprecated file-based buildIndex. Use buildIndex(Map<String, String> crawledPages) instead.");
        // ... (old logic) ...
    }
    */
    /*
    public int indexOneLine(String ln, int fid) {
         System.out.println("WARNING: Using deprecated indexOneLine. Use indexPage(String text, String docId) instead.");
        // ... (old logic) ...
        return 0;
    }
    */
}