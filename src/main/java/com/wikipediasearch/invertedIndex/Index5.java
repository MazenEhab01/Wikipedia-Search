package com.wikipediasearch.invertedIndex;

// Import necessary classes including the new similarity calculators
import com.wikipediasearch.similarity.TFIDFCalculator;
import com.wikipediasearch.similarity.CosineSimilarityCalculator;

import java.util.*;
import java.util.stream.Collectors;

public class Index5 {

    // --- Fields ---
    private Map<String, DictEntry> index;          // Term -> DictEntry (df, postings list)
    private Map<Integer, SourceRecord> sources;    // docId (int) -> SourceRecord (URL, title, etc.)
     private Stemmer stemmer; // Stemmer is present but not used per assignment spec
     private Set<String> stopWords; // Stop words not implemented here
    private Map<Integer, Double> docMagnitudes;    // Store pre-calculated document norms (magnitudes)

    // --- Constructor ---
    public Index5() {
        index = new HashMap<>();
        sources = new HashMap<>();
        docMagnitudes = new HashMap<>();
         stemmer = new Stemmer(); // Initialize if used
         stopWords = new HashSet<>(); // Initialize if used
//         loadStopWords("stopwords.txt"); // Example if needed
    }
    /*
        doc1 -> term1, term2, term3
     */

    // --- buildIndex Method (Updated - No functional change, still doesn't use stemmer) ---
    public void buildIndex(Map<String, String> pages) {
        System.out.println("Building index...");
        int docIdCounter = 0;

        index.clear();
        sources.clear();
        docMagnitudes.clear();

        if (pages == null || pages.isEmpty()) {
            System.out.println("No pages provided to build index.");
            return;
        }

        for (Map.Entry<String, String> pageEntry : pages.entrySet()) {
            String url = pageEntry.getKey();
            String content = pageEntry.getValue();
            if (content == null || content.trim().isEmpty()) {
                System.err.println("Warning: Skipping page with empty content: " + url);
                continue;
            }

            int currentDocId = docIdCounter++;
            SourceRecord currentSource = new SourceRecord(currentDocId, url, "Title Placeholder - " + url);
            sources.put(currentDocId, currentSource);

            // --- Text Processing ---
            // 1. Tokenize (split by non-word chars) and Lowercase
            String[] terms = content.toLowerCase().split("\\W+");
            int tokenCount = 0;
            Map<String, Integer> termFrequenciesInDoc = new HashMap<>();

            for (String term : terms) {
                if (term.isEmpty()) continue;

                // 2. Filter out purely numeric tokens (optional, but kept from original)
                if (term.matches("\\d+")) {
                    continue;
                }
                // Consider stricter filter? e.g., minimum length, remove single chars?
                if (term.length() < 2) { // Optional: Remove very short tokens
                    continue;
                }

                tokenCount++;

                // 3. Stop Word Removal (NOT IMPLEMENTED)
                 if (stopWord(term)) continue;

                // 4. Stemming (NOT APPLIED per assignment spec)
                stemmer.addString(term); // Add the term to the stemmer
                stemmer.stem();          // Perform stemming
                String stemmedTerm = stemmer.toString(); // Get the stemmed result

                if (stemmedTerm.isEmpty()) continue; // Skip if stemming produces nothing

                termFrequenciesInDoc.put(stemmedTerm, termFrequenciesInDoc.getOrDefault(stemmedTerm, 0) + 1);
            }
            currentSource.setLength(tokenCount);

            // --- Update Inverted Index ---
            for (Map.Entry<String, Integer> tfEntry : termFrequenciesInDoc.entrySet()) {
                String processedTerm = tfEntry.getKey();
                // No need to stem again, already done above
                int termFreqInThisDoc = tfEntry.getValue();

                DictEntry dictEntry = index.computeIfAbsent(processedTerm, k -> new DictEntry());

                // Check if this document ID is already in the posting list for this term
                // (Should not happen if we process each doc only once, but good practice)
                boolean docAlreadyInPosting = dictEntry.postingListContains(currentDocId);

                if (!docAlreadyInPosting) {
                    dictEntry.incrementDocFreq(); // Increment df only if this doc is new for this term
                    dictEntry.addToTermFreq(termFreqInThisDoc); // Add this doc's TF to corpus TF

                    // Create and add the Posting
                    Posting newPosting = new Posting(currentDocId, termFreqInThisDoc);
                    dictEntry.addPosting(newPosting);
                } else {
                    // This case implies the document was processed twice or logic error
                    System.err.println("Error: Document ID " + currentDocId + " processed multiple times for term '" + processedTerm + "'.");
                }
            }
            // Progress indicator (optional)
            // if (currentDocId % 1 == 0 ) { // Print every document
            //     System.out.println("  Indexed document " + (currentDocId + 1) + "/" + pages.size() + " : " + url);
            // }
        }

        System.out.println("Initial index build complete. Total terms: " + index.size() + ", Total documents: " + sources.size());

        // --- Post-processing: Calculate Document Magnitudes ---
        if (!sources.isEmpty()) {
            System.out.println("Calculating document magnitudes (norms)...");
            calculateAllDocumentMagnitudes();
            System.out.println("Document magnitudes calculated.");
        } else {
            System.out.println("Skipping magnitude calculation as no documents were indexed.");
        }
    }


