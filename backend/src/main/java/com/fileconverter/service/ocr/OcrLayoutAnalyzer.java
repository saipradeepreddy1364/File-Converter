package com.fileconverter.service.ocr;

import com.fileconverter.model.BoundingBox;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Custom In-House Document Layout Analysis & Text Region Detection Pipeline:
 * 1. Horizontal Projection Profile Line Segmentation
 * 2. Vertical Projection Profile Word & Glyph Segmentation
 * 3. Connected Component Analysis (CCA)
 * 4. Reading Order Reconstruction (Top-to-Bottom, Left-to-Right)
 * 5. Paragraph Grouping based on Line Height and Gap Distribution
 */
@Component
public class OcrLayoutAnalyzer {

    public static class RawGlyph {
        private final BoundingBox box;
        private final boolean[][] matrix;

        public RawGlyph(BoundingBox box, boolean[][] matrix) {
            this.box = box;
            this.matrix = matrix;
        }

        public BoundingBox getBox() { return box; }
        public boolean[][] getMatrix() { return matrix; }
    }

    public static class RawWord {
        private final BoundingBox box;
        private final List<RawGlyph> glyphs;

        public RawWord(BoundingBox box, List<RawGlyph> glyphs) {
            this.box = box;
            this.glyphs = glyphs;
        }

        public BoundingBox getBox() { return box; }
        public List<RawGlyph> getGlyphs() { return glyphs; }
    }

    public static class RawLine {
        private final BoundingBox box;
        private final List<RawWord> words;
        private boolean isNewParagraph;

        public RawLine(BoundingBox box, List<RawWord> words) {
            this.box = box;
            this.words = words;
        }

        public BoundingBox getBox() { return box; }
        public List<RawWord> getWords() { return words; }
        public boolean isNewParagraph() { return isNewParagraph; }
        public void setNewParagraph(boolean newParagraph) { isNewParagraph = newParagraph; }
    }

    public List<RawLine> analyzeLayout(boolean[][] binary) {
        int height = binary.length;
        int width = binary[0].length;

        // 1. Calculate Horizontal Projection Profile (count of text pixels per row)
        int[] hProfile = new int[height];
        for (int y = 0; y < height; y++) {
            int count = 0;
            for (int x = 0; x < width; x++) {
                if (binary[y][x]) count++;
            }
            hProfile[y] = count;
        }

        // 2. Segment rows into text lines
        List<int[]> lineBands = findLineBands(hProfile, width);

        // 3. For each line band, segment words via vertical projection
        List<RawLine> detectedLines = new ArrayList<>();
        int lastLineBottom = 0;
        int averageLineHeight = 16;
        if (!lineBands.isEmpty()) {
            int totalH = 0;
            for (int[] b : lineBands) totalH += (b[1] - b[0]);
            averageLineHeight = Math.max(12, totalH / lineBands.size());
        }

        for (int[] band : lineBands) {
            int top = band[0];
            int bottom = band[1];
            int lineH = bottom - top;

            // Trim left/right empty whitespace for line
            int lineLeft = width;
            int lineRight = 0;
            for (int y = top; y < bottom; y++) {
                for (int x = 0; x < width; x++) {
                    if (binary[y][x]) {
                        if (x < lineLeft) lineLeft = x;
                        if (x > lineRight) lineRight = x;
                    }
                }
            }

            if (lineRight <= lineLeft) continue; // empty band

            // Segment words and characters within this horizontal band
            List<RawWord> words = segmentWordsInBand(binary, top, bottom, lineLeft, lineRight, averageLineHeight);
            if (!words.isEmpty()) {
                BoundingBox lineBox = new BoundingBox(lineLeft, top, lineRight - lineLeft + 1, lineH);
                RawLine rawLine = new RawLine(lineBox, words);

                // Paragraph detection: if vertical gap from previous line is > 1.6x average line height, mark new paragraph
                if (detectedLines.isEmpty() || (top - lastLineBottom) > (averageLineHeight * 1.5)) {
                    rawLine.setNewParagraph(true);
                }
                lastLineBottom = bottom;
                detectedLines.add(rawLine);
            }
        }

        // Strict reading order sort (top to bottom)
        detectedLines.sort(Comparator.comparingInt(l -> l.getBox().getY()));
        return detectedLines;
    }

