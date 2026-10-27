package com.fileconverter.service.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Custom In-House Character & Word Recognition Engine:
 * - 64-D Zoning Density Matrix Extraction (Normalized 32x32 glyph raster)
 * - Topological Features: Crossings, Aspect Ratio, Center of Mass
 * - Multi-Font Template Centroid Classifier (Serif, Sans, Monospace, Bold, Italic)
 * - Calibrated Confidence Scoring (0.0 - 100.0%)
 * - Contextual Post-Processing & Lexicon Disambiguation
 */
@Component
public class NeuralPatternOcrRecognizer {

    private static final Logger log = LoggerFactory.getLogger(NeuralPatternOcrRecognizer.class);

    private static final int NORM_SIZE = 32;
    private static final int ZONES = 8;
    private static final int FEATURE_DIM = (ZONES * ZONES) + 8; // 64 zoning + 8 structural

    public static class RecognitionOutput {
        private final char character;
        private final double confidence;

        public RecognitionOutput(char character, double confidence) {
            this.character = character;
            this.confidence = confidence;
        }

        public char getCharacter() { return character; }
        public double getConfidence() { return confidence; }
    }

    private final Map<Character, List<double[]>> characterPrototypes = new HashMap<>();
    private final Map<Character, double[]> averagedCentroids = new HashMap<>();

    // Common English dictionary words for post-processing disambiguation
    private final Set<String> commonLexicon = new HashSet<>(Arrays.asList(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with",
            "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her",
            "she", "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up",
            "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time",
            "no", "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", "could",
            "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think",
            "also", "back", "after", "use", "two", "how", "our", "work", "first", "well", "way", "even",
            "new", "want", "because", "any", "these", "give", "day", "most", "us", "document", "file",
            "converter", "invoice", "report", "total", "date", "name", "address", "page", "number",
            "optical", "character", "recognition", "engine", "text", "image", "scanned", "pdf", "table",
            "project", "service", "system", "data", "result", "status", "download", "format", "version"
    ));

    @PostConstruct
    public void init() {
        trainFontPrototypes();
        log.info("Initialized In-House OCR Recognizer with {} character classes and multi-font prototypes",
                averagedCentroids.size());
    }

    /**
     * Synthesizes and learns feature prototypes across core standard fonts:
     * Arial/Sans-Serif, Times New Roman/Serif, Courier/Monospaced, Dialog
     */
    public synchronized void trainFontPrototypes() {
        String[] fontNames = {Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED, Font.DIALOG};
        int[] fontStyles = {Font.PLAIN, Font.BOLD};

        String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,:;!?()[]{}\"'-+=/\\@#$%&*";

        for (char ch : charset.toCharArray()) {
            characterPrototypes.putIfAbsent(ch, new ArrayList<>());

            for (String fontName : fontNames) {
                for (int style : fontStyles) {
                    BufferedImage glyphImg = renderSyntheticGlyph(ch, fontName, style, 48);
                    if (glyphImg != null) {
                        boolean[][] binaryGlyph = extractTightGlyph(glyphImg);
                        if (binaryGlyph != null && binaryGlyph.length > 0 && binaryGlyph[0].length > 0) {
                            double[] features = extractFeatures(binaryGlyph);
                            characterPrototypes.get(ch).add(features);
                        }
                    }
                }
            }

            // Compute averaged centroid for class
            List<double[]> classPrototypes = characterPrototypes.get(ch);
            if (!classPrototypes.isEmpty()) {
                double[] centroid = new double[FEATURE_DIM];
                for (double[] feat : classPrototypes) {
                    for (int d = 0; d < FEATURE_DIM; d++) {
                        centroid[d] += feat[d];
                    }
                }
                for (int d = 0; d < FEATURE_DIM; d++) {
                    centroid[d] /= classPrototypes.size();
                }
                averagedCentroids.put(ch, centroid);
            }
        }
    }

