package com.wikipediasearch.invertedIndex;

import java.util.LinkedList; // Using LinkedList as implied by original Index5 usage
import java.util.List;      // Import List interface
import java.util.Objects;   // For hashCode

/**
 * Represents an entry in the dictionary (inverted index) for a specific term.
 * Stores statistics about the term's occurrence across the document collection
 * and manages a list of Posting objects.
 * This version uses integer document IDs and java.util.List for postings.
 */
public class DictEntry {

    private int doc_freq = 0;    // Number of unique documents containing the term.
    private int term_freq = 0;   // Total number of times the term appears across the entire collection.
    // Changed: Replaced custom linked list (pList, last) with java.util.List
    private List<Posting> pList; // List of postings (docId, dtf) for this term.

    /**
     * Default constructor. Initializes frequencies to 0 and creates an empty posting list.
     */
    public DictEntry() {
        this.doc_freq = 0;
        this.term_freq = 0;
        // Initialize with a concrete List implementation, LinkedList is a common choice for indices
        this.pList = new LinkedList<>();
    }

    // --- Getters for encapsulated fields ---

    /**
     * Gets the document frequency (df) of the term.
     * This is the number of unique documents the term appears in.
     * @return The document frequency.
     */
    public int getDoc_freq() {
        return doc_freq;
    }

    /**
     * Gets the total term frequency (tf) of the term across the entire collection.
     * Note: This might not be strictly necessary for all ranking models but is often stored.
     * @return The total term frequency.
     */
    public int getTerm_freq() {
        return term_freq;
    }

    /**
     * Gets the list of postings for this term.
     * Each Posting contains a document ID (int) and the term frequency (dtf) in that document.
     * @return The list of Postings. It's recommended not to modify the returned list directly
     *         outside the indexing process unless intended.
     */
    public List<Posting> getPlist() {
        return pList;
    }

    // --- Setters / Modifiers (used during index construction) ---

    /**
     * Increments the document frequency (df) for this term.
     * Typically called when the term is found in a new document during indexing.
     */
    public void incrementDocFreq() {
        this.doc_freq++;
    }

    /**
     * Adds to the total term frequency for this term across the collection.
     * Typically called during indexing, adding the dtf from a specific document.
     * @param frequencyInDoc The term frequency (dtf) from the document being processed.
     */
    public void addToTermFreq(int frequencyInDoc) {
        if (frequencyInDoc > 0) {
            this.term_freq += frequencyInDoc;
        }
    }

    /**
     * Adds a new Posting to the end of the posting list for this term.
     * This method solely adds the Posting object to the list.
     * Responsibility for updating doc_freq and term_freq should ideally be handled
     * by the calling code (e.g., the indexer) that determines *when* to call this.
     * (Our Index5 code currently updates frequencies directly in buildIndex before adding).
     *
     * @param posting The Posting object (containing docId and dtf) to add.
     */
    public void addPosting(Posting posting) {
        if (posting != null) {
            this.pList.add(posting); // Add to the end of the list
        }
    }


    // --- Utility Methods (adapted from original) ---

    /**
     * Checks if the posting list for this term contains a specific document ID.
     * @param docId The integer document identifier to search for.
     * @return true if the docId is found in the posting list, false otherwise.
     */
    public boolean postingListContains(int docId) {
        for (Posting p : pList) {
            if (p.getDocId() == docId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the term frequency (dtf) for a specific document ID from the posting list.
     * @param docId The integer document identifier to search for.
     * @return The term frequency (dtf) in the specified document, or 0 if the document is not found in the list.
     */
    public int getTermFrequencyInDoc(int docId) {
        for (Posting p : pList) {
            if (p.getDocId() == docId) {
                return p.getDtf(); // Return the frequency for this document
            }
        }
        return 0; // Document ID not found in the posting list for this term
    }


    // --- Standard Methods ---

    @Override
    public String toString() {
        // Provides a concise representation for debugging
        return "DictEntry{" +
                "df=" + doc_freq +
                ", tf=" + term_freq +
                ", postings=" + pList.size() + // Show count instead of full list usually
                // ", pList=" + pList + // Uncomment to see full posting list
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictEntry dictEntry = (DictEntry) o;
        // Equality based on frequencies and the content of the posting list
        return doc_freq == dictEntry.doc_freq &&
                term_freq == dictEntry.term_freq &&
                Objects.equals(pList, dictEntry.pList); // List equality checks element-wise
    }

    @Override
    public int hashCode() {
        // Hash code based on frequencies and the posting list content
        return Objects.hash(doc_freq, term_freq, pList);
    }
}