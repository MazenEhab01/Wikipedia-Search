package com.wikipediasearch;

import com.wikipediasearch.invertedIndex.Index5;
import com.wikipediasearch.invertedIndex.Posting;
import com.wikipediasearch.invertedIndex.SourceRecord;
import com.wikipediasearch.similarity.CosineSimilarityCalculator;
import com.wikipediasearch.similarity.TFIDFCalculator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*; // Import static assertion methods

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the Wikipedia Search project components.
 * Focuses on calculation logic and input processing.
 */

import static org.junit.jupiter.api.Assertions.*;
class Test {
    private static final double DELTA = 0.00001; // Tolerance for floating-point comparisons
    private static Index5 testIndexInstance; // Instance needed for non-static processQuery

    @BeforeAll
    static void setUp() {
        // Create an instance of Index5 to test its non-static methods like processQuery
        testIndexInstance = new Index5();
    }

    // --- TFIDFCalculator Tests ---

    @Test
    @DisplayName("TF Weight Calculation (1 + log10(tf))")
    void testTfWeightCalculation() {
        assertEquals(1.0, TFIDFCalculator.calculateTfWeight(1), DELTA, "TF=1 should yield weight=1.0");
        assertEquals(2.0, TFIDFCalculator.calculateTfWeight(10), DELTA, "TF=10 should yield weight=2.0");
        assertEquals(1 + Math.log10(5), TFIDFCalculator.calculateTfWeight(5), DELTA, "TF=5 calculation");
        assertEquals(0.0, TFIDFCalculator.calculateTfWeight(0), DELTA, "TF=0 should yield weight=0.0");
        assertEquals(0.0, TFIDFCalculator.calculateTfWeight(-5), DELTA, "Negative TF should yield weight=0.0");
    }

    @Test
    @DisplayName("IDF Calculation (log10(N / df))")
    void testIdfCalculation() {
        int N = 10; // Sample collection size
        assertEquals(1.0, TFIDFCalculator.calculateIdf(N, 1), DELTA, "df=1 should yield IDF=1.0");
        assertEquals(0.0, TFIDFCalculator.calculateIdf(N, 10), DELTA, "df=N should yield IDF=0.0");
        assertEquals(Math.log10(2.0), TFIDFCalculator.calculateIdf(N, 5), DELTA, "df=5 calculation");
        assertEquals(0.0, TFIDFCalculator.calculateIdf(N, 0), DELTA, "df=0 should yield IDF=0.0");
        assertEquals(0.0, TFIDFCalculator.calculateIdf(N, 11), DELTA, "df > N should yield IDF=0.0");
        assertEquals(0.0, TFIDFCalculator.calculateIdf(0, 5), DELTA, "N=0 should yield IDF=0.0");
        assertEquals(0.0, TFIDFCalculator.calculateIdf(-5, 5), DELTA, "Negative N should yield IDF=0.0");
        assertEquals(0.0, TFIDFCalculator.calculateIdf(N, -2), DELTA, "Negative df should yield IDF=0.0");
    }

    @Test
    @DisplayName("TF-IDF Score Calculation (TF-Weight * IDF)")
    void testTfIdfCalculation() {
        int N = 10;
        assertEquals((1 + Math.log10(5)) * Math.log10(10.0/1.0), TFIDFCalculator.calculateTfIdf(5, N, 1), DELTA, "Standard TF-IDF case");
        assertEquals(0.0, TFIDFCalculator.calculateTfIdf(10, N, 10), DELTA, "TF-IDF when df=N (IDF=0)");
        assertEquals(0.0, TFIDFCalculator.calculateTfIdf(0, N, 5), DELTA, "TF-IDF when TF=0");
        assertEquals(0.0, TFIDFCalculator.calculateTfIdf(5, N, 0), DELTA, "TF-IDF when df=0 (IDF=0)");
    }

    // --- CosineSimilarityCalculator Tests ---