    // --- Method to calculate magnitudes for all documents (MODIFIED to use TFIDFCalculator) ---
    private void calculateAllDocumentMagnitudes() {
        int N = sources.size();
        if (N == 0) return;

        Map<Integer, Double> docScoresSumOfSquares = new HashMap<>();

        for (Map.Entry<String, DictEntry> indexEntry : index.entrySet()) {
            DictEntry dictEntry = indexEntry.getValue();
            int df = dictEntry.getDoc_freq();
            if (df == 0) continue;

            // Calculate IDF using the external calculator
            double idf = TFIDFCalculator.calculateIdf(N, df);
            if (idf == 0.0) continue; // Skip terms in all docs

            for (Posting post : dictEntry.getPlist()) {
                int docId = post.getDocId();
                int tf = post.getDtf();

                // Calculate TF-IDF using the external calculator
                double tfIdf = TFIDFCalculator.calculateTfIdf(tf, N, df);

                // Accumulate the *square* of the TF-IDF weight
                docScoresSumOfSquares.put(docId,
                        docScoresSumOfSquares.getOrDefault(docId, 0.0) + (tfIdf * tfIdf));
            }
        }

        // Calculate the final magnitude (sqrt of sum of squares)
        docMagnitudes.clear();
        for (Map.Entry<Integer, Double> entry : docScoresSumOfSquares.entrySet()) {
            int docId = entry.getKey();
            double sumOfSquares = entry.getValue();
            if (sumOfSquares > 0) { // Avoid storing magnitude for empty/zero-vector docs
                docMagnitudes.put(docId, Math.sqrt(sumOfSquares));
            }
        }
        System.out.println("Calculated non-zero magnitudes for " + docMagnitudes.size() + " documents.");
    }

    // --- REMOVED: calculateTfWeight and calculateIdf methods ---
    // Now handled by TFIDFCalculator

