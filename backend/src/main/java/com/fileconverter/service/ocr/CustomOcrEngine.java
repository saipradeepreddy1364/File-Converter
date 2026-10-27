package com.fileconverter.service.ocr;

import com.fileconverter.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Primary In-House OCR Engine Orchestrator:
 * - Directs Preprocessing, Layout Analysis, Character & Word Recognition
 * - Supports Multi-Page Scanned PDFs and standard Image Formats (JPG, PNG, WebP, TIFF, BMP)
 * - Generates Searchable PDFs, Editable DOCX, Clean TXT, and Structured JSON
 * - 100% In-House & Local: Zero External Paid APIs or Remote Cloud Endpoints
 */
@Service
public class CustomOcrEngine {

    private static final Logger log = LoggerFactory.getLogger(CustomOcrEngine.class);

    private final OcrImagePreprocessor preprocessor;
    private final OcrLayoutAnalyzer layoutAnalyzer;
    private final NeuralPatternOcrRecognizer recognizer;
    private final SearchablePdfService searchablePdfService;
    private final DocxExportService docxExportService;

    @Autowired
    public CustomOcrEngine(OcrImagePreprocessor preprocessor,
                           OcrLayoutAnalyzer layoutAnalyzer,
                           NeuralPatternOcrRecognizer recognizer,
                           SearchablePdfService searchablePdfService,
                           DocxExportService docxExportService) {
        this.preprocessor = preprocessor;
        this.layoutAnalyzer = layoutAnalyzer;
        this.recognizer = recognizer;
        this.searchablePdfService = searchablePdfService;
        this.docxExportService = docxExportService;
    }

    public OcrResult recognizeImage(BufferedImage image, String language, boolean autoDeskew, boolean enhanceContrast) {
        long startTime = System.currentTimeMillis();

        OcrImagePreprocessor.PreprocessedImage preprocessed = preprocessor.preprocess(image, autoDeskew, enhanceContrast);
        List<OcrLayoutAnalyzer.RawLine> rawLines = layoutAnalyzer.analyzeLayout(preprocessed.getBinaryMatrix());

        List<OcrLineResult> lineResults = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();
        double totalLineConfidence = 0.0;

        for (OcrLayoutAnalyzer.RawLine rl : rawLines) {
            List<OcrWordResult> wordResults = new ArrayList<>();
            StringBuilder lineTextBuilder = new StringBuilder();
            double totalWordConfidence = 0.0;

            for (OcrLayoutAnalyzer.RawWord rw : rl.getWords()) {
                double[] wordConf = new double[1];
                String wordText = recognizer.recognizeWord(rw.getGlyphs(), wordConf);
                if (!wordText.isEmpty()) {
                    wordResults.add(new OcrWordResult(wordText, wordConf[0], rw.getBox()));
                    if (lineTextBuilder.length() > 0) lineTextBuilder.append(" ");
                    lineTextBuilder.append(wordText);
                    totalWordConfidence += wordConf[0];
                }
            }

            if (lineTextBuilder.length() > 0) {
                double lineConf = wordResults.isEmpty() ? 0.0 : (totalWordConfidence / wordResults.size());
                lineResults.add(new OcrLineResult(lineTextBuilder.toString(), lineConf, rl.getBox(), wordResults));
                totalLineConfidence += lineConf;

                if (rl.isNewParagraph() && fullTextBuilder.length() > 0) {
                    fullTextBuilder.append("\n\n");
                } else if (fullTextBuilder.length() > 0) {
                    fullTextBuilder.append("\n");
                }
                fullTextBuilder.append(lineTextBuilder);
            }
        }

        double pageConfidence = lineResults.isEmpty() ? 0.0 : (totalLineConfidence / lineResults.size());
        OcrPageResult pageResult = new OcrPageResult(1, image.getWidth(), image.getHeight(),
                fullTextBuilder.toString(), pageConfidence, lineResults);

        OcrResult result = new OcrResult();
        result.setJobId(UUID.randomUUID().toString());
        result.setFullText(fullTextBuilder.toString());
        result.setAverageConfidence(Math.round(pageConfidence * 10.0) / 10.0);
        result.setPageCount(1);
        result.setPages(Collections.singletonList(pageResult));
        result.setLanguage(language != null ? language : "eng");
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setLowQualityWarning(pageConfidence < 65.0);
        result.setEngineInfo("In-House Neural Pattern Recognition Engine v1.0 (Local Preprocessing & Binarization)");

        return result;
    }

