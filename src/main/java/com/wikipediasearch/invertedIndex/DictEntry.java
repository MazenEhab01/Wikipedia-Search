package com.wikipediasearch.invertedIndex;


/**
 * Represents an entry in the dictionary (inverted index) for a specific term.
 * Stores statistics about the term's occurrence across the document collection
 * and manages the head/tail of its posting list (a linked list of Posting objects).
 */
public class DictEntry {

    public int doc_freq = 0;    // Number of unique documents containing the term.
    public int term_freq = 0;   // Total number of times the term appears across the entire collection.
    public Posting pList = null; // Head of the posting list (linked list) for this term.
    public Posting last = null;  // Tail of the posting list (for efficient appending).

    /**
     * Default constructor. Initializes frequencies to 0 and lists to null.
     */
    public DictEntry() {
        // Frequencies default to 0, lists default to null
    }

    /**
     * Checks if the posting list for this term contains a specific document ID.
     * @param docId The document identifier (String, e.g., URL) to search for.
     * @return true if the docId is found in the posting list, false otherwise.
     */
    public boolean postingListContains(String docId) {
        Posting current = pList;
        while (current != null) {
            // Use .equals() for String comparison
            if (current.docId != null && current.docId.equals(docId)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Retrieves the term frequency (dtf) for a specific document ID from the posting list.
     * @param docId The document identifier (String, e.g., URL) to search for.
     * @return The term frequency (dtf) in the specified document, or 0 if the document is not found in the list.
     */
    public int getPosting(String docId) {
        Posting current = pList;
        while (current != null) {
            // Use .equals() for String comparison
            if (current.docId != null && current.docId.equals(docId)) {
                return current.dtf; // Return the frequency for this document
            }
            // Optimization: If current docId is alphabetically > target docId,
            // and list is sorted, target won't be found later. (Requires sorted list)
            // For unsorted lists, we must check all nodes.
            current = current.next;
        }
        return 0; // Document ID not found in the posting list for this term
    }

    /**
     * Adds a new Posting node to the end of the linked list for this term.
     * Assumes the posting does not already exist (checking should be done beforehand if necessary).
     * Updates document frequency and collection term frequency.
     *
     * @param docId The document ID (String URL) to add.
     * @param termFrequencyInDoc The frequency (dtf) of the term in this specific document.
     */
    public void addPosting(String docId, int termFrequencyInDoc) {
        Posting newPosting = new Posting(docId, termFrequencyInDoc);

        if (pList == null) {
            // List is empty, new posting is both head and tail
            pList = newPosting;
            last = newPosting;
        } else {
            // Append to the end of the list
            last.next = newPosting;
            last = newPosting; // Update the tail pointer
        }

        // Update statistics for the term
        this.doc_freq += 1;             // Increment document frequency (one more doc contains the term)
        this.term_freq += termFrequencyInDoc; // Increment total term frequency by its count in this new doc
    }

    // Note: The original constructor DictEntry(int df, int tf) might be less useful now
    // as frequencies are typically calculated incrementally during indexing.
}