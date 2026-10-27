package com.fileconverter;

import com.fileconverter.service.pdf.PdfOptimizeService;
import com.fileconverter.service.pdf.PdfOrganizeService;
import com.fileconverter.service.pdf.PdfSecurityService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PdfServicesTest {

    private PdfOrganizeService organizeService;
    private PdfOptimizeService optimizeService;
    private PdfSecurityService securityService;

    @BeforeEach
    public void setup() {
        organizeService = new PdfOrganizeService();
        optimizeService = new PdfOptimizeService();
        securityService = new PdfSecurityService();
    }

    private File createSamplePdf(String text, int pages) throws IOException {
        File file = File.createTempFile("test_sample_", ".pdf");
        file.deleteOnExit();
        try (PDDocument doc = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(font, 14);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(text + " - Page " + (i + 1));
                    cs.endText();
                }
            }
            doc.save(file);
        }
        return file;
    }

    @Test
    public void testMergePdfs() throws IOException {
        File pdf1 = createSamplePdf("Document A", 2);
        File pdf2 = createSamplePdf("Document B", 3);
        File merged = File.createTempFile("merged_", ".pdf");
        merged.deleteOnExit();

        organizeService.mergePdfs(Arrays.asList(pdf1, pdf2), merged);

        try (PDDocument doc = Loader.loadPDF(merged)) {
            assertEquals(5, doc.getNumberOfPages(), "Merged PDF should have 5 pages total");
        }
    }

    @Test
    public void testSplitPdf() throws IOException {
        File multi = createSamplePdf("Split Doc", 4);
        File splitOut = File.createTempFile("split_", ".pdf");
        splitOut.deleteOnExit();

        organizeService.splitPdf(multi, "1-2", splitOut);

        try (PDDocument doc = Loader.loadPDF(splitOut)) {
            assertEquals(2, doc.getNumberOfPages(), "Split PDF should have 2 extracted pages");
        }
    }

    @Test
    public void testRotatePdf() throws IOException {
        File sample = createSamplePdf("Rotate Doc", 1);
        File rotated = File.createTempFile("rotated_", ".pdf");
        rotated.deleteOnExit();

        organizeService.rotatePdf(sample, 90, "all", rotated);

        try (PDDocument doc = Loader.loadPDF(rotated)) {
            assertEquals(90, doc.getPage(0).getRotation());
        }
    }

    @Test
    public void testPasswordProtectAndUnlock() throws IOException {
        File sample = createSamplePdf("Secret Document", 1);
        File protectedPdf = File.createTempFile("protected_", ".pdf");
        protectedPdf.deleteOnExit();

        securityService.protectPdf(sample, "secret123", "owner123", protectedPdf);

        // Verify protected
        try (PDDocument doc = Loader.loadPDF(protectedPdf, "secret123")) {
            assertTrue(doc.isEncrypted());
        }

        // Unlock
        File unlockedPdf = File.createTempFile("unlocked_", ".pdf");
        unlockedPdf.deleteOnExit();
        securityService.unlockPdf(protectedPdf, "secret123", unlockedPdf);

        try (PDDocument doc = Loader.loadPDF(unlockedPdf)) {
            assertFalse(doc.isEncrypted());
        }
    }

    @Test
    public void testComparePdfs() throws IOException {
        File doc1 = createSamplePdf("Unique Text Alpha", 1);
        File doc2 = createSamplePdf("Unique Text Beta", 1);

        Map<String, Object> diff = securityService.comparePdfs(doc1, doc2);
        assertNotNull(diff);
        assertTrue(diff.containsKey("similarityPercentage"));
        assertTrue((Double) diff.get("similarityPercentage") >= 0);
    }
}