    public OcrResult recognizeImageFile(File imageFile, String language, boolean autoDeskew, boolean enhanceContrast) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) {
            throw new IllegalArgumentException("Unsupported image format or corrupt file: " + imageFile.getName());
        }
        OcrResult result = recognizeImage(img, language, autoDeskew, enhanceContrast);
        result.setOriginalFileName(imageFile.getName());
        return result;
    }

    public OcrResult recognizePdf(File pdfFile, String language, boolean autoDeskew, boolean enhanceContrast) throws IOException {
        long startTime = System.currentTimeMillis();
        List<OcrPageResult> pageResults = new ArrayList<>();
        StringBuilder fullDocText = new StringBuilder();
        double totalDocConfidence = 0.0;
        List<BufferedImage> renderedPages = new ArrayList<>();

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
                // Render at 300 DPI for high recognition fidelity
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIdx, 300, ImageType.RGB);
                renderedPages.add(pageImage);

                OcrResult pageOcr = recognizeImage(pageImage, language, autoDeskew, enhanceContrast);
                if (!pageOcr.getPages().isEmpty()) {
                    OcrPageResult pr = pageOcr.getPages().get(0);
                    pr.setPageNumber(pageIdx + 1);
                    pageResults.add(pr);
                    totalDocConfidence += pr.getConfidence();

                    if (fullDocText.length() > 0) fullDocText.append("\n\n--- Page ").append(pageIdx + 1).append(" ---\n\n");
                    fullDocText.append(pr.getText());
                }
            }
        }

        double docConfidence = pageResults.isEmpty() ? 0.0 : (totalDocConfidence / pageResults.size());

        OcrResult result = new OcrResult();
        result.setJobId(UUID.randomUUID().toString());
        result.setOriginalFileName(pdfFile.getName());
        result.setFullText(fullDocText.toString());
        result.setAverageConfidence(Math.round(docConfidence * 10.0) / 10.0);
        result.setPageCount(pageResults.size());
        result.setPages(pageResults);
        result.setLanguage(language != null ? language : "eng");
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setLowQualityWarning(docConfidence < 65.0);
        result.setEngineInfo("In-House Multi-Page PDF OCR Pipeline (Apache PDFBox 3.0 DPI Rendering + In-House Pattern Recognizer)");

        return result;
    }

    public void generateSearchablePdf(File inputFile, OcrResult ocrResult, File outputFile) throws IOException {
        String name = inputFile.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            searchablePdfService.addSearchableTextLayerToPdf(inputFile, ocrResult, outputFile);
        } else {
            BufferedImage img = ImageIO.read(inputFile);
            searchablePdfService.createSearchablePdf(Collections.singletonList(img), ocrResult, outputFile);
        }
    }

    public void generateDocx(OcrResult ocrResult, File outputFile) throws IOException {
        docxExportService.exportOcrToDocx(ocrResult, outputFile);
    }

    public Map<String, Object> getEngineArchitectureMetadata() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("engineName", "FileConverter Custom OCR Engine");
        meta.put("version", "1.0.0-PROD");
        meta.put("architectureType", "Hybrid Zoning-Density Neural Centroid Classifier with Sauvola Adaptive Binarization");
        meta.put("supportedInputTypes", Arrays.asList("PDF (Scanned/Vector)", "JPG/JPEG", "PNG", "WebP", "TIFF", "BMP"));
        meta.put("supportedOutputFormats", Arrays.asList("Searchable PDF", "Microsoft Word (.docx)", "Plain Text (.txt)", "Markdown (.md)", "Structured JSON (Bounding Boxes)"));
        meta.put("preprocessingStages", Arrays.asList(
                "Grayscale Luminance Weighting (ITU-R BT.601)",
                "Adaptive Contrast Enhancement (Min-Max Histogram Normalization)",
                "Gaussian 3x3 Smoothing & Median Noise Reduction",
                "Automated Deskew Angle Detection (-15° to +15° via Projection Profile Variance)",
                "Sauvola & Otsu Hybrid Dynamic Binarization"
        ));
        meta.put("detectionStages", Arrays.asList(
                "Horizontal Projection Profile Line Segmentation",
                "Vertical Projection Profile Word & Glyph Segmentation",
                "Connected Component Analysis (CCA) with Density Pruning",
                "Reading Order Spatial Clustering & Paragraph Break Grouping"
        ));
        meta.put("recognitionFeatures", "64-D Sub-Cell Zoning Matrix + 8-D Topological Crossing & Symmetry Invariants");
        meta.put("openSourceAttributions", Arrays.asList(
                "Apache PDFBox 3.0.3 (Apache 2.0 License - Used for PDF rasterization and Searchable PDF text layer emission)",
                "Apache POI 5.3.0 (Apache 2.0 License - Used for DOCX OpenXML generation)",
                "TwelveMonkeys ImageIO 3.11.0 (BSD 3-Clause - Used for WebP and TIFF decoding in pure Java)"
        ));
        meta.put("originalInHouseComponents", Arrays.asList(
                "com.fileconverter.service.ocr.OcrImagePreprocessor (Custom noise removal, deskewing, binarization)",
                "com.fileconverter.service.ocr.OcrLayoutAnalyzer (Custom line/word projection segmentation & reading order)",
                "com.fileconverter.service.ocr.NeuralPatternOcrRecognizer (Custom feature extraction, template centroids, lexicon post-processing)",
                "com.fileconverter.service.ocr.OcrEvaluator (Custom CER and WER Levenshtein measurement harness)"
        ));
        meta.put("supportedLanguages", Arrays.asList("eng (English)", "spa (Spanish)", "fra (French)", "deu (German)", "ita (Italian)", "por (Portuguese)"));
        return meta;
    }
}
