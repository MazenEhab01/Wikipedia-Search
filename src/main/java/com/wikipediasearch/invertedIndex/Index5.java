package com.wikipediasearch.invertedIndex;

import java.util.*; // Import necessary collections
import java.util.stream.Collectors; // For sorting results

// Add necessary imports if not already present
// If SourceRecord is in another package, import it:
// import com.wikipediasearch.SourceRecord; // Example if needed
// If Stemmer is in another package, import it:
// import com.wikipediasearch.Stemmer; // Example if needed


public class Index5 {

    // --- Fields ---
    private Map<String, DictEntry> index;          // Term -> DictEntry (df, postings list)
    private Map<Integer, SourceRecord> sources;    // docId (int) -> SourceRecord (URL, title, etc.)
    private Stemmer stemmer;                       // Assuming a stemmer is used
    private Set<String> stopWords;                 // Assuming stop words are handled
    private Map<Integer, Double> docMagnitudes;    // Store pre-calculated document norms (magnitudes)

    // --- Constructor ---
    public Index5() {
        index = new HashMap<>();
        sources = new HashMap<>();
        docMagnitudes = new HashMap<>();
        stemmer = new Stemmer();       // Initialize stemmer (or however it's done in your project)
        stopWords = new HashSet<>();   // Initialize stopwords (load them if needed)
        // loadStopWords("stopwords.txt"); // Example: you would need a method to load these
    }

    // --- buildIndex Method (Corrected) ---
    public void buildIndex(Map<String, String> pages) {
        System.out.println("Building index...");
        int docIdCounter = 0;

        // Clear previous index data before building anew
        index.clear();
        sources.clear();
        docMagnitudes.clear();

        if (pages == null || pages.isEmpty()) {
            System.out.println("No pages provided to build index.");
            return;
        }

        // --- Iterate through each page provided by the crawler ---
        for (Map.Entry<String, String> pageEntry : pages.entrySet()) {
            String url = pageEntry.getKey();
            String content = pageEntry.getValue();
            int currentDocId = docIdCounter++; // Assign the next available integer ID

            // Store document metadata (URL, maybe title if available)
            SourceRecord currentSource = new SourceRecord(currentDocId, url, "Title Placeholder - " + url); // Using URL as placeholder title
            sources.put(currentDocId, currentSource);

            // --- Text Processing for the current document ---
            // 1. Tokenize (split into words) - example: split by non-word characters
            String[] terms = content.toLowerCase().split("\\W+");
            int tokenCount = 0; // Keep track of valid tokens for document length

            // 2. Calculate Term Frequencies (TF) within this document
            Map<String, Integer> termFrequenciesInDoc = new HashMap<>();
            for (String term : terms) {
                if (term.isEmpty()) continue; // Skip empty strings resulting from split

                // ***** FILTER OUT NUMERIC-ONLY TERMS *****
                if (term.matches("\\d+")) {
                    continue; // Skip this term if it consists only of digits
                }
                // Optional: Use this stricter filter if you want to remove any token without letters
                /*
                if (!term.matches(".*[a-zA-Z].*")) {
                    continue; // Skip term if it doesn't contain any letters
                }
                */

                tokenCount++; // Count this as a valid token for document length

                // 3. Apply Stop Word Removal (Optional but recommended)
                // if (stopWords.contains(term)) continue; // Don't forget to decrement tokenCount if stopping here

                // 4. Apply Stemming (Crucial for matching variations like 'run', 'running')
                // String stemmedTerm = stemmer.stem(term); // Assuming stemmer has a 'stem' method
                String stemmedTerm = term; // Replace with actual stemming call if available

                // Count frequency of the stemmed term in this document
                termFrequenciesInDoc.put(stemmedTerm, termFrequenciesInDoc.getOrDefault(stemmedTerm, 0) + 1);
            }
            // Set the document length in SourceRecord (optional)
            currentSource.setLength(tokenCount);
            // --- End of Text Processing ---


            // --- Update Inverted Index using calculated TFs ---
            for (Map.Entry<String, Integer> tfEntry : termFrequenciesInDoc.entrySet()) {
                String processedTerm = tfEntry.getKey();
                int termFreqInThisDoc = tfEntry.getValue(); // dtf for this term in currentDocId

                // Get or create the dictionary entry for the term
                DictEntry dictEntry = index.get(processedTerm);
                boolean isNewTermOccurrenceInIndex = (dictEntry == null);

                if (isNewTermOccurrenceInIndex) {
                    dictEntry = new DictEntry();
                    index.put(processedTerm, dictEntry);
                }

                // Since we process each document completely once, finding a term
                // always means it's appearing in *this* document for the first time
                // during this build process. Therefore, we always increment doc_freq.
                dictEntry.incrementDocFreq(); // Increment document frequency for the term

                // Add the term's frequency in *this* document to the total term frequency count
                dictEntry.addToTermFreq(termFreqInThisDoc);

                // Create the Posting object for this term and document
                Posting newPosting = new Posting(currentDocId, termFreqInThisDoc);

                // Add the Posting to the DictEntry's posting list
                dictEntry.addPosting(newPosting);
            }
            // Progress indicator (optional)
            if (currentDocId % 10 == 0 && currentDocId > 0) {
                System.out.println("  Indexed " + (currentDocId + 1) + " documents...");
            }

        } // --- End of loop processing individual pages ---

        System.out.println("Initial index build complete. Total documents: " + sources.size());

        // --- Post-processing: Calculate Document Magnitudes ---
        System.out.println("Calculating document magnitudes for TF-IDF...");
        calculateAllDocumentMagnitudes();
        System.out.println("Document magnitudes calculated.");
    }


