package com.fileconverter.service.ocr;

import com.fileconverter.model.OcrLineResult;
import com.fileconverter.model.OcrPageResult;
import com.fileconverter.model.OcrResult;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Service to convert OCR results and raw text into editable Microsoft Word (.docx) documents.
 */
@Service
public class DocxExportService {

    private static final Logger log = LoggerFactory.getLogger(DocxExportService.class);

    public void exportOcrToDocx(OcrResult ocrResult, File outputFile) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            // Document Header
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("OCR Extracted Document: " + (ocrResult.getOriginalFileName() != null ? ocrResult.getOriginalFileName() : "Document"));
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setColor("1A365D");

            // Document Metadata note
            XWPFParagraph metaPara = doc.createParagraph();
            XWPFRun metaRun = metaPara.createRun();
            metaRun.setText("Processed by In-House OCR Engine | Pages: " + ocrResult.getPageCount() +
                    " | Average Confidence: " + String.format("%.1f%%", ocrResult.getAverageConfidence()));
            metaRun.setItalic(true);
            metaRun.setFontSize(10);
            metaRun.setColor("718096");

            // Divider
            XWPFParagraph divider = doc.createParagraph();
            divider.setBorderBottom(Borders.SINGLE);

            for (OcrPageResult page : ocrResult.getPages()) {
                if (ocrResult.getPageCount() > 1) {
                    XWPFParagraph pageHeader = doc.createParagraph();
                    XWPFRun phRun = pageHeader.createRun();
                    phRun.setText("--- Page " + page.getPageNumber() + " ---");
                    phRun.setBold(true);
                    phRun.setFontSize(12);
                    phRun.setColor("4A5568");
                }

                XWPFParagraph currentPara = null;
                for (OcrLineResult line : page.getLines()) {
                    if (currentPara == null) {
                        currentPara = doc.createParagraph();
                        currentPara.setSpacingAfter(120);
                    }
                    XWPFRun lineRun = currentPara.createRun();
                    lineRun.setText(line.getText());
                    lineRun.setFontFamily("Calibri");
                    lineRun.setFontSize(11);
                    currentPara.createRun().addBreak();
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                doc.write(fos);
            }
            log.info("Saved editable DOCX document at {}", outputFile.getName());
        }
    }

    public void exportTextToDocx(String text, String documentTitle, File outputFile) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            if (documentTitle != null && !documentTitle.isEmpty()) {
                XWPFParagraph titlePara = doc.createParagraph();
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText(documentTitle);
                titleRun.setBold(true);
                titleRun.setFontSize(16);
            }

            String[] paragraphs = text.split("\r?\n\r?\n");
            for (String p : paragraphs) {
                XWPFParagraph para = doc.createParagraph();
                para.setSpacingAfter(150);
                String[] lines = p.split("\r?\n");
                for (int i = 0; i < lines.length; i++) {
                    XWPFRun run = para.createRun();
                    run.setText(lines[i]);
                    run.setFontFamily("Calibri");
                    run.setFontSize(11);
                    if (i < lines.length - 1) {
                        run.addBreak();
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                doc.write(fos);
            }
        }
    }
}