    @Test
    @DisplayName("Vector Magnitude (Euclidean Norm)")
    void testMagnitudeCalculation() {
        Map<String, Double> vec1 = Map.of("a", 3.0, "b", 4.0); // Magnitude 5
        Map<String, Double> vec2 = Map.of("c", 1.0, "d", 0.0); // Magnitude 1
        Map<String, Double> vec3 = Map.of("e", -2.0);          // Magnitude 2
        Map<String, Double> vecEmpty = Collections.emptyMap();

        assertEquals(5.0, CosineSimilarityCalculator.calculateMagnitude(vec1), DELTA, "Magnitude of {a=3, b=4}");
        assertEquals(1.0, CosineSimilarityCalculator.calculateMagnitude(vec2), DELTA, "Magnitude of {c=1, d=0}");
        assertEquals(2.0, CosineSimilarityCalculator.calculateMagnitude(vec3), DELTA, "Magnitude of {e=-2}");
        assertEquals(0.0, CosineSimilarityCalculator.calculateMagnitude(vecEmpty), DELTA, "Magnitude of empty vector");
        assertEquals(0.0, CosineSimilarityCalculator.calculateMagnitude(null), DELTA, "Magnitude of null vector");
    }

    @Test
    @DisplayName("Vector Dot Product")
    void testDotProductCalculation() {
        Map<String, Double> vecA = Map.of("x", 2.0, "y", 1.0);
        Map<String, Double> vecB = Map.of("x", 3.0, "z", 5.0); // Overlap on 'x'
        Map<String, Double> vecC = Map.of("w", 1.0, "z", 5.0); // No overlap with A
        Map<String, Double> vecD = Map.of("x", -1.0, "y", 2.0, "w": 3.0); // Overlap on x, y with A
        Map<String, Double> vecEmpty = Collections.emptyMap();

        assertEquals(6.0, CosineSimilarityCalculator.calculateDotProduct(vecA, vecB), DELTA, "Dot product A.B (overlap x)");
        assertEquals(6.0, CosineSimilarityCalculator.calculateDotProduct(vecB, vecA), DELTA, "Dot product B.A (commutative)");
        assertEquals(0.0, CosineSimilarityCalculator.calculateDotProduct(vecA, vecC), DELTA, "Dot product A.C (no overlap)");
        assertEquals(0.0, CosineSimilarityCalculator.calculateDotProduct(vecA, vecEmpty), DELTA, "Dot product A.Empty");
        assertEquals(0.0, CosineSimilarityCalculator.calculateDotProduct(vecEmpty, vecB), DELTA, "Dot product Empty.B");
        assertEquals(0.0, CosineSimilarityCalculator.calculateDotProduct(vecEmpty, vecEmpty), DELTA, "Dot product Empty.Empty");
        assertEquals((2.0 * -1.0) + (1.0 * 2.0), CosineSimilarityCalculator.calculateDotProduct(vecA, vecD), DELTA, "Dot product A.D (overlap x, y)");
        assertEquals(0.0, CosineSimilarityCalculator.calculateDotProduct(null, vecB), DELTA, "Dot product null.B");
        assertEquals(0.0, CosineSimilarityCalculator.calculateDotProduct(vecA, null), DELTA, "Dot product A.null");
    }


    @Test
    @DisplayName("Cosine Similarity Calculation")
    void testCosineSimilarityCalculation() {
        // Vectors: vecA = {x=2, y=1} mag=sqrt(5), vecB = {x=3, z=5} mag=sqrt(34) Dot=6
        double magA = Math.sqrt(5.0);
        double magB = Math.sqrt(34.0);
        double dotAB = 6.0;
        assertEquals(dotAB / (magA * magB), CosineSimilarityCalculator.calculateCosineSimilarity(dotAB, magA, magB), DELTA, "Standard cosine similarity");

        // Vectors: vecA={a=3, b=4} mag=5, vecC={c=1} mag=1 Dot=0
        assertEquals(0.0, CosineSimilarityCalculator.calculateCosineSimilarity(0.0, 5.0, 1.0), DELTA, "Zero dot product -> zero similarity");

        // Test zero magnitudes
        assertEquals(0.0, CosineSimilarityCalculator.calculateCosineSimilarity(10.0, 0.0, 5.0), DELTA, "Zero magnitude 1");
        assertEquals(0.0, CosineSimilarityCalculator.calculateCosineSimilarity(10.0, 5.0, 0.0), DELTA, "Zero magnitude 2");
        assertEquals(0.0, CosineSimilarityCalculator.calculateCosineSimilarity(0.0, 0.0, 0.0), DELTA, "Both magnitudes zero");

        // Test perfect similarity (vectors are scalar multiples)
        // vecE={x=1, y=2} mag=sqrt(5), vecF={x=2, y=4} mag=sqrt(20)=2*sqrt(5), Dot=1*2+2*4=10
        assertEquals(10.0 / (Math.sqrt(5.0) * Math.sqrt(20.0)), CosineSimilarityCalculator.calculateCosineSimilarity(10.0, Math.sqrt(5.0), Math.sqrt(20.0)), DELTA, "Perfect similarity (should be close to 1.0)");
        // Check clamping if needed (though previous result should be very close to 1)
        assertEquals(1.0, CosineSimilarityCalculator.calculateCosineSimilarity(10.0, Math.sqrt(5.0), Math.sqrt(20.0)), DELTA, "Perfect similarity check");
    }

