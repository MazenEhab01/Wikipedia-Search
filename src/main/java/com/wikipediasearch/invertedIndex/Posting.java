package com.wikipediasearch.invertedIndex; // Make sure this matches your package structure

import java.util.Objects; // For hashCode generation

/**
 * Represents an entry in a posting list for a term.
 * It identifies a document (by its integer ID) where the term appears
 * and the term's frequency within that document.
 * This version uses an integer for docId and is designed to be stored
 * within standard Java collections (like List) in the DictEntry,
 * rather than acting as a node in a custom linked list.
 */
public class Posting {

    // Removed: public Posting next = null;
    // We use List<Posting> in DictEntry, so the list manages the sequence.

    private int docId; // The unique integer identifier of the document
    private int dtf;   // Document Term Frequency: how many times the term appears in this document

    /**
     * Constructor for creating a Posting with a document ID and term frequency.
     *
     * @param docId The integer document identifier. Must be non-negative.
     * @param dtf   The term frequency (dtf) in this document. Should be positive.
     * @throws IllegalArgumentException if docId is negative.
     */
    public Posting(int docId, int dtf) {
        if (docId < 0) {
            throw new IllegalArgumentException("Document ID cannot be negative. Received: " + docId);
        }
        // Typically, dtf should be >= 1 if a term exists in a document.
        // A warning might be useful if dtf <= 0, but we allow it for flexibility.
        if (dtf <= 0) {
            System.err.printf("Warning: Creating Posting with non-positive dtf (%d) for docId %d%n", dtf, docId);
        }

        this.docId = docId;
        this.dtf = dtf;
    }

    /**
     * Gets the integer document identifier.
     *
     * @return The document ID.
     */
    public int getDocId() {
        return docId;
    }

    /**
     * Gets the term frequency within this document.
     *
     * @return The document term frequency (dtf).
     */
    public int getDtf() {
        return dtf;
    }

    // --- Standard Methods for usability ---

    @Override
    public String toString() {
        // Useful for debugging, e.g., when printing DictEntry contents
        return "(" + docId + "," + dtf + ")";
    }

    @Override
    public boolean equals(Object o) {
        // Important if you ever need to compare Posting objects, e.g., in tests or Sets
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Posting posting = (Posting) o;
        return docId == posting.docId && dtf == posting.dtf;
    }

    @Override
    public int hashCode() {
        // Important if you ever use Posting objects as keys in HashMaps or store them in HashSets
        return Objects.hash(docId, dtf);
    }
}