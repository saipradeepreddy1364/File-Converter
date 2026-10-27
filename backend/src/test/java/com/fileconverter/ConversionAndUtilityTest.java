package com.fileconverter;

import com.fileconverter.service.conversion.ConvertToPdfService;
import com.fileconverter.service.data.DataConversionService;
import com.fileconverter.service.intelligence.DocumentIntelligenceService;
import com.fileconverter.service.utility.UtilityService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConversionAndUtilityTest {

    private ConvertToPdfService convertToPdfService;
    private DataConversionService dataConversionService;
    private DocumentIntelligenceService intelligenceService;
    private UtilityService utilityService;

    @BeforeEach
    public void setup() {
        convertToPdfService = new ConvertToPdfService();
        dataConversionService = new DataConversionService();
        intelligenceService = new DocumentIntelligenceService();
        utilityService = new UtilityService();
    }

    @Test
    public void testTextToPdf() throws IOException {
        File txt = File.createTempFile("sample_text_", ".txt");
        txt.deleteOnExit();
        Files.writeString(txt.toPath(), "File Converter Platform\nTesting text to PDF conversion with pagination.\nLine 3.");

        File pdfOut = File.createTempFile("converted_txt_", ".pdf");
        pdfOut.deleteOnExit();

        convertToPdfService.textToPdf(txt, pdfOut);

        try (PDDocument doc = Loader.loadPDF(pdfOut)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    public void testCsvToJsonAndBack() throws IOException {
        File csv = File.createTempFile("data_", ".csv");
        csv.deleteOnExit();
        Files.writeString(csv.toPath(), "name,role,department\nAlice,Engineer,Tech\nBob,Designer,Product");

        File jsonOut = File.createTempFile("data_", ".json");
        jsonOut.deleteOnExit();

        dataConversionService.csvToJson(csv, jsonOut);
        String jsonContent = Files.readString(jsonOut.toPath());
        assertTrue(jsonContent.contains("Alice"));
        assertTrue(jsonContent.contains("Tech"));

        File csvOut = File.createTempFile("data_back_", ".csv");
        csvOut.deleteOnExit();
        dataConversionService.jsonToCsv(jsonOut, csvOut);

        String csvContent = Files.readString(csvOut.toPath());
        assertTrue(csvContent.contains("Alice"));
        assertTrue(csvContent.contains("Bob"));
    }

    @Test
    public void testQrCodeGenerationAndScanning() throws Exception {
        String payload = "https://fileconverter.app/verify/12345";
        File qrImage = File.createTempFile("qr_test_", ".png");
        qrImage.deleteOnExit();

        utilityService.generateQrCode(payload, 300, 300, qrImage);
        assertTrue(qrImage.length() > 500);

        String scanned = utilityService.scanQrCode(qrImage);
        assertEquals(payload, scanned);
    }

    @Test
    public void testFileHashCalculation() throws Exception {
        File sample = File.createTempFile("hash_test_", ".txt");
        sample.deleteOnExit();
        Files.writeString(sample.toPath(), "Antigravity OCR and File Converter Engine");

        Map<String, String> hashes = utilityService.calculateFileHashes(sample);
        assertNotNull(hashes.get("MD5"));
        assertNotNull(hashes.get("SHA-256"));
        assertEquals(32, hashes.get("MD5").length());
        assertEquals(64, hashes.get("SHA-256").length());
    }

    @Test
    public void testDocumentSummarizerAndEntities() throws IOException {
        File doc = File.createTempFile("doc_intel_", ".txt");
        doc.deleteOnExit();
        Files.writeString(doc.toPath(),
                "Optical Character Recognition is a vital document technology. " +
                "The File Converter platform converts PDF and document formats. " +
                "Contact our engineering team at support@fileconverter.io or call 555-123-4567. " +
                "The project started on 2026-01-15 with a budget of $50000 USD.");

        Map<String, Object> summary = intelligenceService.summarizeDocument(doc, 2);
        assertNotNull(summary.get("summary"));

        Map<String, Object> entities = intelligenceService.extractEntities(doc);
        assertTrue(entities.toString().contains("support@fileconverter.io"));
    }
}