    // --- Method to calculate magnitudes for all documents (TF-IDF Norm) ---
    private void calculateAllDocumentMagnitudes() {
        int N = sources.size(); // Total number of documents indexed
        if (N == 0) {
            System.out.println("No documents indexed, cannot calculate magnitudes.");
            return;
        }

        // Temporary map to store sum of squared TF-IDF weights for each doc
        Map<Integer, Double> docScoresSumOfSquares = new HashMap<>();

        // Iterate through each term in the dictionary (inverted index)
        for (Map.Entry<String, DictEntry> indexEntry : index.entrySet()) {
            // String term = indexEntry.getKey(); // Term itself isn't needed here
            DictEntry dictEntry = indexEntry.getValue();

            int df = dictEntry.getDoc_freq(); // Document frequency for the term
            if (df == 0) continue; // Skip if term somehow has 0 df (shouldn't happen)

            // Calculate IDF for the current term
            double idf = calculateIdf(N, df);
            if (idf == 0) continue; // Skip terms present in all docs (IDF=0, contribute nothing to magnitude)

            // Iterate through the posting list (documents containing this term)
            for (Posting post : dictEntry.getPlist()) { // Use the getter
                int docId = post.getDocId();   // Use getter
                int tf = post.getDtf();    // Use getter

                // Calculate TF weight for the term in this document
                double tfWeight = calculateTfWeight(tf);

                // Calculate TF-IDF score for this term in this document
                double tfIdf = tfWeight * idf;

                // Accumulate the *square* of the TF-IDF weight for the document's magnitude calculation
                docScoresSumOfSquares.put(docId,
                        docScoresSumOfSquares.getOrDefault(docId, 0.0) + (tfIdf * tfIdf));
            }
        }

        // Calculate the final magnitude (Euclidean norm = sqrt of sum of squares) for each document
        docMagnitudes.clear(); // Ensure the map is clear before filling
        for (Map.Entry<Integer, Double> entry : docScoresSumOfSquares.entrySet()) {
            int docId = entry.getKey();
            double sumOfSquares = entry.getValue();
            docMagnitudes.put(docId, Math.sqrt(sumOfSquares));
        }
        System.out.println("Calculated magnitudes for " + docMagnitudes.size() + " documents.");
    }

    // --- Helper method for TF weight (1 + log10(tf)) ---
    private double calculateTfWeight(int tf) {
        if (tf <= 0) {
            return 0.0; // Term doesn't appear or frequency is non-positive
        }
        return 1 + Math.log10(tf);
    }

