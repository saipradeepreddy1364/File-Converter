package com.fileconverter.service.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class PdfOptimizeService {

    private static final Logger log = LoggerFactory.getLogger(PdfOptimizeService.class);

    public static class CompressionResult {
        private final long originalSize;
        private final long compressedSize;
        private final double savedPercentage;

        public CompressionResult(long originalSize, long compressedSize) {
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            double saved = originalSize > 0 ? (100.0 * (originalSize - compressedSize) / originalSize) : 0.0;
            this.savedPercentage = Math.round(saved * 100.0) / 100.0;
        }

        public long getOriginalSize() { return originalSize; }
        public long getCompressedSize() { return compressedSize; }
        public double getSavedPercentage() { return savedPercentage; }
    }

    public CompressionResult compressPdf(File inputFile, String compressionLevel, File outputFile) throws IOException {
        long originalSize = inputFile.length();

        float jpegQuality;
        double maxDimension;
        String level = compressionLevel != null ? compressionLevel.toLowerCase() : "medium";

        switch (level) {
            case "high":
                jpegQuality = 0.45f;
                maxDimension = 1200;
                break;
            case "low":
                jpegQuality = 0.85f;
                maxDimension = 2400;
                break;
            case "medium":
            default:
                jpegQuality = 0.65f;
                maxDimension = 1800;
                break;
        }

        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            // Compress embedded images
            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                if (resources != null) {
                    for (COSName name : resources.getXObjectNames()) {
                        if (resources.isImageXObject(name)) {
                            try {
                                PDImageXObject currentImg = (PDImageXObject) resources.getXObject(name);
                                BufferedImage bImg = currentImg.getImage();
                                if (bImg != null) {
                                    BufferedImage resized = scaleDownImage(bImg, maxDimension);
                                    PDImageXObject compressedXObject = JPEGFactory.createFromImage(doc, resized, jpegQuality);
                                    resources.put(name, compressedXObject);
                                }
                            } catch (Exception ex) {
                                log.debug("Skipped non-standard embedded image during compression: {}", ex.getMessage());
                            }
                        }
                    }
                }
            }

            // Remove non-essential metadata if high compression
            if ("high".equals(level)) {
                doc.getDocumentInformation().setCustomMetadataValue("Keywords", null);
                doc.getDocumentCatalog().setMetadata(null);
            }

            doc.save(outputFile);
        }

        long compressedSize = outputFile.length();
        // If the resulting file is unexpectedly larger, use original
        if (compressedSize >= originalSize) {
            java.nio.file.Files.copy(inputFile.toPath(), outputFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            compressedSize = originalSize;
        }

        return new CompressionResult(originalSize, compressedSize);
    }

    public void flattenPdf(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                acroForm.flatten();
            }
            for (PDPage page : doc.getPages()) {
                page.getAnnotations().clear();
            }
            doc.save(outputFile);
        }
    }

    public void repairPdf(File inputFile, File outputFile) throws IOException {
        // PDFBox's Loader automatically scans xref table, repairs trailer dictionaries,
        // and rebuilds broken cross-reference streams.
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            doc.save(outputFile);
            log.info("Rebuilt and repaired PDF document structure at {}", outputFile.getName());
        }
    }

    private BufferedImage scaleDownImage(BufferedImage src, double maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxDim && h <= maxDim) {
            return src;
        }

        double ratio = Math.min(maxDim / w, maxDim / h);
        int newW = (int) (w * ratio);
        int newH = (int) (h * ratio);

        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return scaled;
    }
}
