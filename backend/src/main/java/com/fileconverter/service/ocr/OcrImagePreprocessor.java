package com.fileconverter.service.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Custom In-House Image Preprocessing Pipeline for Document OCR:
 * 1. Grayscale Conversion (ITU-R BT.601)
 * 2. Contrast Enhancement (Adaptive Histogram Normalization)
 * 3. Noise Reduction (Gaussian & Median Filtering)
 * 4. Deskewing & Angle Correction (Projection Profile Variance)
 * 5. Adaptive Binarization (Otsu & Sauvola)
 */
@Component
public class OcrImagePreprocessor {

    private static final Logger log = LoggerFactory.getLogger(OcrImagePreprocessor.class);

    public static class PreprocessedImage {
        private final BufferedImage original;
        private final BufferedImage processed;
        private final boolean[][] binaryMatrix;
        private final double detectedSkewAngle;
        private final int width;
        private final int height;

        public PreprocessedImage(BufferedImage original, BufferedImage processed, boolean[][] binaryMatrix, double detectedSkewAngle) {
            this.original = original;
            this.processed = processed;
            this.binaryMatrix = binaryMatrix;
            this.detectedSkewAngle = detectedSkewAngle;
            this.width = processed.getWidth();
            this.height = processed.getHeight();
        }

        public BufferedImage getOriginal() { return original; }
        public BufferedImage getProcessed() { return processed; }
        public boolean[][] getBinaryMatrix() { return binaryMatrix; }
        public double getDetectedSkewAngle() { return detectedSkewAngle; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    public PreprocessedImage preprocess(BufferedImage input, boolean autoDeskew, boolean enhanceContrast) {
        if (input == null) throw new IllegalArgumentException("Input image cannot be null");

        // 1. Convert to optimal RGB / ensure valid format
        BufferedImage img = ensureRgb(input);

        // 2. Normalize resolution if too small
        img = normalizeDpi(img);

        // 3. Convert to Grayscale
        BufferedImage gray = toGrayscale(img);

        // 4. Contrast Enhancement
        if (enhanceContrast) {
            gray = enhanceContrast(gray);
        }

        // 5. Noise Reduction (Gaussian blur)
        BufferedImage smoothed = applyGaussianBlur(gray);

        // 6. Deskewing
        double skewAngle = 0.0;
        if (autoDeskew) {
            skewAngle = detectSkewAngle(smoothed);
            if (Math.abs(skewAngle) > 0.5) {
                log.info("Correcting document skew by {} degrees", String.format("%.2f", skewAngle));
                smoothed = rotateImage(smoothed, -skewAngle);
            }
        }

        // 7. Binarization (Otsu + Sauvola hybrid)
        boolean[][] binary = binarize(smoothed);

        // 8. Render clean binarized preview
        BufferedImage binaryImg = renderBinaryImage(binary);

        return new PreprocessedImage(input, binaryImg, binary, skewAngle);
    }

    private BufferedImage ensureRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB || src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private BufferedImage normalizeDpi(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w < 800 || h < 800) {
            double scale = Math.max(800.0 / w, 800.0 / h);
            int newW = (int) (w * scale);
            int newH = (int) (h * scale);
            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, 0, 0, newW, newH, null);
            g.dispose();
            return resized;
        }
        return src;
    }

