package com.fileconverter.service.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Benchmark Evaluation Harness for OCR Engine:
 * - Computes genuine Character Error Rate (CER) via Levenshtein Distance
 * - Computes genuine Word Error Rate (WER) via Token-level Dynamic Programming
 * - Evaluates across diverse synthetic and labeled test scenarios (noise, rotations, font sizes)
 */
@Component
public class OcrEvaluator {

    private static final Logger log = LoggerFactory.getLogger(OcrEvaluator.class);

    public static class EvaluationReport {
        private final String datasetName;
        private final int sampleCount;
        private final double characterErrorRate; // CER
        private final double wordErrorRate;      // WER
        private final double characterAccuracy;  // 100 - CER
        private final double wordAccuracy;       // 100 - WER
        private final double averageConfidence;
        private final long averageLatencyMs;
        private final List<SampleResult> sampleResults;
        private final List<String> limitations;

        public EvaluationReport(String datasetName, int sampleCount, double cer, double wer,
                                double avgConf, long avgLatencyMs, List<SampleResult> samples, List<String> limitations) {
            this.datasetName = datasetName;
            this.sampleCount = sampleCount;
            this.characterErrorRate = Math.round(cer * 100.0) / 100.0;
            this.wordErrorRate = Math.round(wer * 100.0) / 100.0;
            this.characterAccuracy = Math.max(0.0, Math.round((100.0 - cer) * 100.0) / 100.0);
            this.wordAccuracy = Math.max(0.0, Math.round((100.0 - wer) * 100.0) / 100.0);
            this.averageConfidence = Math.round(avgConf * 10.0) / 10.0;
            this.averageLatencyMs = avgLatencyMs;
            this.sampleResults = samples;
            this.limitations = limitations;
        }

        public String getDatasetName() { return datasetName; }
        public int getSampleCount() { return sampleCount; }
        public double getCharacterErrorRate() { return characterErrorRate; }
        public double getWordErrorRate() { return wordErrorRate; }
        public double getCharacterAccuracy() { return characterAccuracy; }
        public double getWordAccuracy() { return wordAccuracy; }
        public double getAverageConfidence() { return averageConfidence; }
        public long getAverageLatencyMs() { return averageLatencyMs; }
        public List<SampleResult> getSampleResults() { return sampleResults; }
        public List<String> getLimitations() { return limitations; }
    }

    public static class SampleResult {
        private final String testCase;
        private final String groundTruth;
        private final String recognizedText;
        private final double cer;
        private final double wer;
        private final double confidence;
        private final long latencyMs;

        public SampleResult(String testCase, String groundTruth, String recognizedText, double cer, double wer, double confidence, long latencyMs) {
            this.testCase = testCase;
            this.groundTruth = groundTruth;
            this.recognizedText = recognizedText;
            this.cer = Math.round(cer * 100.0) / 100.0;
            this.wer = Math.round(wer * 100.0) / 100.0;
            this.confidence = Math.round(confidence * 10.0) / 10.0;
            this.latencyMs = latencyMs;
        }

        public String getTestCase() { return testCase; }
        public String getGroundTruth() { return groundTruth; }
        public String getRecognizedText() { return recognizedText; }
        public double getCer() { return cer; }
        public double getWer() { return wer; }
        public double getConfidence() { return confidence; }
        public long getLatencyMs() { return latencyMs; }
    }

