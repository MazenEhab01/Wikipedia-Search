package com.wikipediasearch.similarity;

/**
 * Provides static utility methods for calculating TF-IDF components.
 */
public class TFIDFCalculator {

    /**
     * Calculates the Term Frequency (TF) weight using the (1 + log10(tf)) formula.
     *
     * @param tf The raw term frequency (number of times a term appears in a document).
     * @return The calculated TF weight. Returns 0 if tf is non-positive.
     */
    public static double calculateTfWeight(int tf) {
        if (tf <= 0) {
            return 0.0; // Term doesn't appear or frequency is non-positive
        }
        return 1 + Math.log10(tf);
    }

    /**
     * Calculates the Inverse Document Frequency (IDF) using the log10(N / df) formula.
     *
     * @param N  The total number of documents in the collection.
     * @param df The document frequency (number of documents containing the term).
     * @return The calculated IDF weight. Returns 0 if df is non-positive, N is zero, or df > N.
     *         Returns 0 if df equals N (term is in all documents).
     */
    public static double calculateIdf(int N, int df) {
        if (N <= 0 || df <= 0 || df > N) {
            // Handle edge cases: invalid N or df, or df > N (shouldn't happen)
            return 0.0;
        }
        // If df == N, idf is log10(1) = 0. This is correct.
        return Math.log10((double) N / df);
    }

    /**
     * Calculates the TF-IDF score for a term in a document.
     *
     * @param tf The raw term frequency in the document.
     * @param N  The total number of documents in the collection.
     * @param df The document frequency of the term.
     * @return The calculated TF-IDF score (TF weight * IDF weight).
     */
    public static double calculateTfIdf(int tf, int N, int df) {
        double tfWeight = calculateTfWeight(tf);
        double idfWeight = calculateIdf(N, df);
        return tfWeight * idfWeight;
    }
}