    private List<int[]> findLineBands(int[] hProfile, int width) {
        List<int[]> bands = new ArrayList<>();
        int noiseThreshold = Math.max(2, width / 400); // filter isolated noise speckles
        boolean inLine = false;
        int startY = 0;

        for (int y = 0; y < hProfile.length; y++) {
            if (hProfile[y] > noiseThreshold) {
                if (!inLine) {
                    inLine = true;
                    startY = y;
                }
            } else {
                if (inLine) {
                    inLine = false;
                    int lineH = y - startY;
                    if (lineH >= 6) { // Minimum height of a readable character line
                        bands.add(new int[]{startY, y});
                    }
                }
            }
        }
        if (inLine && (hProfile.length - startY) >= 6) {
            bands.add(new int[]{startY, hProfile.length});
        }
        return bands;
    }

    private List<RawWord> segmentWordsInBand(boolean[][] binary, int top, int bottom, int left, int right, int avgHeight) {
        int bandW = right - left + 1;
        int[] vProfile = new int[bandW];

        for (int x = 0; x < bandW; x++) {
            int actualX = left + x;
            int count = 0;
            for (int y = top; y < bottom; y++) {
                if (binary[y][actualX]) count++;
            }
            vProfile[x] = count;
        }

        // Detect glyph bounding boxes
        List<BoundingBox> glyphBoxes = new ArrayList<>();
        boolean inGlyph = false;
        int glyphStartX = 0;

        for (int x = 0; x < bandW; x++) {
            if (vProfile[x] > 0) {
                if (!inGlyph) {
                    inGlyph = true;
                    glyphStartX = x;
                }
            } else {
                if (inGlyph) {
                    inGlyph = false;
                    int glyphW = x - glyphStartX;
                    if (glyphW >= 2) {
                        // Tight vertical bounds for glyph
                        int actualLeft = left + glyphStartX;
                        int gTop = bottom;
                        int gBottom = top;
                        for (int gy = top; gy < bottom; gy++) {
                            for (int gx = 0; gx < glyphW; gx++) {
                                if (binary[gy][actualLeft + gx]) {
                                    if (gy < gTop) gTop = gy;
                                    if (gy > gBottom) gBottom = gy;
                                }
                            }
                        }
                        if (gBottom >= gTop) {
                            glyphBoxes.add(new BoundingBox(actualLeft, gTop, glyphW, gBottom - gTop + 1));
                        }
                    }
                }
            }
        }

        if (glyphBoxes.isEmpty()) return Collections.emptyList();

        // Group glyphs into words based on horizontal space gaps
        int spaceThreshold = Math.max(5, avgHeight / 3);
        List<RawWord> words = new ArrayList<>();
        List<RawGlyph> currentWordGlyphs = new ArrayList<>();

        for (int i = 0; i < glyphBoxes.size(); i++) {
            BoundingBox box = glyphBoxes.get(i);
            boolean[][] glyphMatrix = cropMatrix(binary, box);
            currentWordGlyphs.add(new RawGlyph(box, glyphMatrix));

            boolean endOfWord = false;
            if (i == glyphBoxes.size() - 1) {
                endOfWord = true;
            } else {
                BoundingBox nextBox = glyphBoxes.get(i + 1);
                int gap = nextBox.getX() - (box.getX() + box.getWidth());
                if (gap >= spaceThreshold) {
                    endOfWord = true;
                }
            }

            if (endOfWord && !currentWordGlyphs.isEmpty()) {
                int wLeft = currentWordGlyphs.get(0).getBox().getX();
                int wRight = currentWordGlyphs.get(currentWordGlyphs.size() - 1).getBox().getX() +
                        currentWordGlyphs.get(currentWordGlyphs.size() - 1).getBox().getWidth();
                int wTop = currentWordGlyphs.stream().mapToInt(g -> g.getBox().getY()).min().orElse(top);
                int wBottom = currentWordGlyphs.stream().mapToInt(g -> g.getBox().getY() + g.getBox().getHeight()).max().orElse(bottom);

                BoundingBox wordBox = new BoundingBox(wLeft, wTop, wRight - wLeft, wBottom - wTop);
                words.add(new RawWord(wordBox, new ArrayList<>(currentWordGlyphs)));
                currentWordGlyphs.clear();
            }
        }

        return words;
    }

    private boolean[][] cropMatrix(boolean[][] binary, BoundingBox box) {
        boolean[][] crop = new boolean[box.getHeight()][box.getWidth()];
        for (int y = 0; y < box.getHeight(); y++) {
            int srcY = box.getY() + y;
            for (int x = 0; x < box.getWidth(); x++) {
                int srcX = box.getX() + x;
                crop[y][x] = binary[srcY][srcX];
            }
        }
        return crop;
    }
}
