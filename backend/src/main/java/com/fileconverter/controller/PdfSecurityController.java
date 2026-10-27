package com.fileconverter.controller;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.service.pdf.PdfSecurityService;
import com.fileconverter.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pdf/security")
@CrossOrigin(originPatterns = "*")
public class PdfSecurityController {

    private static final Logger log = LoggerFactory.getLogger(PdfSecurityController.class);

    private final PdfSecurityService pdfSecurityService;
    private final StorageService storageService;

    @Autowired
    public PdfSecurityController(PdfSecurityService pdfSecurityService, StorageService storageService) {
        this.pdfSecurityService = pdfSecurityService;
        this.storageService = storageService;
    }

    @PostMapping("/protect")
    public ResponseEntity<ConversionJob> protectPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password,
            @RequestParam(value = "ownerPassword", required = false) String ownerPassword
    ) {
        ConversionJob job = storageService.createJob("Protect PDF", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "protected_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfSecurityService.protectPdf(inputFile, password, ownerPassword, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error protecting PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/unlock")
    public ResponseEntity<ConversionJob> unlockPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password
    ) {
        ConversionJob job = storageService.createJob("Unlock PDF", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "unlocked_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfSecurityService.unlockPdf(inputFile, password, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error unlocking PDF", e);
            storageService.failJob(job.getId(), "Password incorrect or decryption failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/sign")
    public ResponseEntity<ConversionJob> signPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("signature") MultipartFile signatureImage,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "x", defaultValue = "100") float x,
            @RequestParam(value = "y", defaultValue = "100") float y,
            @RequestParam(value = "width", defaultValue = "150") float width,
            @RequestParam(value = "height", defaultValue = "60") float height
    ) {
        ConversionJob job = storageService.createJob("Sign PDF", file.getOriginalFilename());
        try {
            String storedFile = storageService.store(file);
            String storedSig = storageService.store(signatureImage);

            File inputFile = storageService.load(storedFile).toFile();
            File sigFile = storageService.load(storedSig).toFile();

            String outputName = "signed_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfSecurityService.stampSignature(inputFile, sigFile, page, x, y, width, height, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error signing PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/redact")
    public ResponseEntity<ConversionJob> redactPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam("x") float x,
            @RequestParam("y") float y,
            @RequestParam("width") float width,
            @RequestParam("height") float height
    ) {
        ConversionJob job = storageService.createJob("Redact PDF", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "redacted_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfSecurityService.redactContent(inputFile, page, x, y, width, height, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error redacting PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> comparePdfs(
            @RequestParam("fileA") MultipartFile fileA,
            @RequestParam("fileB") MultipartFile fileB
    ) {
        try {
            String storedA = storageService.store(fileA);
            String storedB = storageService.store(fileB);

            File inputA = storageService.load(storedA).toFile();
            File inputB = storageService.load(storedB).toFile();

            Map<String, Object> diff = pdfSecurityService.comparePdfs(inputA, inputB);
            diff.put("docAName", fileA.getOriginalFilename());
            diff.put("docBName", fileB.getOriginalFilename());

            return ResponseEntity.ok(diff);
        } catch (Exception e) {
            log.error("Error comparing PDFs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/metadata/remove")
    public ResponseEntity<ConversionJob> removeMetadata(@RequestParam("file") MultipartFile file) {
        ConversionJob job = storageService.createJob("Remove Metadata", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "anonymized_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfSecurityService.removeMetadata(inputFile, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error removing metadata", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/metadata/inspect")
    public ResponseEntity<Map<String, Object>> inspectMetadata(@RequestParam("file") MultipartFile file) {
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            Map<String, Object> meta = pdfSecurityService.inspectMetadata(inputFile);
            meta.put("fileName", file.getOriginalFilename());
            meta.put("fileSizeBytes", file.getSize());

            return ResponseEntity.ok(meta);
        } catch (Exception e) {
            log.error("Error inspecting metadata", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
