package com.wikipediasearch.invertedIndex; // Ensure this matches your package structure

import java.util.Objects;

/**
 * Stores metadata about a single source document that has been indexed.
 * Uses an integer ID as the primary key consistent with Index5's usage,
 * but also stores the original URL and title.
 */
public class SourceRecord {

    // Fields are private for encapsulation, final if set only in constructor
    private final int docId;    // Unique integer identifier assigned during indexing
    private final String url;   // The original URL of the document
    private final String title; // The title of the document (can be a placeholder)
    private int length;     // Length of the document (e.g., number of indexed tokens). Initialized to 0.

    // Removed: 'norm' field, as it's handled by Index5.docMagnitudes map

    /**
     * Constructor to create a SourceRecord.
     * Initializes length to 0.
     *
     * @param docId The unique integer ID assigned to this document.
     * @param url   The URL of the document. Cannot be null or empty.
     * @param title A title for the document (e.g., extracted or placeholder). Can be null.
     * @throws IllegalArgumentException if url is null or empty.
     */
    public SourceRecord(int docId, String url, String title) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Document URL cannot be null or empty for docId " + docId);
        }
        this.docId = docId;
        this.url = url;
        this.title = (title != null) ? title : ""; // Use empty string if title is null
        this.length = 0; // Initialize length; can be updated later via setLength()
    }

    // --- Getters ---

    /**
     * Gets the unique integer document ID.
     * @return the document ID.
     */
    public int getDocId() {
        return docId;
    }

    /**
     * Gets the URL of the document.
     * @return the document URL string.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the title of the document.
     * @return the document title string (might be empty).
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the length of the document (e.g., token count).
     * Value should be set using setLength() during indexing if used.
     * @return the document length.
     */
    public int getLength() {
        return length;
    }

    /**
     * Gets the primary locator/identifier string (URL) needed by Index5's result display.
     * This specifically addresses the `getL()` call in Index5.
     * Consider refactoring Index5 to use getUrl() directly for clarity.
     *
     * @return the document URL string.
     */
    public String getL() {
        return this.url;
    }

    // --- Setters ---

    /**
     * Sets the length of the document (e.g., number of tokens indexed).
     * Should be called during the indexing process in Index5 if this value is needed.
     *
     * @param length The calculated length (must be non-negative).
     */
    public void setLength(int length) {
        if (length >= 0) {
            this.length = length;
        } else {
            System.err.println("Warning: Attempted to set negative length (" + length + ") for docId " + this.docId);
            // Optionally throw an exception:
            // throw new IllegalArgumentException("Document length cannot be negative.");
        }
    }

    // Removed: setNorm() method

    // --- Standard Methods ---

    @Override
    public String toString() {
        return "SourceRecord{" +
                "docId=" + docId +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", length=" + length +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceRecord that = (SourceRecord) o;
        // Equality is typically based on the unique document ID
        return docId == that.docId;
    }

    @Override
    public int hashCode() {
        // Hash code based on the unique document ID
        return Objects.hash(docId);
    }
}