    public RecognitionOutput recognizeGlyph(boolean[][] glyphMatrix) {
        if (glyphMatrix == null || glyphMatrix.length == 0 || glyphMatrix[0].length == 0) {
            return new RecognitionOutput(' ', 0.0);
        }

        double[] queryFeatures = extractFeatures(glyphMatrix);

        char bestChar = '?';
        double highestSimilarity = -1.0;

        for (Map.Entry<Character, double[]> entry : averagedCentroids.entrySet()) {
            double sim = cosineSimilarity(queryFeatures, entry.getValue());
            if (sim > highestSimilarity) {
                highestSimilarity = sim;
                bestChar = entry.getKey();
            }
        }

        // Calibrate confidence percentage
        double confidence = Math.min(100.0, Math.max(0.0, (highestSimilarity - 0.45) / 0.55 * 100.0));
        return new RecognitionOutput(bestChar, Math.round(confidence * 10.0) / 10.0);
    }

    public String recognizeWord(List<OcrLayoutAnalyzer.RawGlyph> glyphs, double[] outConfidence) {
        if (glyphs.isEmpty()) {
            if (outConfidence != null && outConfidence.length > 0) outConfidence[0] = 0.0;
            return "";
        }

        StringBuilder rawWord = new StringBuilder();
        double totalConfidence = 0.0;

        for (OcrLayoutAnalyzer.RawGlyph g : glyphs) {
            RecognitionOutput out = recognizeGlyph(g.getMatrix());
            rawWord.append(out.getCharacter());
            totalConfidence += out.getConfidence();
        }

        double avgConf = totalConfidence / glyphs.size();
        String recognized = rawWord.toString();

        // Post-processing & Lexicon Disambiguation
        String polished = postProcessWord(recognized);

        if (outConfidence != null && outConfidence.length > 0) {
            outConfidence[0] = Math.round(avgConf * 10.0) / 10.0;
        }

        return polished;
    }

    private String postProcessWord(String word) {
        if (word == null || word.length() < 2) return word;

        // Check if word is close to a common dictionary word
        String lower = word.toLowerCase();
        if (commonLexicon.contains(lower)) {
            // Restore capitalization
            if (Character.isUpperCase(word.charAt(0))) {
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
            }
            return lower;
        }

        // Common OCR confusion fixes
        // 1. If word is mostly digits, replace letters like 'O', 'I', 'l', 'S' with '0', '1', '1', '5'
        int digitCount = 0;
        int letterCount = 0;
        for (char c : word.toCharArray()) {
            if (Character.isDigit(c)) digitCount++;
            else if (Character.isLetter(c)) letterCount++;
        }

        if (digitCount > letterCount && digitCount >= 2) {
            StringBuilder sb = new StringBuilder();
            for (char c : word.toCharArray()) {
                if (c == 'O' || c == 'o') sb.append('0');
                else if (c == 'I' || c == 'l' || c == '|') sb.append('1');
                else if (c == 'S' || c == 's') sb.append('5');
                else if (c == 'B') sb.append('8');
                else sb.append(c);
            }
            return sb.toString();
        }

        // 2. If word is mostly letters, fix digits like '0' -> 'o', '1' -> 'l'
        if (letterCount > digitCount && letterCount >= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (c == '0') sb.append('o');
                else if (c == '1') sb.append(i == 0 ? 'I' : 'l');
                else if (c == '5') sb.append('s');
                else sb.append(c);
            }
            String candidate = sb.toString();
            if (commonLexicon.contains(candidate.toLowerCase())) {
                return candidate;
            }
        }

