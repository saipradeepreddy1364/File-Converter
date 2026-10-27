package com.fileconverter.service.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PdfOrganizeService {

    private static final Logger log = LoggerFactory.getLogger(PdfOrganizeService.class);

    public void mergePdfs(List<File> files, File outputFile) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(outputFile.getAbsolutePath());
        for (File f : files) {
            merger.addSource(f);
        }
        merger.mergeDocuments(null);
        log.info("Merged {} PDF documents into {}", files.size(), outputFile.getName());
    }

    public void splitPdf(File inputFile, String pageRange, File outputFileOrZip) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int totalPages = doc.getNumberOfPages();
            Set<Integer> selectedPages = parsePageRange(pageRange, totalPages);

            if (selectedPages.isEmpty() || selectedPages.size() == totalPages) {
                // Split every page into zip
                Splitter splitter = new Splitter();
                List<PDDocument> pages = splitter.split(doc);
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFileOrZip))) {
                    int pNum = 1;
                    for (PDDocument pageDoc : pages) {
                        ZipEntry entry = new ZipEntry("page_" + pNum + ".pdf");
                        zos.putNextEntry(entry);
                        pageDoc.save(zos);
                        zos.closeEntry();
                        pageDoc.close();
                        pNum++;
                    }
                }
            } else {
                // Extract selected pages into single PDF
                try (PDDocument newDoc = new PDDocument()) {
                    for (int p : selectedPages) {
                        newDoc.addPage(doc.getPage(p - 1));
                    }
                    newDoc.save(outputFileOrZip);
                }
            }
        }
    }

    public void removePages(File inputFile, String pagesToRemove, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int total = doc.getNumberOfPages();
            Set<Integer> toRemove = parsePageRange(pagesToRemove, total);

            try (PDDocument newDoc = new PDDocument()) {
                for (int i = 1; i <= total; i++) {
                    if (!toRemove.contains(i)) {
                        newDoc.addPage(doc.getPage(i - 1));
                    }
                }
                if (newDoc.getNumberOfPages() == 0) {
                    throw new IllegalArgumentException("Cannot remove all pages from PDF");
                }
                newDoc.save(outputFile);
            }
        }
    }

    public void reorderPages(File inputFile, List<Integer> newOrder, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int total = doc.getNumberOfPages();
            try (PDDocument newDoc = new PDDocument()) {
                for (int pageNum : newOrder) {
                    if (pageNum >= 1 && pageNum <= total) {
                        newDoc.addPage(doc.getPage(pageNum - 1));
                    }
                }
                newDoc.save(outputFile);
            }
        }
    }

    public void rotatePdf(File inputFile, int rotationDegrees, String pageRange, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int total = doc.getNumberOfPages();
            Set<Integer> targetPages = (pageRange == null || pageRange.trim().isEmpty() || pageRange.equalsIgnoreCase("all"))
                    ? null : parsePageRange(pageRange, total);

            for (int i = 0; i < total; i++) {
                int pageNum = i + 1;
                if (targetPages == null || targetPages.contains(pageNum)) {
                    PDPage page = doc.getPage(i);
                    int currentRotation = page.getRotation();
                    page.setRotation((currentRotation + rotationDegrees) % 360);
                }
            }
            doc.save(outputFile);
        }
    }

    public void addPageNumbers(File inputFile, String position, String format, int startNumber, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int total = doc.getNumberOfPages();
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float fontSize = 10.0f;

            for (int i = 0; i < total; i++) {
                PDPage page = doc.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                float width = mediaBox.getWidth();
                float height = mediaBox.getHeight();

                int currentNumber = startNumber + i;
                String pageText = (format != null && format.contains("{total}"))
                        ? format.replace("{n}", String.valueOf(currentNumber)).replace("{total}", String.valueOf(total))
                        : (format != null ? format.replace("{n}", String.valueOf(currentNumber)) : "Page " + currentNumber + " of " + total);

                float stringWidth = font.getStringWidth(pageText) / 1000 * fontSize;

                float x;
                float y;
                String pos = position != null ? position.toLowerCase() : "bottom-center";

                if (pos.contains("left")) {
                    x = 40;
                } else if (pos.contains("right")) {
                    x = width - stringWidth - 40;
                } else {
                    x = (width - stringWidth) / 2;
                }

                if (pos.contains("top")) {
                    y = height - 30;
                } else {
                    y = 30;
                }

                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(x, y);
                    cs.showText(pageText);
                    cs.endText();
                }
            }
            doc.save(outputFile);
        }
    }

    public void addWatermark(File inputFile, String text, float opacity, float rotationDegrees, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            float fontSize = 48.0f;

            for (PDPage page : doc.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                float width = mediaBox.getWidth();
                float height = mediaBox.getHeight();
                float stringWidth = font.getStringWidth(text) / 1000 * fontSize;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(opacity > 0 ? opacity : 0.25f);
                    cs.setGraphicsStateParameters(gs);
                    cs.setNonStrokingColor(0.7f, 0.7f, 0.7f);

                    cs.saveGraphicsState();
                    cs.transform(org.apache.pdfbox.util.Matrix.getRotateInstance(Math.toRadians(rotationDegrees), width / 2, height / 2));
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset((width - stringWidth) / 2, height / 2);
                    cs.showText(text);
                    cs.endText();
                    cs.restoreGraphicsState();
                }
            }
            doc.save(outputFile);
        }
    }

    public void reversePageOrder(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int total = doc.getNumberOfPages();
            try (PDDocument newDoc = new PDDocument()) {
                for (int i = total - 1; i >= 0; i--) {
                    newDoc.addPage(doc.getPage(i));
                }
                newDoc.save(outputFile);
            }
        }
    }

    public void insertBlankPage(File inputFile, int atPosition, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int total = doc.getNumberOfPages();
            PDRectangle standardSize = total > 0 ? doc.getPage(0).getMediaBox() : PDRectangle.A4;
            PDPage blankPage = new PDPage(standardSize);

            try (PDDocument newDoc = new PDDocument()) {
                int pos = Math.max(1, Math.min(total + 1, atPosition));
                for (int i = 1; i <= total; i++) {
                    if (i == pos) {
                        newDoc.addPage(blankPage);
                    }
                    newDoc.addPage(doc.getPage(i - 1));
                }
                if (pos > total) {
                    newDoc.addPage(blankPage);
                }
                newDoc.save(outputFile);
            }
        }
    }

    public String extractText(File inputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public void extractImages(File inputFile, File outputZip) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {
            int imgIndex = 1;
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                PDPage page = doc.getPage(p);
                PDResources resources = page.getResources();
                for (COSName name : resources.getXObjectNames()) {
                    if (resources.isImageXObject(name)) {
                        PDImageXObject image = (PDImageXObject) resources.getXObject(name);
                        BufferedImage bImage = image.getImage();
                        ZipEntry entry = new ZipEntry("page" + (p + 1) + "_img" + imgIndex + ".png");
                        zos.putNextEntry(entry);
                        ImageIO.write(bImage, "PNG", zos);
                        zos.closeEntry();
                        imgIndex++;
                    }
                }
            }
        }
    }

    private Set<Integer> parsePageRange(String pageRange, int totalPages) {
        Set<Integer> pages = new TreeSet<>();
        if (pageRange == null || pageRange.trim().isEmpty()) {
            return pages;
        }
        String[] parts = pageRange.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.contains("-")) {
                String[] range = trimmed.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = Math.max(1, start); i <= Math.min(totalPages, end); i++) {
                            pages.add(i);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    int p = Integer.parseInt(trimmed);
                    if (p >= 1 && p <= totalPages) {
                        pages.add(p);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return pages;
    }
}
