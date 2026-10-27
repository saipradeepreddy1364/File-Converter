package com.fileconverter.service.conversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileconverter.model.OcrResult;
import com.fileconverter.service.ocr.CustomOcrEngine;
import com.fileconverter.service.ocr.DocxExportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ConvertFromPdfService {

    private static final Logger log = LoggerFactory.getLogger(ConvertFromPdfService.class);

    private final CustomOcrEngine ocrEngine;
    private final DocxExportService docxExportService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ConvertFromPdfService(CustomOcrEngine ocrEngine, DocxExportService docxExportService) {
        this.ocrEngine = ocrEngine;
        this.docxExportService = docxExportService;
    }

    public void pdfToImages(File inputFile, String format, int dpi, File outputZipOrImage) throws IOException {
        String fmt = format != null ? format.toLowerCase() : "png";
        int resolution = dpi > 0 ? dpi : 200;

        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int total = doc.getNumberOfPages();

            if (total == 1 && !outputZipOrImage.getName().endsWith(".zip")) {
                BufferedImage bImg = renderer.renderImageWithDPI(0, resolution, ImageType.RGB);
                ImageIO.write(bImg, fmt, outputZipOrImage);
            } else {
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZipOrImage))) {
                    for (int i = 0; i < total; i++) {
                        BufferedImage bImg = renderer.renderImageWithDPI(i, resolution, ImageType.RGB);
                        ZipEntry entry = new ZipEntry("page_" + (i + 1) + "." + fmt);
                        zos.putNextEntry(entry);
                        ImageIO.write(bImg, fmt, zos);
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    public void pdfToText(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // If empty (scanned PDF), invoke In-House OCR
            if (text == null || text.trim().length() < 20) {
                log.info("Detected scanned PDF with no embedded text layer. Invoking in-house OCR engine.");
                OcrResult ocrResult = ocrEngine.recognizePdf(inputFile, "eng", true, true);
                text = ocrResult.getFullText();
            }

            java.nio.file.Files.writeString(outputFile.toPath(), text, StandardCharsets.UTF_8);
        }
    }

    public void pdfToDocx(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // If scanned document, run in-house OCR pipeline
            if (text == null || text.trim().length() < 20) {
                log.info("Scanned PDF detected during Word export. Invoking OCR pipeline.");
                OcrResult ocrResult = ocrEngine.recognizePdf(inputFile, "eng", true, true);
                docxExportService.exportOcrToDocx(ocrResult, outputFile);
            } else {
                docxExportService.exportTextToDocx(text, inputFile.getName(), outputFile);
            }
        }
    }

    public void pdfToMarkdown(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            if (text == null || text.trim().length() < 20) {
                OcrResult ocr = ocrEngine.recognizePdf(inputFile, "eng", true, true);
                text = ocr.getFullText();
            }

            StringBuilder md = new StringBuilder();
            md.append("# ").append(inputFile.getName().replace(".pdf", "")).append("\n\n");

            String[] paragraphs = text.split("\r?\n\r?\n");
            for (String p : paragraphs) {
                String trimmed = p.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.length() < 60 && !trimmed.endsWith(".")) {
                    md.append("## ").append(trimmed).append("\n\n");
                } else {
                    md.append(trimmed).append("\n\n");
                }
            }

            java.nio.file.Files.writeString(outputFile.toPath(), md.toString(), StandardCharsets.UTF_8);
        }
    }

    public void pdfToHtml(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            if (text == null || text.trim().length() < 20) {
                OcrResult ocr = ocrEngine.recognizePdf(inputFile, "eng", true, true);
                text = ocr.getFullText();
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>")
                    .append(inputFile.getName()).append("</title>")
                    .append("<style>body{font-family:Arial,sans-serif;max-width:800px;margin:40px auto;line-height:1.6;color:#333;padding:0 20px;}h1{color:#1a365d;}p{margin-bottom:1em;}</style>")
                    .append("</head><body><h1>").append(inputFile.getName()).append("</h1>");

            String[] paragraphs = text.split("\r?\n\r?\n");
            for (String p : paragraphs) {
                String clean = p.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
                html.append("<p>").append(clean).append("</p>");
            }
            html.append("</body></html>");

            java.nio.file.Files.writeString(outputFile.toPath(), html.toString(), StandardCharsets.UTF_8);
        }
    }

    public void extractTablesToCsv(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile);
             FileWriter out = new FileWriter(outputFile, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            String[] lines = text.split("\r?\n");

            for (String line : lines) {
                // Heuristic: identify tab, comma, or multi-space separated columns
                String[] tokens = line.trim().split("(\\t|,|\\s{2,})");
                if (tokens.length >= 2) {
                    printer.printRecord((Object[]) tokens);
                }
            }
        }
    }

    public void extractTablesToXlsx(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile);
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Extracted Data");
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            String[] lines = text.split("\r?\n");

            int rowIdx = 0;
            for (String line : lines) {
                String[] tokens = line.trim().split("(\\t|,|\\s{2,})");
                if (tokens.length >= 2) {
                    Row row = sheet.createRow(rowIdx++);
                    for (int c = 0; c < tokens.length; c++) {
                        row.createCell(c).setCellValue(tokens[c].trim());
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
}