    // --- Method to process query terms (tokenize, filter, etc.) ---
    // (No changes needed here, still mirrors buildIndex processing)
    private List<String> processQuery(String query) {
        List<String> processedTerms = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return processedTerms;
        }
        String[] terms = query.toLowerCase().split("\\W+");
        for (String term : terms) {
            if (term.isEmpty() || term.matches("\\d+") || term.length() < 2) { // Apply same filters as indexing
                continue;
            }
            if(stopWord(term)) continue; // Apply stop word filter
            // Apply stemming if used
            stemmer.addString(term); // Add the term to the stemmer
            stemmer.stem();          // Perform stemming
            String stemmedTerm = stemmer.toString(); // Get the stemmed result

            if (stemmedTerm.isEmpty()) continue; // Skip if stemming produces nothing
            processedTerms.add(stemmedTerm); // Add to processed terms
        }
        return processedTerms;
    }


    // --- Ranked Search Method (MODIFIED to use calculators) ---
    public List<SearchResult> findQueryRanked(String query) {
        int N = sources.size();
        if (N == 0) {
            System.err.println("Error: Index is empty. Cannot perform search.");
            return Collections.emptyList();
        }

        List<String> queryTerms = processQuery(query);
        if (queryTerms.isEmpty()) {
            System.out.println("Query processed to empty term list. No results.");
            return Collections.emptyList();
        }

        // 1. Calculate Query Vector (TF-IDF weights and magnitude)
        Map<String, Integer> queryTermFrequency = new HashMap<>();
        for (String term : queryTerms) {
            queryTermFrequency.put(term, queryTermFrequency.getOrDefault(term, 0) + 1);
        }

        Map<String, Double> queryTfIdfVector = new HashMap<>();
        double queryMagnitudeSquared = 0.0;

        for (Map.Entry<String, Integer> entry : queryTermFrequency.entrySet()) {
            String term = entry.getKey();
            int qTf = entry.getValue();
            DictEntry dictEntry = index.get(term);

            double termIdf = 0.0;
            if (dictEntry != null && dictEntry.getDoc_freq() > 0) {
                // Use TFIDFCalculator for IDF
                termIdf = TFIDFCalculator.calculateIdf(N, dictEntry.getDoc_freq());
            }

            // Use TFIDFCalculator for TF weight
            double qTfWeight = TFIDFCalculator.calculateTfWeight(qTf);
            double tfIdfValue = qTfWeight * termIdf; // Query term TF-IDF

            if (tfIdfValue > 0) {
                queryTfIdfVector.put(term, tfIdfValue);
                queryMagnitudeSquared += (tfIdfValue * tfIdfValue);
            }
        }

        double queryMagnitude = Math.sqrt(queryMagnitudeSquared);
        if (queryMagnitude == 0.0) {
            System.out.println("Query terms not found in index or have zero relevance (IDF=0). Cannot rank.");
            return Collections.emptyList();
        }

        // 2. Calculate Dot Products using Score Accumulators
        Map<Integer, Double> docScores = new HashMap<>(); // docId -> dot product score

        for (Map.Entry<String, Double> queryVectorEntry : queryTfIdfVector.entrySet()) {
            String term = queryVectorEntry.getKey();
            double queryTermTfIdf = queryVectorEntry.getValue();

            DictEntry dictEntry = index.get(term); // We know this exists if it's in queryTfIdfVector
            if (dictEntry == null) continue; // Should not happen, but defensive check

            // IDF is needed again for document TF-IDF calculation
            double termIdf = TFIDFCalculator.calculateIdf(N, dictEntry.getDoc_freq());
            if (termIdf == 0.0) continue; // Skip if term has zero IDF

            for (Posting post : dictEntry.getPlist()) {
                int docId = post.getDocId();
                int docTf = post.getDtf();

                // Use TFIDFCalculator for document TF-IDF
                double docTermTfIdf = TFIDFCalculator.calculateTfIdf(docTf, N, dictEntry.getDoc_freq());
                // Or: double docTfWeight = TFIDFCalculator.calculateTfWeight(docTf);
                // double docTermTfIdf = docTfWeight * termIdf; // This is equivalent

                // Accumulate dot product component
                docScores.put(docId,
                        docScores.getOrDefault(docId, 0.0) + (queryTermTfIdf * docTermTfIdf));
            }
        }

        // 3. Calculate Final Cosine Similarity Scores and Rank
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Integer, Double> scoreEntry : docScores.entrySet()) {
            int docId = scoreEntry.getKey();
            double dotProduct = scoreEntry.getValue();

            double docMagnitude = docMagnitudes.getOrDefault(docId, 0.0);

            // Use CosineSimilarityCalculator for the final step
            double cosineSimilarity = CosineSimilarityCalculator.calculateCosineSimilarity(dotProduct, queryMagnitude, docMagnitude);

            // Create result if similarity is positive
            if (cosineSimilarity > 0.0) {
                SourceRecord docInfo = sources.get(docId);
                String docIdentifier = (docInfo != null) ? docInfo.getL() : "Unknown Doc ID: " + docId;
                results.add(new SearchResult(docId, cosineSimilarity, docIdentifier));
            }
        }

        // 4. Sort results by score (descending)
        Collections.sort(results); // Uses compareTo in SearchResult
        return results;
    }
    public String findQueryBooleanAnd(String phrase) {
        List<String> queryTerms = processQuery(phrase); // Process query consistently
        if (queryTerms.isEmpty()){
            return "Boolean AND Results for '" + phrase + "':\n  <No valid terms in query>\n";
        }

        Map<Integer, Integer> intersectionMap = new HashMap<>();
        int requiredTermCount = 0;
        List<Integer> firstTermDocIds = null;
        boolean isFirstTerm = true;

        for (String term : queryTerms) {
            DictEntry dictEntry = index.get(term);
            if (dictEntry != null && dictEntry.getDoc_freq() > 0) {
                requiredTermCount++;
                List<Posting> postings = dictEntry.getPlist();
                if (isFirstTerm) {
                    if (postings.isEmpty()) return "Boolean AND Results for '" + phrase + "':\n  <First valid term '" + term + "' not found in any docs>\n";
                    firstTermDocIds = new ArrayList<>(postings.size());
                    for(Posting p : postings) {
                        firstTermDocIds.add(p.getDocId());
                        intersectionMap.put(p.getDocId(), 1);
                    }
                    isFirstTerm = false;
                } else {
                    Set<Integer> currentTermDocIds = new HashSet<>();
                    for (Posting p : postings) { currentTermDocIds.add(p.getDocId()); }
                    for (int docId : new ArrayList<>(intersectionMap.keySet())) {
                        if(currentTermDocIds.contains(docId)) {
                            intersectionMap.put(docId, intersectionMap.get(docId) + 1);
                        } else {
                            intersectionMap.remove(docId);
                        }
                    }
                    if (intersectionMap.isEmpty()) return "Boolean AND Results for '" + phrase + "':\n  <No documents contain all terms up to '" + term + "'>\n";
                }
            } else {
                return "Boolean AND Results for '" + phrase + "':\n  <Term '" + term + "' not found in index. No results possible.>\n";
            }
        }

        StringBuilder resultBuilder = new StringBuilder("Boolean AND Results for '");
        resultBuilder.append(phrase).append("' (Required terms found: ").append(requiredTermCount).append("):\n");
        boolean found = false;
        if (requiredTermCount > 0 && !intersectionMap.isEmpty()) {
            List<Integer> sortedDocIds = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : intersectionMap.entrySet()) {
                if (entry.getValue() == requiredTermCount) {
                    sortedDocIds.add(entry.getKey());
                }
            }
            if (!sortedDocIds.isEmpty()) {
                Collections.sort(sortedDocIds);
                found = true;
                for (int docId : sortedDocIds) {
                    SourceRecord docInfo = sources.get(docId);
                    String docIdentifier = (docInfo != null) ? docInfo.getL() : "Doc ID: " + docId;
                    resultBuilder.append("  - ").append(docIdentifier).append("\n");
                }
            }
        }
        if (!found) { resultBuilder.append("  <No documents found containing all required terms>\n"); }
        return resultBuilder.toString();
    }

    // --- Method to retrieve SourceRecord (useful for Main) ---
    public SourceRecord getSourceRecord(int docId) {
        return sources.get(docId);
    }

    // --- Method to get total number of documents (useful for Main) ---
    public int getNumberOfDocuments() {
        return sources.size();
    }

    // --- Method to get the index size (number of terms) ---
    public int getIndexSize() {
        return index.size();
    }

    // --- Method to get pre-calculated document magnitude ---
    public double getDocumentMagnitude(int docId) {
        return docMagnitudes.getOrDefault(docId, 0.0);
    }


    // --- printDictionary Method (No changes needed) ---
    public void printDictionary() {
        System.out.println("--- Printing Dictionary Sample (" + index.size() + " total terms) ---");
        int count = 0;
        List<String> sortedTerms = new ArrayList<>(index.keySet());
        Collections.sort(sortedTerms);

        for (String term : sortedTerms) {
            DictEntry de = index.get(term);
            System.out.printf("Term: '%-15s' DF: %-4d CorpusTF: %-5d Postings: %d%n",
                    term, de.getDoc_freq(), de.getTerm_freq(), de.getPlist().size());
            if (++count >= 50) {
                System.out.println("... (limiting printout to first 50 terms alphabetically)");
                break;
            }
        }
        System.out.println("--- End of Dictionary Sample ---");
    }
    boolean stopWord(String word) {
        if (word.equals("the") || word.equals("to") || word.equals("be") || word.equals("for") || word.equals("from") || word.equals("in")
                || word.equals("a") || word.equals("into") || word.equals("by") || word.equals("or") || word.equals("and") || word.equals("that")) {
            return true;
        }
        if (word.length() < 2) {
            return true;
        }
        return false;
    }

    // --- Inner Helper Class for Search Results (No changes needed) ---
    public static class SearchResult implements Comparable<SearchResult> {
        private final int docId;
        private final double score;
        private final String identifier; // URL or Title

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
            return String.format("Score: %.6f - %s (ID: %d)", score, identifier, docId);
        }

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
            // Consider equality based on docId only, or docId and score
            return docId == that.docId && Double.compare(that.score, score) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(docId, score);
        }
    }

    // --- REMOVED: Placeholders for SourceRecord and Stemmer classes ---
    // They should be in their own files now.

} // End of Index5 class