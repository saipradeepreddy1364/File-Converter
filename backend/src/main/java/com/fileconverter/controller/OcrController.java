package com.fileconverter.controller;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.model.JobStatus;
import com.fileconverter.model.OcrResult;
import com.fileconverter.service.ocr.CustomOcrEngine;
import com.fileconverter.service.ocr.OcrEvaluator;
import com.fileconverter.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(originPatterns = "*")
public class OcrController {

    private static final Logger log = LoggerFactory.getLogger(OcrController.class);

    private final CustomOcrEngine ocrEngine;
    private final OcrEvaluator ocrEvaluator;
    private final StorageService storageService;

    // Cache of completed OCR results for retrieval & download
    private final Map<String, OcrResult> ocrResultCache = new ConcurrentHashMap<>();
    private final Map<String, String> ocrDownloadFiles = new ConcurrentHashMap<>();

    @Autowired
    public OcrController(CustomOcrEngine ocrEngine, OcrEvaluator ocrEvaluator, StorageService storageService) {
        this.ocrEngine = ocrEngine;
        this.ocrEvaluator = ocrEvaluator;
        this.storageService = storageService;
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversionJob> processOcr(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "eng") String language,
            @RequestParam(value = "autoDeskew", defaultValue = "true") boolean autoDeskew,
            @RequestParam(value = "enhanceContrast", defaultValue = "true") boolean enhanceContrast,
            @RequestParam(value = "outputFormat", defaultValue = "txt") String outputFormat
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ConversionJob job = storageService.createJob("OCR Recognition", file.getOriginalFilename());
        job.setStatus(JobStatus.PROCESSING);
        job.setProgress(15);
        job.setMessage("Preprocessing document images & detecting text regions");

        try {
            String storedInput = storageService.store(file);
            File inputFile = storageService.load(storedInput).toFile();

            OcrResult ocrResult;
            String nameLower = file.getOriginalFilename().toLowerCase();
            if (nameLower.endsWith(".pdf")) {
                ocrResult = ocrEngine.recognizePdf(inputFile, language, autoDeskew, enhanceContrast);
            } else {
                ocrResult = ocrEngine.recognizeImageFile(inputFile, language, autoDeskew, enhanceContrast);
            }

            ocrResult.setJobId(job.getId());
            ocrResultCache.put(job.getId(), ocrResult);

            // Generate requested downloadable format
            String baseName = getBaseName(file.getOriginalFilename());
            String outputFileName;
            File outputFile;

            switch (outputFormat.toLowerCase()) {
                case "searchable_pdf":
                    outputFileName = "searchable_" + baseName + ".pdf";
                    outputFile = storageService.getRootLocation().resolve(outputFileName).toFile();
                    ocrEngine.generateSearchablePdf(inputFile, ocrResult, outputFile);
                    break;
                case "docx":
                    outputFileName = "ocr_" + baseName + ".docx";
                    outputFile = storageService.getRootLocation().resolve(outputFileName).toFile();
                    ocrEngine.generateDocx(ocrResult, outputFile);
                    break;
                case "txt":
                default:
                    outputFileName = "extracted_" + baseName + ".txt";
                    outputFile = storageService.getRootLocation().resolve(outputFileName).toFile();
                    Files.writeString(outputFile.toPath(), ocrResult.getFullText(), StandardCharsets.UTF_8);
                    break;
            }

            ocrDownloadFiles.put(job.getId(), outputFileName);
            storageService.completeJob(job.getId(), outputFileName, outputFile.length());
            job.getMetadata().put("ocrResult", ocrResult);

            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("OCR processing failed for job: {}", job.getId(), e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ConversionJob> getOcrStatus(@PathVariable String jobId) {
        ConversionJob job = storageService.getJob(jobId);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }

    @GetMapping("/result/{jobId}")
    public ResponseEntity<OcrResult> getOcrResult(@PathVariable String jobId) {
        OcrResult result = ocrResultCache.get(jobId);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<Resource> downloadOcrResult(@PathVariable String jobId) throws IOException {
        String fileName = ocrDownloadFiles.get(jobId);
        if (fileName == null) {
            ConversionJob job = storageService.getJob(jobId);
            if (job != null && job.getOutputFileName() != null) {
                fileName = job.getOutputFileName();
            }
        }
        if (fileName == null) return ResponseEntity.notFound().build();

        Resource resource = storageService.loadAsResource(fileName);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileName.endsWith(".pdf")) mediaType = MediaType.APPLICATION_PDF;
        else if (fileName.endsWith(".docx")) mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        else if (fileName.endsWith(".txt")) mediaType = MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getEngineInfo() {
        return ResponseEntity.ok(ocrEngine.getEngineArchitectureMetadata());
    }

    @PostMapping("/evaluate")
    public ResponseEntity<OcrEvaluator.EvaluationReport> runEvaluation() {
        log.info("Executing comprehensive OCR benchmark evaluation suite");
        OcrEvaluator.EvaluationReport report = ocrEvaluator.evaluateEngine(ocrEngine);
        return ResponseEntity.ok(report);
    }

    private String getBaseName(String filename) {
        if (filename == null) return "document";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
