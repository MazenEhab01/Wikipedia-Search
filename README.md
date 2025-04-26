# Wikipedia Search Engine

A Java-based Wikipedia web crawler and search engine using TF-IDF and Cosine Similarity.

## Project Description

This project crawls Wikipedia pages starting from two given seed URLs, builds an inverted index from the crawled documents, and allows users to search for queries using TF-IDF and cosine similarity ranking.

- Crawling limited to 10 unique Wikipedia pages.
- Text extraction, tokenization, normalization, and stemming.
- Inverted index construction with term frequency (TF).
- Query processing using TF-IDF weighting and cosine similarity.
- Top 10 relevant documents ranked and displayed.

## Technologies Used

- Java
- Maven
- Jsoup (for HTML parsing)
