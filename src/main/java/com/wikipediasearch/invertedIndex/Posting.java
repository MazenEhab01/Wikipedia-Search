package com.wikipediasearch.invertedIndex; // Make sure this matches your package structure

/**
 * Represents an entry in a posting list for a term.
 * It identifies a document where the term appears and the term's frequency within that document.
 * Uses a String (typically a URL) for the document identifier.
 */
public class Posting {

    public Posting next = null; // Pointer to the next posting in the linked list for the same term
    public String docId;        // The identifier of the document (e.g., URL)
    public int dtf;             // Document Term Frequency: how many times the term appears in this document (docId)

    /**
     * Constructor for creating a Posting with a document ID and term frequency.
     * @param id The document identifier (String, e.g., URL).
     * @param t The term frequency (dtf) in this document.
     */
    public Posting(String id, int t) {
        this.docId = id;
        this.dtf = t;
        this.next = null; // Initialize next to null
    }

    /**
     * Constructor for creating a Posting with only a document ID.
     * Term frequency (dtf) defaults to 1 (or should be set later if needed).
     * Note: The constructor Posting(String id, int t) is generally preferred
     *       when the term frequency is known at creation time.
     * @param id The document identifier (String, e.g., URL).
     */
    public Posting(String id) {
        this.docId = id;
        this.dtf = 1; // Default dtf to 1, might need adjustment based on usage
        this.next = null;
    }

    // Optional: Add getters if you prefer private fields
    // public String getDocId() { return docId; }
    // public int getDtf() { return dtf; }
    // public Posting getNext() { return next; }
}