    // --- Helper method for IDF (log10(N / df)) ---
    private double calculateIdf(int N, int df) {
        if (N == 0 || df <= 0 || df > N) {
            // Handle edge cases to avoid log10(0) or invalid scenarios
            // If df=N, idf is log10(1) = 0. This is correct.
            // If df=0 or df > N (shouldn't happen with correct indexing), return 0.
            return 0.0;
        }
        return Math.log10((double) N / df);
    }

    // --- Method to process query terms (tokenize, stem, etc.) ---
    //     MUST mirror the processing done during indexing!
    private List<String> processQuery(String query) {
        List<String> processedTerms = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return processedTerms; // Return empty list for empty query
        }

        String[] terms = query.toLowerCase().split("\\W+"); // Same tokenizer as indexer
        for (String term : terms) {
            if (term.isEmpty()) {
                continue;
            }

            // ***** APPLY SAME FILTER TO QUERY TERMS *****
            if (term.matches("\\d+")) {
                continue; // Skip purely numeric tokens in query
            }
            // Optional stricter filter:
            /*
            if (!term.matches(".*[a-zA-Z].*")) {
                 continue; // Skip query term if it doesn't contain any letters
            }
            */

            // Apply stop word removal if used during indexing
            // if (stopWords.contains(term)) continue;

            // Apply stemming if used during indexing
            // String stemmedTerm = stemmer.stem(term);
            String stemmedTerm = term; // Replace with actual stemming call

            processedTerms.add(stemmedTerm);
        }
        return processedTerms;
    }


    // --- Ranked Search Method using TF-IDF and Cosine Similarity ---
    public List<SearchResult> findQueryRanked(String query) {
        int N = sources.size();
        if (N == 0) {
            System.err.println("Error: Index is empty. Cannot perform search.");
            return Collections.emptyList();
        }

        // 1. Process the query (tokenize, filter, stem, etc.)
        List<String> queryTerms = processQuery(query); // Uses the updated processQuery
        if (queryTerms.isEmpty()) {
            System.out.println("Query processed to empty term list (e.g., only stopwords or numbers?). No results.");
            return Collections.emptyList();
        }

        // 2. Calculate Query Vector (TF-IDF weights and magnitude)
        Map<String, Integer> queryTermFrequency = new HashMap<>();
        for (String term : queryTerms) {
            queryTermFrequency.put(term, queryTermFrequency.getOrDefault(term, 0) + 1);
        }

        Map<String, Double> queryTfIdfVector = new HashMap<>(); // Stores TF-IDF for each query term
        double queryMagnitudeSquared = 0.0;

        for (Map.Entry<String, Integer> entry : queryTermFrequency.entrySet()) {
            String term = entry.getKey();
            int qTf = entry.getValue(); // Term frequency in query

            DictEntry dictEntry = index.get(term); // Check if term exists in the index

            double termIdf = 0.0;
            if (dictEntry != null && dictEntry.getDoc_freq() > 0) {
                // Term must exist in index (and thus not be purely numeric due to buildIndex filter)
                termIdf = calculateIdf(N, dictEntry.getDoc_freq());
            }
            // IDF is 0 if term not in index or df=0

            double qTfWeight = calculateTfWeight(qTf);
            double tfIdfValue = qTfWeight * termIdf; // TF-IDF for this term in the query

            if (tfIdfValue > 0) { // Only store terms that contribute to score/magnitude
                queryTfIdfVector.put(term, tfIdfValue);
                queryMagnitudeSquared += (tfIdfValue * tfIdfValue); // Accumulate for query magnitude
            }
        }

        double queryMagnitude = Math.sqrt(queryMagnitudeSquared);

        // If query magnitude is 0 (e.g., query only contains terms not in index, or terms present in all docs), no results.
        if (queryMagnitude == 0) {
            System.out.println("Query terms not found in index or have zero relevance (IDF=0). Cannot rank.");
            return Collections.emptyList(); // Avoid division by zero later
        }

        // 3. Calculate Dot Products using Score Accumulators
        //    Map: docId -> accumulated dot product score
        Map<Integer, Double> docScores = new HashMap<>();

        // Iterate through the *terms present in the query's TF-IDF vector*
        for (Map.Entry<String, Double> queryVectorEntry : queryTfIdfVector.entrySet()) {
            String term = queryVectorEntry.getKey();
            double queryTermTfIdf = queryVectorEntry.getValue(); // Pre-calculated query TF-IDF

            DictEntry dictEntry = index.get(term); // We know this exists from query vector calculation

            // Re-calculate or retrieve IDF (already calculated above, could optimize but fine for clarity)
            double termIdf = calculateIdf(N, dictEntry.getDoc_freq());

            // Iterate through the posting list for this term
            for (Posting post : dictEntry.getPlist()) { // Use getter
                int docId = post.getDocId();   // Use getter
                int docTf = post.getDtf();    // Use getter

                // Calculate TF-IDF for this term in *this specific document*
                double docTfWeight = calculateTfWeight(docTf);
                double docTermTfIdf = docTfWeight * termIdf;

                // Accumulate the dot product component: (query_term_tfidf * doc_term_tfidf)
                docScores.put(docId,
                        docScores.getOrDefault(docId, 0.0) + (queryTermTfIdf * docTermTfIdf));
            }
        }

        // 4. Calculate Final Cosine Similarity Scores and Rank
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Integer, Double> scoreEntry : docScores.entrySet()) {
            int docId = scoreEntry.getKey();
            double dotProduct = scoreEntry.getValue();

            // Retrieve the pre-calculated magnitude for the document
            double docMagnitude = docMagnitudes.getOrDefault(docId, 0.0);

            // Avoid division by zero if a document somehow has zero magnitude
            // (e.g., if it only contained terms with IDF=0)
            if (docMagnitude == 0) {
                continue; // Skip this document, cosine similarity is undefined/zero
            }

            // Calculate cosine similarity
            double cosineSimilarity = dotProduct / (queryMagnitude * docMagnitude);

            // Ensure score is not NaN or infinite due to potential floating point issues (unlikely here)
            if (Double.isNaN(cosineSimilarity) || Double.isInfinite(cosineSimilarity)) {
                cosineSimilarity = 0.0;
            }


            // Get document identifier (URL/Title) from sources map
            SourceRecord docInfo = sources.get(docId);
            String docIdentifier = (docInfo != null) ? docInfo.getL() : "Unknown Doc ID: " + docId; // Use getter from SourceRecord

            results.add(new SearchResult(docId, cosineSimilarity, docIdentifier));
        }

        // 5. Sort results by score (descending)
        //    Using lambda expression for Comparator
        results.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore())); // r2 vs r1 for descending

        return results;
    }


    // --- Boolean AND Search Method (Kept for reference/testing) ---
    // This method also needs to use the updated DictEntry/Posting getters
    public String findQueryBooleanAnd(String phrase) {
        List<String> queryTerms = processQuery(phrase); // Process query consistently
        if (queryTerms.isEmpty()){
            return "Boolean AND Results for '" + phrase + "':\n  <No valid terms in query>\n";
        }

        Map<Integer, Integer> intersectionMap = new HashMap<>(); // docId -> count of query terms found
        int requiredTermCount = 0;
        List<Integer> firstTermDocIds = null; // Optimization: docs must contain first term

        boolean isFirstTerm = true;
        for (String term : queryTerms) { // queryTerms are already filtered
            DictEntry dictEntry = index.get(term);
            // Term might not be in index IF it was filtered during build but not query (unlikely with consistent processing)
            // OR if it genuinely wasn't in any document.
            if (dictEntry != null && dictEntry.getDoc_freq() > 0) {
                requiredTermCount++; // Count only terms actually present in the index
                List<Posting> postings = dictEntry.getPlist(); // Use getter

                if (isFirstTerm) {
                    // Optimization: If first term yields no postings, impossible to satisfy AND
                    if (postings.isEmpty()) return "Boolean AND Results for '" + phrase + "':\n  <First valid term '" + term + "' not found in any docs>\n";

                    firstTermDocIds = new ArrayList<>(postings.size());
                    for(Posting p : postings) {
                        firstTermDocIds.add(p.getDocId()); // Use getter
                        intersectionMap.put(p.getDocId(), 1);
                    }
                    isFirstTerm = false;

                } else {
                    // For subsequent terms, only check docs that contained previous terms
                    Set<Integer> currentTermDocIds = new HashSet<>();
                    for (Posting p : postings) {
                        currentTermDocIds.add(p.getDocId());
                    }
                    // Update intersectionMap only for docs present in the first list *and* current list
                    // Iterate over a copy of the keyset to allow removal while iterating
                    for (int docId : new ArrayList<>(intersectionMap.keySet())) {
                        if(currentTermDocIds.contains(docId)) {
                            // Increment count for docs that also contain this term
                            intersectionMap.put(docId, intersectionMap.get(docId) + 1);
                        } else {
                            // Remove doc from candidates if it doesn't contain the current term
                            intersectionMap.remove(docId);
                        }
                    }
                    // Optimization: If intersection becomes empty, impossible to satisfy AND
                    if (intersectionMap.isEmpty()) return "Boolean AND Results for '" + phrase + "':\n  <No documents contain all terms up to '" + term + "'>\n";
                }

            } else {
                // If any term *required* by the query (after processing) is not in the index, AND fails
                // This case is less likely if filtering is consistent, but covers terms genuinely not found.
                return "Boolean AND Results for '" + phrase + "':\n  <Term '" + term + "' not found in index. No results possible.>\n";
            }
        }


        StringBuilder resultBuilder = new StringBuilder("Boolean AND Results for '");
        resultBuilder.append(phrase).append("' (Required terms found in index: ").append(requiredTermCount).append("):\n");
        boolean found = false;

        // Check if we found any terms and if the intersection is non-empty
        if (requiredTermCount > 0 && !intersectionMap.isEmpty()) {
            List<Integer> sortedDocIds = new ArrayList<>(); // Store matching doc IDs

            // Find documents where the count matches the number of required terms found in the index
            for (Map.Entry<Integer, Integer> entry : intersectionMap.entrySet()) {
                if (entry.getValue() == requiredTermCount) {
                    sortedDocIds.add(entry.getKey());
                }
            }

            if (!sortedDocIds.isEmpty()) {
                Collections.sort(sortedDocIds); // Optional: sort results by docId
                found = true;
                for (int docId : sortedDocIds) {
                    SourceRecord docInfo = sources.get(docId);
                    // Ensure SourceRecord has a getter for the identifier (e.g., getL() or getUrl())
                    String docIdentifier = (docInfo != null) ? docInfo.getL() : "Doc ID: " + docId;
                    resultBuilder.append("  - ").append(docIdentifier).append("\n");
                }
            }
        }

        if (!found) {
            resultBuilder.append("  <No documents found containing all required terms>\n");
        }

        return resultBuilder.toString();
    }
    // --- Method to retrieve the TF-IDF vector for a specific document ---
    public Map<String, Double> getDocumentTfIdfVector(int docId) {
        Map<String, Double> tfIdfVector = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order (optional)
        int N = sources.size();

        // Basic validation
        if (N == 0 || !sources.containsKey(docId)) {
            System.err.println("Warning: Cannot get vector for invalid docId: " + docId + " or index is empty.");
            return Collections.emptyMap(); // Return empty map for invalid ID or empty index
        }

        // Iterate through all terms in the main index
        for (Map.Entry<String, DictEntry> indexEntry : index.entrySet()) {
            String term = indexEntry.getKey(); // <<< term IS THE ACTUAL WORD (String)
            DictEntry dictEntry = indexEntry.getValue();

            // Find the posting for the requested docId within this term's list
            Posting targetPosting = null;
            for (Posting p : dictEntry.getPlist()) { // Use getter
                if (p.getDocId() == docId) { // Use getter
                    targetPosting = p;
                    break; // Found the posting for this docId
                }
            }


            // If the term exists in the specified document (posting found)
            if (targetPosting != null) {
                int tf = targetPosting.getDtf(); // Use getter
                int df = dictEntry.getDoc_freq(); // Use getter

                // Calculate TF-IDF weight for this term IN THIS DOCUMENT
                if (tf > 0 && df > 0) { // Ensure valid TF and DF
                    double tfWeight = calculateTfWeight(tf);
                    double idf = calculateIdf(N, df);
                    double tfIdf = tfWeight * idf;

                    // Only add terms with a positive TF-IDF weight to the vector representation
                    // Note: tfIdf can be 0 if idf is 0 (term in all docs)
                    if (tfIdf > 0) {
                        tfIdfVector.put(term, tfIdf);
                    }
                }
            }
            // If targetPosting is null, this term is not in the specified docId,
            // so its component in the vector is implicitly zero.
        }

        // Optional: Sort the vector components by term for consistent output
        // Map<String, Double> sortedVector = new TreeMap<>(tfIdfVector);
        // return sortedVector;

        return tfIdfVector; // Return the map representing non-zero vector components
    }

    // --- printDictionary Method (Minor update to use getter) ---
    public void printDictionary() {
        System.out.println("--- Printing Dictionary Sample ("+ index.size() + " total terms) ---");
        int count = 0;
        // Sort terms alphabetically for consistent output (optional)
        List<String> sortedTerms = new ArrayList<>(index.keySet());
        Collections.sort(sortedTerms);

        for (String term : sortedTerms) {
            DictEntry de = index.get(term);
            // Use getter for doc_freq
            System.out.println("Term: '" + term + "', DF: " + de.getDoc_freq() + ", Total TF: " + de.getTerm_freq() + ", Postings Count: " + de.getPlist().size());
            // Avoid printing the full posting list unless debugging specific term
            // System.out.println("  Postings: " + de.getPlist());
            if (++count >= 50) { // Limit printout to avoid flooding console
                System.out.println("... (limiting printout to first 50 terms alphabetically)");
                break;
            }
        }
        System.out.println("--- End of Dictionary Sample ---");
    }

    // --- Helper find method for boolean search (OBSOLETE if boolean search updated fully) ---
    // Kept only if needed by external code, boolean search now uses index directly
    /*
    private List<Posting> find(String term) {
        // Process term the same way as indexing/querying
        String processedTerm = term.toLowerCase(); // Add stemming if used
        // processedTerm = stemmer.stem(processedTerm);
        DictEntry entry = index.get(processedTerm);
        return (entry != null) ? entry.getPlist() : null; // Use getter
    }
    */


    // --- Inner Helper Class for Search Results ---
    public static class SearchResult implements Comparable<SearchResult> { // Implement Comparable
        private int docId;
        private double score;
        private String identifier; // URL or Title

        public SearchResult(int docId, double score, String identifier) {
            this.docId = docId;
            this.score = score;
            this.identifier = identifier;
        }

        public int getDocId() { return docId; }
        public double getScore() { return score; }
        public String getIdentifier() { return identifier; }

        @Override
        public String toString() {
            // Format score for better readability
            return String.format("Score: %.6f - %s", score, identifier); // Increased precision
        }

        // Allow sorting directly on SearchResult objects
        @Override
        public int compareTo(SearchResult other) {
            // Descending order of score
            return Double.compare(other.score, this.score);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchResult that = (SearchResult) o;
            return docId == that.docId && Double.compare(that.score, score) == 0; // Compare score and docId
        }

        @Override
        public int hashCode() {
            return Objects.hash(docId, score);
        }
    }

    // --- Placeholder for SourceRecord class ---
    // Make sure you have this class defined correctly, possibly in its own file
    // Example:

    // public static class SourceRecord {
    //     private int docId;
    //     private String url;
    //     private String title;
    //     private int length;
    //
    //     public SourceRecord(int id, String u, String t) {
    //         this.docId = id;
    //         this.url = u;
    //         this.title = (t != null) ? t : "";
    //         this.length = 0;
    //     }
    //     public String getL() { return url; } // Getter for the primary identifier (URL)
    //     public int getDocId() { return docId;}
    //     public String getUrl() { return url;}
    //     public String getTitle() { return title;}
    //     public int getLength() { return length;}
    //     public void setLength(int len) { this.length = len;}
    // }


    // --- Placeholder for Stemmer class ---
    // Make sure you have this class defined

    public static class Stemmer {
        // Implement Porter Stemmer or use a library
        public String stem(String term) {
            // Placeholder: return term unchanged FOR NOW
            return term;
        }
    }


} // End of Index5 class