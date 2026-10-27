package com.fileconverter;

import com.fileconverter.model.OcrResult;
import com.fileconverter.service.ocr.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class CustomOcrEngineTest {

    private CustomOcrEngine ocrEngine;
    private OcrEvaluator ocrEvaluator;

    @BeforeEach
    public void setup() {
        OcrImagePreprocessor preprocessor = new OcrImagePreprocessor();
        OcrLayoutAnalyzer layoutAnalyzer = new OcrLayoutAnalyzer();
        NeuralPatternOcrRecognizer recognizer = new NeuralPatternOcrRecognizer();
        recognizer.init();

        SearchablePdfService searchablePdfService = new SearchablePdfService();
        DocxExportService docxExportService = new DocxExportService();

        ocrEngine = new CustomOcrEngine(preprocessor, layoutAnalyzer, recognizer, searchablePdfService, docxExportService);
        ocrEvaluator = new OcrEvaluator();
    }

    @Test
    public void testOcrRecognitionOnSyntheticImage() {
        String testText = "INVOICE 2026";
        BufferedImage img = renderTestText(testText, 28);

        OcrResult result = ocrEngine.recognizeImage(img, "eng", false, true);

        assertNotNull(result);
        assertNotNull(result.getFullText());
        assertFalse(result.getPages().isEmpty());
        assertTrue(result.getAverageConfidence() > 50.0, "Confidence should exceed 50%: " + result.getAverageConfidence());
        assertTrue(result.getFullText().contains("INVOICE") || result.getFullText().contains("2026"),
                "Recognized text should contain key tokens: " + result.getFullText());
    }

    @Test
    public void testCerAndWerCalculation() {
        String reference = "hello world file converter";
        String hypothesis = "hello world file conv3rter";

        double cer = ocrEvaluator.calculateCer(reference, hypothesis);
        double wer = ocrEvaluator.calculateWer(reference, hypothesis);

        assertTrue(cer > 0 && cer < 15, "CER should be low for single character difference: " + cer);
        assertEquals(25.0, wer, 0.1, "WER should be 25% (1 out of 4 words changed)");

        // Perfect match
        assertEquals(0.0, ocrEvaluator.calculateCer("test", "test"));
        assertEquals(0.0, ocrEvaluator.calculateWer("test", "test"));
    }

    @Test
    public void testSearchablePdfGeneration() throws IOException {
        BufferedImage img = renderTestText("Searchable PDF Layer Verification", 24);
        OcrResult result = ocrEngine.recognizeImage(img, "eng", false, false);

        File tempImg = File.createTempFile("source_img_", ".png");
        tempImg.deleteOnExit();
        javax.imageio.ImageIO.write(img, "PNG", tempImg);

        File tempPdf = File.createTempFile("searchable_test_", ".pdf");
        tempPdf.deleteOnExit();

        ocrEngine.generateSearchablePdf(tempImg, result, tempPdf);
        assertTrue(tempPdf.exists());
        assertTrue(tempPdf.length() > 500, "Searchable PDF should have content: " + tempPdf.length());
    }

    @Test
    public void testDocxExport() throws IOException {
        BufferedImage img = renderTestText("Document Word Export Test", 24);
        OcrResult result = ocrEngine.recognizeImage(img, "eng", false, false);

        File tempDocx = File.createTempFile("ocr_test_", ".docx");
        tempDocx.deleteOnExit();

        ocrEngine.generateDocx(result, tempDocx);
        assertTrue(tempDocx.exists());
        assertTrue(tempDocx.length() > 1000, "Word document should have valid OpenXML structure");
    }

    private BufferedImage renderTestText(String text, int fontSize) {
        int w = 700;
        int h = 120;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
        g.drawString(text, 30, 70);
        g.dispose();
        return img;
    }
}