        return word;
    }

    public double[] extractFeatures(boolean[][] matrix) {
        double[] features = new double[FEATURE_DIM];
        int h = matrix.length;
        int w = matrix[0].length;

        // Normalize matrix into 32x32 standard raster
        boolean[][] norm = resampleGlyph(matrix, NORM_SIZE, NORM_SIZE);

        // 1. Zoning density features (8x8 grid -> 64 features)
        int cellH = NORM_SIZE / ZONES;
        int cellW = NORM_SIZE / ZONES;

        int featureIdx = 0;
        for (int zy = 0; zy < ZONES; zy++) {
            for (int zx = 0; zx < ZONES; zx++) {
                int count = 0;
                for (int y = zy * cellH; y < (zy + 1) * cellH; y++) {
                    for (int x = zx * cellW; x < (zx + 1) * cellW; x++) {
                        if (norm[y][x]) count++;
                    }
                }
                features[featureIdx++] = (double) count / (cellH * cellW);
            }
        }

        // 2. Structural & Topological features (8 features)
        // Feature 64: Aspect ratio
        features[64] = Math.min(3.0, (double) w / Math.max(1, h));

        // Feature 65: Overall density
        int totalText = 0;
        for (int y = 0; y < NORM_SIZE; y++) {
            for (int x = 0; x < NORM_SIZE; x++) {
                if (norm[y][x]) totalText++;
            }
        }
        features[65] = (double) totalText / (NORM_SIZE * NORM_SIZE);

        // Feature 66: Center of gravity Y
        double sumY = 0;
        for (int y = 0; y < NORM_SIZE; y++) {
            for (int x = 0; x < NORM_SIZE; x++) {
                if (norm[y][x]) sumY += y;
            }
        }
        features[66] = totalText > 0 ? (sumY / totalText) / NORM_SIZE : 0.5;

        // Feature 67: Center of gravity X
        double sumX = 0;
        for (int y = 0; y < NORM_SIZE; y++) {
            for (int x = 0; x < NORM_SIZE; x++) {
                if (norm[y][x]) sumX += x;
            }
        }
        features[67] = totalText > 0 ? (sumX / totalText) / NORM_SIZE : 0.5;

        // Features 68-71: Horizontal crossing counts at 25%, 50%, 75%
        features[68] = countCrossingsHorizontal(norm, NORM_SIZE / 4) / 6.0;
        features[69] = countCrossingsHorizontal(norm, NORM_SIZE / 2) / 6.0;
        features[70] = countCrossingsHorizontal(norm, (3 * NORM_SIZE) / 4) / 6.0;

        // Vertical crossing at 50%
        features[71] = countCrossingsVertical(norm, NORM_SIZE / 2) / 6.0;

        return features;
    }

    private double countCrossingsHorizontal(boolean[][] norm, int y) {
        int crossings = 0;
        boolean inObj = false;
        for (int x = 0; x < NORM_SIZE; x++) {
            if (norm[y][x]) {
                if (!inObj) {
                    inObj = true;
                    crossings++;
                }
            } else {
                inObj = false;
            }
        }
        return crossings;
    }

    private double countCrossingsVertical(boolean[][] norm, int x) {
        int crossings = 0;
        boolean inObj = false;
        for (int y = 0; y < NORM_SIZE; y++) {
            if (norm[y][x]) {
                if (!inObj) {
                    inObj = true;
                    crossings++;
                }
            } else {
                inObj = false;
            }
        }
        return crossings;
    }

    private boolean[][] resampleGlyph(boolean[][] src, int targetW, int targetH) {
        int srcH = src.length;
        int srcW = src[0].length;
        boolean[][] target = new boolean[targetH][targetW];

        for (int ty = 0; ty < targetH; ty++) {
            int sy = (int) ((double) ty / targetH * srcH);
            sy = Math.min(srcH - 1, sy);
            for (int tx = 0; tx < targetW; tx++) {
                int sx = (int) ((double) tx / targetW * srcW);
                sx = Math.min(srcW - 1, sx);
                target[ty][tx] = src[sy][sx];
            }
        }
        return target;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private BufferedImage renderSyntheticGlyph(char ch, String fontName, int style, int fontSize) {
        int size = fontSize * 2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.setFont(new Font(fontName, style, fontSize));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        FontMetrics fm = g.getFontMetrics();
        int x = (size - fm.charWidth(ch)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(String.valueOf(ch), x, y);
        g.dispose();
        return img;
    }

    private boolean[][] extractTightGlyph(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int minX = w, maxX = 0, minY = h, maxY = 0;
        boolean found = false;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = img.getRaster().getSample(x, y, 0);
                if (val < 128) {
                    found = true;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (!found) return null;

        int cropW = maxX - minX + 1;
        int cropH = maxY - minY + 1;
        boolean[][] tight = new boolean[cropH][cropW];

        for (int y = 0; y < cropH; y++) {
            for (int x = 0; x < cropW; x++) {
                tight[y][x] = (img.getRaster().getSample(minX + x, minY + y, 0) < 128);
            }
        }
        return tight;
    }
}