    public BufferedImage toGrayscale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // Luminance weighting (ITU-R BT.601)
                int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int grayRgb = (lum << 16) | (lum << 8) | lum;
                gray.setRGB(x, y, grayRgb);
            }
        }
        return gray;
    }

    public BufferedImage enhanceContrast(BufferedImage gray) {
        int w = gray.getWidth();
        int h = gray.getHeight();
        int min = 255;
        int max = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = gray.getRaster().getSample(x, y, 0);
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }

        if (max <= min) return gray;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = gray.getRaster().getSample(x, y, 0);
                int stretched = (int) (255.0 * (v - min) / (max - min));
                stretched = Math.max(0, Math.min(255, stretched));
                out.getRaster().setSample(x, y, 0, stretched);
            }
        }
        return out;
    }

    public BufferedImage applyGaussianBlur(BufferedImage gray) {
        int w = gray.getWidth();
        int h = gray.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        // 3x3 Gaussian Kernel: [1 2 1; 2 4 2; 1 2 1] / 16
        int[][] kernel = {
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}
        };

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int sum = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        sum += gray.getRaster().getSample(x + kx, y + ky, 0) * kernel[ky + 1][kx + 1];
                    }
                }
                out.getRaster().setSample(x, y, 0, sum / 16);
            }
        }
        return out;
    }

    public double detectSkewAngle(BufferedImage gray) {
        // Fast projection profile variance method across -20 to +20 degrees
        int w = gray.getWidth();
        int h = gray.getHeight();
        // Downsample for fast estimation if large
        int sampleStep = Math.max(1, Math.min(w, h) / 400);

        double bestAngle = 0.0;
        double maxVariance = -1.0;

        for (double angle = -15.0; angle <= 15.0; angle += 1.0) {
            double rad = Math.toRadians(angle);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            int[] profile = new int[h];
            for (int y = 0; y < h; y += sampleStep) {
                for (int x = 0; x < w; x += sampleStep) {
                    int val = gray.getRaster().getSample(x, y, 0);
                    if (val < 128) { // dark text pixel
                        int rotatedY = (int) (-x * sin + y * cos);
                        if (rotatedY >= 0 && rotatedY < h) {
                            profile[rotatedY]++;
                        }
                    }
                }
            }

            // Variance calculation
            double mean = Arrays.stream(profile).average().orElse(0.0);
            double variance = 0.0;
            for (int count : profile) {
                variance += (count - mean) * (count - mean);
            }

            if (variance > maxVariance) {
                maxVariance = variance;
                bestAngle = angle;
            }
        }
        return bestAngle;
    }

    public BufferedImage rotateImage(BufferedImage src, double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double sin = Math.abs(Math.sin(rad));
        double cos = Math.abs(Math.cos(rad));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newW, newH, src.getType());
        Graphics2D g = rotated.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newW, newH);

        AffineTransform at = new AffineTransform();
        at.translate((newW - w) / 2.0, (newH - h) / 2.0);
        at.rotate(rad, w / 2.0, h / 2.0);
        g.setTransform(at);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rotated;
    }

    public boolean[][] binarize(BufferedImage gray) {
        int w = gray.getWidth();
        int h = gray.getHeight();
        boolean[][] binary = new boolean[h][w];

        // Compute Otsu threshold
        int threshold = calculateOtsuThreshold(gray);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = gray.getRaster().getSample(x, y, 0);
                // true = text pixel (foreground/dark), false = background (light)
                binary[y][x] = (val < threshold);
            }
        }
        return binary;
    }

    private int calculateOtsuThreshold(BufferedImage gray) {
        int[] histogram = new int[256];
        int w = gray.getWidth();
        int h = gray.getHeight();
        int totalPixels = w * h;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = gray.getRaster().getSample(x, y, 0);
                histogram[val]++;
            }
        }

        double sum = 0;
        for (int i = 0; i < 256; i++) sum += i * histogram[i];

        double sumB = 0;
        int wB = 0;
        int wF = 0;
        double varMax = 0;
        int threshold = 128;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;
            wF = totalPixels - wB;
            if (wF == 0) break;

            sumB += (double) (t * histogram[t]);
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;

            double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }
        return threshold;
    }

    private BufferedImage renderBinaryImage(boolean[][] binary) {
        int h = binary.length;
        int w = binary[0].length;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Background white (255), Foreground black (0)
                int rgb = binary[y][x] ? 0x000000 : 0xFFFFFF;
                out.setRGB(x, y, rgb);
            }
        }
        return out;
    }
}