    // --- Index5 Tests (Focus on query processing) ---

    @Test
    @DisplayName("Query Processing - Basic")
    void testProcessQueryBasic() {
        String query = "Pharaoh ancient Egypt";
        List<String> expected = Arrays.asList("pharaoh", "ancient", "egypt");
        List<String> actual = testIndexInstance.processQuery(query);
        assertEquals(expected, actual, "Basic query tokenization and lowercasing");
    }

    @Test
    @DisplayName("Query Processing - With Punctuation and Numbers")
    void testProcessQueryComplex() {
        String query = "The 18th Dynasty, queen's name? Ankhesenamun!";
        // Filters: lowercase, split \\W+, remove numeric-only, remove len<2
        List<String> expected = Arrays.asList("the", "dynasty", "queen", "name", "ankhesenamun");
        List<String> actual = testIndexInstance.processQuery(query);
        assertEquals(expected, actual, "Query with punctuation, numbers, short words");
    }

    @Test
    @DisplayName("Query Processing - Only Numbers and Short Words")
    void testProcessQueryFilteredOut() {
        String query = "123 456 a b 789";
        List<String> expected = Collections.emptyList();
        List<String> actual = testIndexInstance.processQuery(query);
        assertEquals(expected, actual, "Query with only filtered tokens");
    }

    @Test
    @DisplayName("Query Processing - Empty and Null")
    void testProcessQueryEmptyNull() {
        assertEquals(Collections.emptyList(), testIndexInstance.processQuery(""), "Empty query");
        assertEquals(Collections.emptyList(), testIndexInstance.processQuery("   "), "Whitespace query");
        assertEquals(Collections.emptyList(), testIndexInstance.processQuery(null), "Null query");
    }

    // --- Data Holder Class Tests (Optional but good for validation) ---

    @Test
    @DisplayName("Posting Constructor Validation")
    void testPostingConstructor() {
        // Valid case
        Posting p1 = new Posting(10, 5);
        assertEquals(10, p1.getDocId());
        assertEquals(5, p1.getDtf());

        // Edge case dtf=1
        Posting p2 = new Posting(11, 1);
        assertEquals(1, p2.getDtf());

        // Warning case dtf=0 (should still create object)
        Posting p3 = new Posting(12, 0);
        assertEquals(0, p3.getDtf());

        // Invalid case docId < 0
        assertThrows(IllegalArgumentException.class, () -> {
            new Posting(-1, 5);
        }, "Negative docId should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("SourceRecord Constructor Validation")
    void testSourceRecordConstructor() {
        // Valid case
        SourceRecord sr1 = new SourceRecord(1, "http://example.com", "Example Title");
        assertEquals(1, sr1.getDocId());
        assertEquals("http://example.com", sr1.getUrl());
        assertEquals("Example Title", sr1.getTitle());
        assertEquals(0, sr1.getLength()); // Initial length is 0

        // Null title case
        SourceRecord sr2 = new SourceRecord(2, "http://another.com", null);
        assertEquals("", sr2.getTitle(), "Null title should default to empty string");

        // Invalid URL cases
        assertThrows(IllegalArgumentException.class, () -> {
            new SourceRecord(3, null, "Title");
        }, "Null URL should throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> {
            new SourceRecord(4, "", "Title");
        }, "Empty URL should throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> {
            new SourceRecord(5, "   ", "Title"); // Whitespace only URL
        }, "Whitespace URL should throw IllegalArgumentException");
    }
}