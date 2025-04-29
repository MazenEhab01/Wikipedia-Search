package com.wikipediasearch.similarity;

import java.util.Map;

/**
 * Provides static utility methods related to Cosine Similarity calculations.
 */
public class CosineSimilarityCalculator {

    /**
     * Calculates the cosine similarity between two vectors, given their dot product
     * and their pre-calculated magnitudes (Euclidean norms).
     * Handles cases where magnitudes might be zero to avoid division by zero.
     *
     * @param dotProduct The dot product of the two vectors.
     * @param magnitude1 The magnitude (Euclidean norm) of the first vector.
     * @param magnitude2 The magnitude (Euclidean norm) of the second vector.
     * @return The cosine similarity score (between 0 and 1, typically), or 0 if either magnitude is zero.
     */
    public static double calculateCosineSimilarity(double dotProduct, double magnitude1, double magnitude2) {
        // Check for zero magnitudes to prevent division by zero
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0; // Cosine similarity is undefined or zero if one vector has zero length
        }

        double similarity = dotProduct / (magnitude1 * magnitude2);

        // Clamp the value between 0 and 1 (due to potential floating point inaccuracies)
        if (similarity < 0.0) {
            return 0.0;
        }
        if (similarity > 1.0) {
            // This can sometimes happen with floating point math, clamp it
            // System.err.printf("Warning: Cosine similarity > 1 (%.8f), clamping to 1.0%n", similarity);
            return 1.0;
        }

        // Ensure score is not NaN or infinite
        if (Double.isNaN(similarity) || Double.isInfinite(similarity)) {
            System.err.println("Warning: Cosine similarity resulted in NaN or Infinity, returning 0.0");
            return 0.0;
        }

        return similarity;
    }

    /**
     * Calculates the magnitude (Euclidean norm) of a vector represented as a map
     * of term -> weight.
     * Norm = sqrt(sum of squares of weights).
     *
     * @param vector The vector (e.g., TF-IDF weights).
     * @return The magnitude of the vector.
     */
    public static double calculateMagnitude(Map<String, Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return 0.0;
        }
        double sumOfSquares = 0.0;
        for (double weight : vector.values()) {
            sumOfSquares += weight * weight;
        }
        return Math.sqrt(sumOfSquares);
    }

    /**
     * Calculates the dot product between two vectors represented as maps
     * of term -> weight. Only terms present in *both* vectors contribute.
     *
     * @param vector1 First vector map.
     * @param vector2 Second vector map.
     * @return The dot product.
     */
    public static double calculateDotProduct(Map<String, Double> vector1, Map<String, Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;

        // Iterate through the smaller vector for efficiency
        Map<String, Double> smallerMap = (vector1.size() < vector2.size()) ? vector1 : vector2;
        Map<String, Double> largerMap = (vector1.size() < vector2.size()) ? vector2 : vector1;

        for (Map.Entry<String, Double> entry : smallerMap.entrySet()) {
            String term = entry.getKey();
            Double weight1 = entry.getValue(); // Weight from the smaller map

            // Check if the term exists in the larger map
            Double weight2 = largerMap.get(term);

            if (weight2 != null) { // Term exists in both vectors
                dotProduct += weight1 * weight2;
            }
        }
        return dotProduct;
    }
}