package com.fileconverter.controller;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.service.storage.StorageService;
import com.fileconverter.service.utility.UtilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/tools")
@CrossOrigin(originPatterns = "*")
public class UtilityController {

    private static final Logger log = LoggerFactory.getLogger(UtilityController.class);

    private final UtilityService utilityService;
    private final StorageService storageService;

    @Autowired
    public UtilityController(UtilityService utilityService, StorageService storageService) {
        this.utilityService = utilityService;
        this.storageService = storageService;
    }

    @PostMapping("/zip")
    public ResponseEntity<ConversionJob> createZip(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return ResponseEntity.badRequest().build();
        ConversionJob job = storageService.createJob("Create ZIP Archive", files.get(0).getOriginalFilename());
        try {
            List<File> sourceFiles = new ArrayList<>();
            for (MultipartFile f : files) {
                String stored = storageService.store(f);
                sourceFiles.add(storageService.load(stored).toFile());
            }

            String outputName = "archive_" + UUID.randomUUID().toString().substring(0, 8) + ".zip";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            utilityService.createZipArchive(sourceFiles, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("ZIP creation error", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/unzip")
    public ResponseEntity<Map<String, Object>> extractZip(@RequestParam("file") MultipartFile file) {
        try {
            String stored = storageService.store(file);
            File zipFile = storageService.load(stored).toFile();

            File destDir = storageService.getRootLocation().resolve("extracted_" + UUID.randomUUID().toString().substring(0, 8)).toFile();
            destDir.mkdirs();

            List<String> extractedFiles = utilityService.extractZipArchive(zipFile, destDir);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("extractedCount", extractedFiles.size());
            res.put("files", extractedFiles);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Unzip error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/hashes")
    public ResponseEntity<Map<String, String>> calculateHashes(@RequestParam("file") MultipartFile file) {
        try {
            String stored = storageService.store(file);
            File f = storageService.load(stored).toFile();
            Map<String, String> hashes = utilityService.calculateFileHashes(f);
            hashes.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(hashes);
        } catch (Exception e) {
            log.error("Hash calculation error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/metadata")
    public ResponseEntity<Map<String, Object>> inspectMetadata(@RequestParam("file") MultipartFile file) {
        try {
            String stored = storageService.store(file);
            File f = storageService.load(stored).toFile();
            return ResponseEntity.ok(utilityService.inspectFileMetadata(f));
        } catch (Exception e) {
            log.error("Metadata extraction error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/word-count")
    public ResponseEntity<Map<String, Object>> countWords(@RequestBody String text) {
        return ResponseEntity.ok(utilityService.countTextMetrics(text));
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<ConversionJob> generateQrCode(
            @RequestParam("content") String content,
            @RequestParam(value = "size", defaultValue = "300") int size
    ) {
        ConversionJob job = storageService.createJob("QR Code Generator", "qrcode.png");
        try {
            String outputName = "qr_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            utilityService.generateQrCode(content, size, size, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("QR Code generation error", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/qr/scan")
    public ResponseEntity<Map<String, String>> scanQrCode(@RequestParam("file") MultipartFile file) {
        try {
            String stored = storageService.store(file);
            File f = storageService.load(stored).toFile();
            String decoded = utilityService.scanQrCode(f);

            Map<String, String> res = new HashMap<>();
            res.put("decodedContent", decoded);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("QR Code scan error", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "No valid QR Code detected in image"));
        }
    }
}