    public double calculateLevenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[m][n];
    }

    public double calculateCer(String groundTruth, String hypothesis) {
        if (groundTruth == null || groundTruth.isEmpty()) {
            return (hypothesis == null || hypothesis.isEmpty()) ? 0.0 : 100.0;
        }
        String ref = groundTruth.replaceAll("\\s+", " ").trim();
        String hyp = hypothesis != null ? hypothesis.replaceAll("\\s+", " ").trim() : "";
        double distance = calculateLevenshteinDistance(ref, hyp);
        return (distance / ref.length()) * 100.0;
    }

    public double calculateWer(String groundTruth, String hypothesis) {
        String[] refWords = groundTruth != null ? groundTruth.trim().split("\\s+") : new String[0];
        String[] hypWords = hypothesis != null ? hypothesis.trim().split("\\s+") : new String[0];

        if (refWords.length == 0) {
            return hypWords.length == 0 ? 0.0 : 100.0;
        }

        int m = refWords.length;
        int n = hypWords.length;
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (refWords[i - 1].equalsIgnoreCase(hypWords[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        double wordDistance = dp[m][n];
        return (wordDistance / m) * 100.0;
    }

    public EvaluationReport evaluateEngine(CustomOcrEngine engine) {
        List<SampleResult> sampleResults = new ArrayList<>();

        // Test Benchmarks
        TestCase[] testCases = {
                new TestCase("Standard Clean Printed Document", "Invoice number 2026 and project report status", 24, 0.0, 0, false),
                new TestCase("Small Font Size (14pt)", "Small print terms and conditions for file conversion", 14, 0.0, 0, false),
                new TestCase("Rotated Document (Skewed 5 Deg)", "Rotated page text with automated deskew alignment", 22, 5.0, 0, true),
                new TestCase("Noisy Document (Scan Grain)", "Document with simulated scanner sensor noise and artifacts", 22, 0.0, 20, false),
                new TestCase("Numeric & Technical Data", "Total price $495.50 reference ID 883921 code 70", 22, 0.0, 0, false)
        };

        double totalCer = 0.0;
        double totalWer = 0.0;
        double totalConf = 0.0;
        long totalLatency = 0;

        for (TestCase tc : testCases) {
            BufferedImage img = renderBenchmarkSample(tc);
            long start = System.currentTimeMillis();
            com.fileconverter.model.OcrResult res = engine.recognizeImage(img, "eng", tc.needsDeskew, true);
            long duration = System.currentTimeMillis() - start;

            String recognized = res.getFullText().trim();
            double cer = calculateCer(tc.text, recognized);
            double wer = calculateWer(tc.text, recognized);

            totalCer += cer;
            totalWer += wer;
            totalConf += res.getAverageConfidence();
            totalLatency += duration;

            sampleResults.add(new SampleResult(tc.name, tc.text, recognized, cer, wer, res.getAverageConfidence(), duration));
        }

        int count = testCases.length;
        List<String> limitations = Arrays.asList(
                "Handwritten cursive text is not supported (engineered specifically for printed & scanned typefaces)",
                "Severely degraded or partially torn documents with fragmented glyph topology may yield higher CER",
                "Complex mathematical formulas and non-Latin character sets (e.g. CJK, Arabic) require dedicated script models",
                "Heavily skewed scans exceeding 45 degrees should be manually oriented prior to processing"
        );

        return new EvaluationReport(
                "In-House Labeled Benchmark Suite (Multi-Font, Noise, Skew)",
                count,
                totalCer / count,
                totalWer / count,
                totalConf / count,
                totalLatency / count,
                sampleResults,
                limitations
        );
    }

    private static class TestCase {
        String name;
        String text;
        int fontSize;
        double rotationAngle;
        int noiseIntensity;
        boolean needsDeskew;

        TestCase(String name, String text, int fontSize, double rotationAngle, int noiseIntensity, boolean needsDeskew) {
            this.name = name;
            this.text = text;
            this.fontSize = fontSize;
            this.rotationAngle = rotationAngle;
            this.noiseIntensity = noiseIntensity;
            this.needsDeskew = needsDeskew;
        }
    }

    private BufferedImage renderBenchmarkSample(TestCase tc) {
        int w = 900;
        int h = 180;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, tc.fontSize));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics fm = g.getFontMetrics();
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(tc.text, 40, y);
        g.dispose();

        // Add Noise if configured
        if (tc.noiseIntensity > 0) {
            Random rng = new Random(42);
            for (int ny = 0; ny < h; ny++) {
                for (int nx = 0; nx < w; nx++) {
                    if (rng.nextInt(100) < tc.noiseIntensity) {
                        int noiseGray = rng.nextInt(100);
                        img.setRGB(nx, ny, (noiseGray << 16) | (noiseGray << 8) | noiseGray);
                    }
                }
            }
        }

        // Add rotation skew if configured
        if (Math.abs(tc.rotationAngle) > 0.1) {
            BufferedImage rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D rg = rotated.createGraphics();
            rg.setColor(Color.WHITE);
            rg.fillRect(0, 0, w, h);
            rg.rotate(Math.toRadians(tc.rotationAngle), w / 2.0, h / 2.0);
            rg.drawImage(img, 0, 0, null);
            rg.dispose();
            return rotated;
        }

        return img;
    }
}
