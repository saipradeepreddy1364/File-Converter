package com.fileconverter.service.ocr;

import com.fileconverter.model.BoundingBox;
import com.fileconverter.model.OcrLineResult;
import com.fileconverter.model.OcrPageResult;
import com.fileconverter.model.OcrResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service to generate genuine Searchable PDFs:
 * Overlays an invisible, aligned text layer onto scanned pages.
 * Supports Ctrl+F search, text selection, and copying in all PDF readers.
 */
@Service
public class SearchablePdfService {

    private static final Logger log = LoggerFactory.getLogger(SearchablePdfService.class);

    public void createSearchablePdf(List<BufferedImage> pageImages, OcrResult ocrResult, File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (int i = 0; i < pageImages.size(); i++) {
                BufferedImage pageImage = pageImages.get(i);
                int imgW = pageImage.getWidth();
                int imgH = pageImage.getHeight();

                PDRectangle pageSize = new PDRectangle(imgW, imgH);
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                // 1. Draw original scanned background image
                PDImageXObject pdImage = JPEGFactory.createFromImage(document, pageImage, 0.85f);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, false)) {
                    contentStream.drawImage(pdImage, 0, 0, imgW, imgH);

                    // 2. Add invisible text layer if OCR page result exists
                    if (ocrResult != null && i < ocrResult.getPages().size()) {
                        OcrPageResult pageResult = ocrResult.getPages().get(i);
                        addInvisibleTextLayer(contentStream, pageResult, font, imgH);
                    }
                }
            }

            document.save(outputFile);
            log.info("Successfully generated searchable PDF with {} pages at {}", pageImages.size(), outputFile.getName());
        }
    }

    public void addSearchableTextLayerToPdf(File inputPdf, OcrResult ocrResult, File outputFile) throws IOException {
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(inputPdf)) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                if (ocrResult != null && i < ocrResult.getPages().size()) {
                    PDPage page = doc.getPage(i);
                    float pageHeight = page.getMediaBox().getHeight();
                    OcrPageResult pageResult = ocrResult.getPages().get(i);

                    try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        addInvisibleTextLayer(cs, pageResult, font, pageHeight);
                    }
                }
            }
            doc.save(outputFile);
        }
    }

    private void addInvisibleTextLayer(PDPageContentStream cs, OcrPageResult pageResult, PDType1Font font, float pageHeight) throws IOException {
        for (OcrLineResult line : pageResult.getLines()) {
            if (line.getText() == null || line.getText().trim().isEmpty()) continue;
            BoundingBox box = line.getBoundingBox();
            if (box == null) continue;

            float x = box.getX();
            // Invert Y coordinate for PDF coordinate space (PDF origin is bottom-left)
            float y = pageHeight - box.getY() - box.getHeight();
            float fontSize = Math.max(8.0f, box.getHeight() * 0.75f);

            // Clean string of characters unsupported by standard PDF Type1 font
            String cleanText = sanitizeForPdf(line.getText());
            if (cleanText.isEmpty()) continue;

            try {
                cs.beginText();
                // Set text rendering mode to 3 (Neither fill nor stroke text = invisible selectable text)
                cs.setRenderingMode(org.apache.pdfbox.pdmodel.graphics.state.RenderingMode.NEITHER);
                cs.setFont(font, fontSize);
                cs.newLineAtOffset(x, y);
                cs.showText(cleanText);
                cs.endText();
            } catch (Exception e) {
                log.debug("Skipped unrenderable text token in searchable layer: {}", e.getMessage());
            }
        }
    }

    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (c == '\t') {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
