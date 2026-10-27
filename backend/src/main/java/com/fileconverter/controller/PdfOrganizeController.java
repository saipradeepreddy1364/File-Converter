package com.fileconverter.controller;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.service.pdf.PdfOrganizeService;
import com.fileconverter.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(originPatterns = "*")
public class PdfOrganizeController {

    private static final Logger log = LoggerFactory.getLogger(PdfOrganizeController.class);

    private final PdfOrganizeService pdfOrganizeService;
    private final StorageService storageService;

    @Autowired
    public PdfOrganizeController(PdfOrganizeService pdfOrganizeService, StorageService storageService) {
        this.pdfOrganizeService = pdfOrganizeService;
        this.storageService = storageService;
    }

    @PostMapping("/merge")
    public ResponseEntity<ConversionJob> mergePdfs(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) List<MultipartFile> singleFiles) {
        if (files == null || files.isEmpty()) {
            files = singleFiles;
        }
        if (files == null || files.size() < 2) {
            return ResponseEntity.badRequest().build();
        }
        ConversionJob job = storageService.createJob("Merge PDF", files.get(0).getOriginalFilename());
        try {
            List<File> sourceFiles = new ArrayList<>();
            for (MultipartFile f : files) {
                String stored = storageService.store(f);
                sourceFiles.add(storageService.load(stored).toFile());
            }

            String outputName = "merged_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();
            pdfOrganizeService.mergePdfs(sourceFiles, outputFile);

            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error merging PDFs", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/split")
    public ResponseEntity<ConversionJob> splitPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pageRange", required = false) String pageRange
    ) {
        ConversionJob job = storageService.createJob("Split PDF", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            boolean isSingleExtract = pageRange != null && !pageRange.trim().isEmpty() && !pageRange.equalsIgnoreCase("all");
            String outputName = isSingleExtract ? "extracted_pages_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf"
                    : "split_pages_" + UUID.randomUUID().toString().substring(0, 8) + ".zip";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOrganizeService.splitPdf(inputFile, pageRange, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error splitting PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/remove-pages")
    public ResponseEntity<ConversionJob> removePages(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pages") String pagesToRemove
    ) {
        ConversionJob job = storageService.createJob("Remove Pages", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "pages_removed_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOrganizeService.removePages(inputFile, pagesToRemove, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error removing pages", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/rotate")
    public ResponseEntity<ConversionJob> rotatePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "angle", defaultValue = "90") int angle,
            @RequestParam(value = "pages", defaultValue = "all") String pages
    ) {
        ConversionJob job = storageService.createJob("Rotate PDF", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "rotated_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOrganizeService.rotatePdf(inputFile, angle, pages, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error rotating PDF", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/page-numbers")
    public ResponseEntity<ConversionJob> addPageNumbers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "position", defaultValue = "bottom-center") String position,
            @RequestParam(value = "format", defaultValue = "Page {n} of {total}") String format,
            @RequestParam(value = "startNumber", defaultValue = "1") int startNumber
    ) {
        ConversionJob job = storageService.createJob("Add Page Numbers", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "numbered_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOrganizeService.addPageNumbers(inputFile, position, format, startNumber, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error adding page numbers", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/watermark")
    public ResponseEntity<ConversionJob> addWatermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("text") String watermarkText,
            @RequestParam(value = "opacity", defaultValue = "0.25") float opacity,
            @RequestParam(value = "rotation", defaultValue = "45") float rotation
    ) {
        ConversionJob job = storageService.createJob("Add Watermark", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "watermarked_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOrganizeService.addWatermark(inputFile, watermarkText, opacity, rotation, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error adding watermark", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/reverse")
    public ResponseEntity<ConversionJob> reversePages(@RequestParam("file") MultipartFile file) {
        ConversionJob job = storageService.createJob("Reverse Page Order", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "reversed_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOrganizeService.reversePageOrder(inputFile, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error reversing pages", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/extract-images")
    public ResponseEntity<ConversionJob> extractImages(@RequestParam("file") MultipartFile file) {
        ConversionJob job = storageService.createJob("Extract Images", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String outputName = "extracted_images_" + UUID.randomUUID().toString().substring(0, 8) + ".zip";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            pdfOrganizeService.extractImages(inputFile, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error extracting images", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/extract-text")
    public ResponseEntity<ConversionJob> extractText(@RequestParam("file") MultipartFile file) {
        ConversionJob job = storageService.createJob("Extract Text", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String text = pdfOrganizeService.extractText(inputFile);
            String outputName = "extracted_text_" + UUID.randomUUID().toString().substring(0, 8) + ".txt";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();
            Files.writeString(outputFile.toPath(), text, StandardCharsets.UTF_8);

            storageService.completeJob(job.getId(), outputName, outputFile.length());
            job.getMetadata().put("extractedText", text);
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Error extracting text", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }
}
