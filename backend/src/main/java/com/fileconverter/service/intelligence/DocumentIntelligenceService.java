package com.fileconverter.service.intelligence;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIntelligenceService.class);

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with",
            "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her",
            "she", "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up",
            "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time",
            "no", "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", "could",
            "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think",
            "also", "back", "after", "use", "two", "how", "our", "work", "first", "well", "way", "even",
            "new", "want", "because", "any", "these", "give", "day", "most", "us", "is", "are", "was", "were"
    ));

    public Map<String, Object> summarizeDocument(File pdfOrTextFile, int maxSentences) throws IOException {
        String text = extractFullText(pdfOrTextFile);
        List<String> sentences = splitIntoSentences(text);

        if (sentences.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("summary", "Document contains no readable text.");
            empty.put("keyPoints", Collections.emptyList());
            return empty;
        }

        // Compute Word Frequency (TF)
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String s : sentences) {
            for (String word : s.toLowerCase().split("[^a-zA-Z0-9]+")) {
                if (word.length() > 3 && !STOP_WORDS.contains(word)) {
                    wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Score sentences by word frequency and positional prominence
        List<SentenceScore> scored = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String s = sentences.get(i);
            double score = 0;
            int count = 0;
            for (String word : s.toLowerCase().split("[^a-zA-Z0-9]+")) {
                if (wordFreq.containsKey(word)) {
                    score += wordFreq.get(word);
                    count++;
                }
            }
            if (count > 0) score /= Math.sqrt(count); // normalize by length
            // Boost beginning sentences
            if (i < 3) score *= 1.3;

            scored.add(new SentenceScore(s, score, i));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        int target = Math.min(sentences.size(), Math.max(3, maxSentences));
        List<SentenceScore> top = scored.subList(0, target);
        // Re-sort by original chronological document order
        top.sort(Comparator.comparingInt(a -> a.index));

        StringBuilder summaryText = new StringBuilder();
        List<String> keyPoints = new ArrayList<>();
        for (SentenceScore ss : top) {
            summaryText.append(ss.sentence).append(" ");
            keyPoints.add(ss.sentence);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("summary", summaryText.toString().trim());
        res.put("keyPoints", keyPoints);
        res.put("totalSentences", sentences.size());
        res.put("compressionRatio", String.format("%.1f%%", (1.0 - (double) target / sentences.size()) * 100.0));
        res.put("engine", "In-House Graph Salience & Term Salience NLP Extractor");

        return res;
    }

    public Map<String, Object> answerQuestion(File pdfOrTextFile, String question) throws IOException {
        String text = extractFullText(pdfOrTextFile);
        List<String> sentences = splitIntoSentences(text);

        Set<String> qTokens = new HashSet<>();
        for (String w : question.toLowerCase().split("[^a-zA-Z0-9]+")) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) {
                qTokens.add(w);
            }
        }

        SentenceScore best = null;
        List<Map<String, Object>> relevantPassages = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i++) {
            String s = sentences.get(i);
            int matchCount = 0;
            for (String w : s.toLowerCase().split("[^a-zA-Z0-9]+")) {
                if (qTokens.contains(w)) matchCount++;
            }

            if (matchCount > 0) {
                double score = (double) matchCount / Math.max(1, qTokens.size());
                SentenceScore current = new SentenceScore(s, score, i);
                if (best == null || current.score > best.score) {
                    best = current;
                }
                Map<String, Object> p = new HashMap<>();
                p.put("passage", s);
                p.put("relevanceScore", Math.round(score * 100.0) / 100.0);
                relevantPassages.add(p);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        if (best != null) {
            result.put("answer", best.sentence);
            result.put("confidence", Math.min(100.0, Math.round(best.score * 100.0)));
        } else {
            result.put("answer", "No exact match found in the document for the query tokens.");
            result.put("confidence", 0.0);
        }
        result.put("relevantPassages", relevantPassages.stream().limit(5).toList());
        return result;
    }

    public Map<String, Object> extractEntities(File pdfOrTextFile) throws IOException {
        String text = extractFullText(pdfOrTextFile);
        Map<String, Object> result = new LinkedHashMap<>();

        // Email regex
        Set<String> emails = new LinkedHashSet<>();
        Matcher emailMatcher = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}").matcher(text);
        while (emailMatcher.find()) emails.add(emailMatcher.group());

        // Phone numbers
        Set<String> phones = new LinkedHashSet<>();
        Matcher phoneMatcher = Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}").matcher(text);
        while (phoneMatcher.find()) phones.add(phoneMatcher.group());

        // Dates (YYYY-MM-DD, DD/MM/YYYY, Month DD, YYYY)
        Set<String> dates = new LinkedHashSet<>();
        Matcher dateMatcher = Pattern.compile("(\\b\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b)|(\\b\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}\\b)|(\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},?\\s+\\d{4}\\b)", Pattern.CASE_INSENSITIVE).matcher(text);
        while (dateMatcher.find()) dates.add(dateMatcher.group());

        // Currencies & Amounts ($100, €50, £25.50)
        Set<String> amounts = new LinkedHashSet<>();
        Matcher amountMatcher = Pattern.compile("[$€£¥₹]\\s*\\d+([.,]\\d{2})?|\\b\\d+([.,]\\d{2})?\\s*(USD|EUR|GBP|INR)\\b").matcher(text);
        while (amountMatcher.find()) amounts.add(amountMatcher.group());

        // Headings / Sections
        List<String> headings = extractHeadings(text);

        result.put("emails", emails);
        result.put("phoneNumbers", phones);
        result.put("dates", dates);
        result.put("monetaryAmounts", amounts);
        result.put("documentHeadings", headings);

        return result;
    }

    public Map<String, Object> generateStudyNotes(File pdfOrTextFile) throws IOException {
        String text = extractFullText(pdfOrTextFile);
        List<String> sentences = splitIntoSentences(text);

        List<Map<String, String>> flashcards = new ArrayList<>();
        List<String> keyTerms = new ArrayList<>();

        for (String s : sentences) {
            if (s.contains(" is ") || s.contains(" are ") || s.contains(" refers to ") || s.contains(" means ")) {
                String[] parts = s.split("( is | are | refers to | means )", 2);
                if (parts.length == 2 && parts[0].split("\\s+").length <= 4 && parts[1].length() > 10) {
                    Map<String, String> card = new HashMap<>();
                    card.put("front", "What is " + parts[0].trim() + "?");
                    card.put("back", s.trim());
                    flashcards.add(card);
                    keyTerms.add(parts[0].trim());
                }
            }
        }

        Map<String, Object> summary = summarizeDocument(pdfOrTextFile, 5);

        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("title", "Study Guide & Notes: " + pdfOrTextFile.getName());
        notes.put("keyTakeaways", summary.get("keyPoints"));
        notes.put("flashcards", flashcards.stream().limit(10).toList());
        notes.put("extractedKeyTerms", keyTerms.stream().distinct().limit(12).toList());

        return notes;
    }

    public List<String> extractHeadings(String text) {
        List<String> headings = new ArrayList<>();
        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            // Check if line looks like a title or numbered heading
            if (t.matches("^(\\d+(\\.\\d+)*|[A-Z])\\s+.*") && t.length() < 70) {
                headings.add(t);
            } else if (t.equals(t.toUpperCase()) && t.length() > 4 && t.length() < 60 && !t.matches(".*\\d.*")) {
                headings.add(t);
            }
        }
        return headings;
    }

    private String extractFullText(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(file)) {
                return new PDFTextStripper().getText(doc);
            }
        } else {
            return java.nio.file.Files.readString(file.toPath());
        }
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null) return sentences;
        String clean = text.replaceAll("\\r", " ").replaceAll("\\n+", " ");
        String[] raw = clean.split("(?<=[.!?])\\s+");
        for (String s : raw) {
            String t = s.trim();
            if (t.length() >= 15 && t.split("\\s+").length >= 4) {
                sentences.add(t);
            }
        }
        return sentences;
    }

    private static class SentenceScore {
        String sentence;
        double score;
        int index;

        SentenceScore(String sentence, double score, int index) {
            this.sentence = sentence;
            this.score = score;
            this.index = index;
        }
    }
}
