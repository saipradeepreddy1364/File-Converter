package com.fileconverter.controller;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.service.pdf.PdfOptimizeService;
import com.fileconverter.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/pdf/optimize")
@CrossOrigin(originPatterns = "*")
public class PdfOptimizeController {

    private static final Logger log = LoggerFactory.getLogger(PdfOptimizeController.class);

    private final PdfOptimizeService pdfOptimizeService;
    private final StorageService storageService;

    @Autowired
    public PdfOptimizeController(PdfOptimizeService pdfOptimizeService, StorageService storageService) {
        this.pdfOptimizeService = pdfOptimizeService;
        this.storageService = storageService;
    }

    @PostMapping("/compress")
    public ResponseEntity<ConversionJob> compressPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "level", defaultValue = "medium") String level
    ) {
        ConversionJob job = storageService.createJob("Compress PDF", file.getOriginalFilename());
        job.setOriginalSizeBytes(file.getSize());

        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "compressed_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            PdfOptimizeService.CompressionResult result = pdfOptimizeService.compressPdf(inputFile, level, outputFile);

            storageService.completeJob(job.getId(), outputName, result.getCompressedSize());
            job.getMetadata().put("savedPercentage", result.getSavedPercentage());
            job.getMetadata().put("originalSizeBytes", result.getOriginalSize());
            job.getMetadata().put("compressedSizeBytes", result.getCompressedSize());

            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error compressing PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/repair")
    public ResponseEntity<ConversionJob> repairPdf(@RequestParam("file") MultipartFile file) {
        ConversionJob job = storageService.createJob("Repair PDF", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "repaired_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOptimizeService.repairPdf(inputFile, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error repairing PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/flatten")
    public ResponseEntity<ConversionJob> flattenPdf(@RequestParam("file") MultipartFile file) {
        ConversionJob job = storageService.createJob("Flatten PDF", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "flattened_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOptimizeService.flattenPdf(inputFile, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error flattening PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }
}
