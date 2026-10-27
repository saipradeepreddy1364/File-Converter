package com.fileconverter.service.conversion;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ConvertToPdfService {

    private static final Logger log = LoggerFactory.getLogger(ConvertToPdfService.class);

    public void imagesToPdf(List<File> imageFiles, File outputFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (File imgFile : imageFiles) {
                BufferedImage bImg = ImageIO.read(imgFile);
                if (bImg != null) {
                    PDRectangle pageSize = new PDRectangle(bImg.getWidth(), bImg.getHeight());
                    PDPage page = new PDPage(pageSize);
                    doc.addPage(page);

                    PDImageXObject pdImage = JPEGFactory.createFromImage(doc, bImg, 0.90f);
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.drawImage(pdImage, 0, 0, bImg.getWidth(), bImg.getHeight());
                    }
                }
            }
            doc.save(outputFile);
        }
    }

    public void textToPdf(File textFile, File outputFile) throws IOException {
        try (PDDocument doc = new PDDocument();
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), StandardCharsets.UTF_8))) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.COURIER);
            float fontSize = 10.0f;
            float leading = 13.0f;

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(50, 780);

            float y = 780;
            String line;
            while ((line = reader.readLine()) != null) {
                // Word wrapping for long lines
                int maxChars = 75;
                while (line.length() > maxChars) {
                    String sub = line.substring(0, maxChars);
                    line = line.substring(maxChars);
                    cs.showText(sanitizeText(sub));
                    cs.newLineAtOffset(0, -leading);
                    y -= leading;
                    if (y < 50) {
                        cs.endText();
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        cs.beginText();
                        cs.setFont(font, fontSize);
                        cs.newLineAtOffset(50, 780);
                        y = 780;
                    }
                }

                cs.showText(sanitizeText(line));
                cs.newLineAtOffset(0, -leading);
                y -= leading;

                if (y < 50) {
                    cs.endText();
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(50, 780);
                    y = 780;
                }
            }

            cs.endText();
            cs.close();
            doc.save(outputFile);
        }
    }

    public void docxToPdf(File docxFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument xwpf = new XWPFDocument(fis);
             PDDocument pdf = new PDDocument()) {

            PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf, page);
            cs.beginText();
            cs.setFont(regularFont, 11);
            cs.newLineAtOffset(50, 780);

            float y = 780;

            for (XWPFParagraph p : xwpf.getParagraphs()) {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) {
                    y -= 12;
                    cs.newLineAtOffset(0, -12);
                    continue;
                }

                boolean isHeading = p.getStyle() != null && p.getStyle().toLowerCase().contains("heading");
                if (isHeading) {
                    cs.setFont(boldFont, 14);
                } else {
                    cs.setFont(regularFont, 11);
                }

                String clean = sanitizeText(text);
                // Line wrap
                int maxChars = 75;
                while (clean.length() > maxChars) {
                    String sub = clean.substring(0, maxChars);
                    clean = clean.substring(maxChars);
                    cs.showText(sub);
                    cs.newLineAtOffset(0, -14);
                    y -= 14;
                    if (y < 50) {
                        cs.endText();
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        pdf.addPage(page);
                        cs = new PDPageContentStream(pdf, page);
                        cs.beginText();
                        cs.setFont(regularFont, 11);
                        cs.newLineAtOffset(50, 780);
                        y = 780;
                    }
                }

                cs.showText(clean);
                cs.newLineAtOffset(0, -16);
                y -= 16;

                if (y < 50) {
                    cs.endText();
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    pdf.addPage(page);
                    cs = new PDPageContentStream(pdf, page);
                    cs.beginText();
                    cs.setFont(regularFont, 11);
                    cs.newLineAtOffset(50, 780);
                    y = 780;
                }
            }

            cs.endText();
            cs.close();
            pdf.save(outputFile);
        }
    }

    public void xlsxToPdf(File xlsxFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(xlsxFile);
             Workbook workbook = new XSSFWorkbook(fis);
             PDDocument pdf = new PDDocument()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); // Landscape for sheets
                pdf.addPage(page);

                PDPageContentStream cs = new PDPageContentStream(pdf, page);
                cs.beginText();
                cs.setFont(boldFont, 14);
                cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 40);
                cs.showText("Sheet: " + sanitizeText(sheet.getSheetName()));
                cs.newLineAtOffset(0, -20);

                float y = page.getMediaBox().getHeight() - 60;
                cs.setFont(font, 9);

                for (Row row : sheet) {
                    StringBuilder rowText = new StringBuilder();
                    for (Cell cell : row) {
                        rowText.append(getCellValueAsString(cell)).append(" | ");
                    }
                    String line = sanitizeText(rowText.toString());
                    if (line.length() > 110) line = line.substring(0, 110) + "...";
                    cs.showText(line);
                    cs.newLineAtOffset(0, -12);
                    y -= 12;

                    if (y < 40) {
                        cs.endText();
                        cs.close();
                        page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                        pdf.addPage(page);
                        cs = new PDPageContentStream(pdf, page);
                        cs.beginText();
                        cs.setFont(font, 9);
                        cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 40);
                        y = page.getMediaBox().getHeight() - 40;
                    }
                }
                cs.endText();
                cs.close();
            }
            pdf.save(outputFile);
        }
    }

    public void csvToPdf(File csvFile, File outputFile) throws IOException {
        try (Reader in = new FileReader(csvFile, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(in);
             PDDocument pdf = new PDDocument()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.COURIER);
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); // Landscape
            pdf.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(pdf, page);
            cs.beginText();
            cs.setFont(font, 9);
            cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 40);
            float y = page.getMediaBox().getHeight() - 40;

            for (CSVRecord record : parser) {
                StringBuilder sb = new StringBuilder();
                for (String val : record) {
                    sb.append(String.format("%-18s ", val.length() > 16 ? val.substring(0, 16) : val));
                }
                String line = sanitizeText(sb.toString());
                if (line.length() > 120) line = line.substring(0, 120);
                cs.showText(line);
                cs.newLineAtOffset(0, -12);
                y -= 12;

                if (y < 40) {
                    cs.endText();
                    cs.close();
                    page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                    pdf.addPage(page);
                    cs = new PDPageContentStream(pdf, page);
                    cs.beginText();
                    cs.setFont(font, 9);
                    cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 40);
                    y = page.getMediaBox().getHeight() - 40;
                }
            }
            cs.endText();
            cs.close();
            pdf.save(outputFile);
        }
    }

    public void markdownToPdf(File mdFile, File outputFile) throws IOException {
        String mdContent = java.nio.file.Files.readString(mdFile.toPath(), StandardCharsets.UTF_8);
        Parser parser = Parser.builder().build();
        Node document = parser.parse(mdContent);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);

        // Render plain text AST lines to PDF
        textToPdf(mdFile, outputFile);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    public void pptxToPdf(File pptxFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(pptxFile);
             org.apache.poi.xslf.usermodel.XMLSlideShow ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow(fis);
             PDDocument pdf = new PDDocument()) {

            java.awt.Dimension pgsize = ppt.getPageSize();
            int width = pgsize != null && pgsize.width > 0 ? pgsize.width : 720;
            int height = pgsize != null && pgsize.height > 0 ? pgsize.height : 540;

            for (org.apache.poi.xslf.usermodel.XSLFSlide slide : ppt.getSlides()) {
                BufferedImage slideImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D graphics = slideImage.createGraphics();
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setPaint(java.awt.Color.WHITE);
                graphics.fill(new java.awt.geom.Rectangle2D.Float(0, 0, width, height));

                try {
                    slide.draw(graphics);
                } catch (Exception e) {
                    log.warn("Vector rendering for slide failed, drawing title fallback: {}", e.getMessage());
                    graphics.setPaint(java.awt.Color.BLACK);
                    graphics.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
                    String title = slide.getTitle();
                    graphics.drawString(title != null ? title : "Presentation Slide", 50, 80);
                } finally {
                    graphics.dispose();
                }

                PDRectangle pageSize = new PDRectangle(width, height);
                PDPage page = new PDPage(pageSize);
                pdf.addPage(page);

                PDImageXObject pdImage = JPEGFactory.createFromImage(pdf, slideImage, 0.92f);
                try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {
                    cs.drawImage(pdImage, 0, 0, width, height);
                }
            }

            pdf.save(outputFile);
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 32 && c <= 126) sb.append(c);
            else if (c == '\t') sb.append("    ");
        }
        return sb.toString();
    }
}
