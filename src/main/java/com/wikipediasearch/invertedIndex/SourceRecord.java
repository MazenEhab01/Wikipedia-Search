package com.wikipediasearch.invertedIndex; // Make sure this matches your package structure

/**
 * Stores metadata about a source document retrieved by the crawler.
 * Uses the URL as the primary identifier.
 */
public class SourceRecord {
    public String URL;      // The unique identifier for the document (its URL)
    public String title;    // A title for the document (can be derived from URL or content)
    public int length;     // The length of the document (e.g., number of indexed tokens/words)
    public Double norm;     // The magnitude (Euclidean norm) of the document's TF-IDF vector (calculated later)

    // Optional: Store the full text if needed, but be mindful of memory usage.
    // public String text;

    /**
     * Constructor to create a SourceRecord.
     * Initializes length to 0 and norm to 0.0.
     *
     * @param url The URL of the document.
     * @param title A title for the document.
     */
    public SourceRecord(String url, String title) {
        this.URL = url;
        this.title = title;
        this.length = 0;    // Initialize length, will be updated during indexing
        this.norm = 0.0;    // Initialize norm, will be updated after TF-IDF calculation
        // this.text = text; // Uncomment and add text to constructor if needed
    }

    // Optional: Add setters for length and norm if calculated outside the constructor
    public void setLength(int length) {
        this.length = length;
    }

    public void setNorm(Double norm) {
        this.norm = norm;
    }

    // Optional: Add getters if you prefer private fields
    public String getURL() {
        return URL;
    }

    public String getTitle() {
        return title;
    }

    public int getLength() {
        return length;
    }

    public Double getNorm() {
        return norm;
    }
}