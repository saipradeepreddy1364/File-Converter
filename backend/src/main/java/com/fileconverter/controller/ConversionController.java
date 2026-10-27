package com.fileconverter.controller;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.service.conversion.ConvertFromPdfService;
import com.fileconverter.service.conversion.ConvertToPdfService;
import com.fileconverter.service.conversion.DocumentConversionService;
import com.fileconverter.service.conversion.ImageConversionService;
import com.fileconverter.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/api/convert")
@CrossOrigin(origins = "*")
public class ConversionController {

    private static final Logger log = LoggerFactory.getLogger(ConversionController.class);

    private final ConvertToPdfService convertToPdfService;
    private final ConvertFromPdfService convertFromPdfService;
    private final ImageConversionService imageConversionService;
    private final DocumentConversionService documentConversionService;
    private final StorageService storageService;

    @Autowired
    public ConversionController(ConvertToPdfService convertToPdfService,
                                ConvertFromPdfService convertFromPdfService,
                                ImageConversionService imageConversionService,
                                DocumentConversionService documentConversionService,
                                StorageService storageService) {
        this.convertToPdfService = convertToPdfService;
        this.convertFromPdfService = convertFromPdfService;
        this.imageConversionService = imageConversionService;
        this.documentConversionService = documentConversionService;
        this.storageService = storageService;
    }

    @PostMapping("/to-pdf")
    public ResponseEntity<ConversionJob> convertToPdf(
            @RequestParam("file") MultipartFile file
    ) {
        String originalName = file.getOriginalFilename();
        ConversionJob job = storageService.createJob("Convert to PDF", originalName);

        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String baseName = getBaseName(originalName);
            String outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            String ext = getExtension(originalName).toLowerCase();
            switch (ext) {
                case "jpg":
                case "jpeg":
                case "png":
                case "webp":
                case "bmp":
                case "tiff":
                    convertToPdfService.imagesToPdf(Collections.singletonList(inputFile), outputFile);
                    break;
                case "docx":
                    convertToPdfService.docxToPdf(inputFile, outputFile);
                    break;
                case "xlsx":
                case "xls":
                    convertToPdfService.xlsxToPdf(inputFile, outputFile);
                    break;
                case "csv":
                    convertToPdfService.csvToPdf(inputFile, outputFile);
                    break;
                case "md":
                case "markdown":
                    convertToPdfService.markdownToPdf(inputFile, outputFile);
                    break;
                case "txt":
                default:
                    convertToPdfService.textToPdf(inputFile, outputFile);
                    break;
            }

            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Conversion to PDF failed", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/to-pdf/multiple")
    public ResponseEntity<ConversionJob> convertMultipleImagesToPdf(
            @RequestParam("files") List<MultipartFile> files
    ) {
        ConversionJob job = storageService.createJob("Images to PDF", files.get(0).getOriginalFilename());
        try {
            List<File> imageFiles = new ArrayList<>();
            for (MultipartFile f : files) {
                String stored = storageService.store(f);
                imageFiles.add(storageService.load(stored).toFile());
            }

            String outputName = "combined_images_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            convertToPdfService.imagesToPdf(imageFiles, outputFile);
            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Multiple images to PDF failed", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/from-pdf")
    public ResponseEntity<ConversionJob> convertFromPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat,
            @RequestParam(value = "dpi", defaultValue = "200") int dpi
    ) {
        String originalName = file.getOriginalFilename();
        ConversionJob job = storageService.createJob("PDF to " + targetFormat.toUpperCase(), originalName);

        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String baseName = getBaseName(originalName);
            String fmt = targetFormat.toLowerCase();
            String outputName;
            File outputFile;

            switch (fmt) {
                case "jpg":
                case "jpeg":
                case "png":
                case "webp":
                    outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".zip";
                    outputFile = storageService.getRootLocation().resolve(outputName).toFile();
                    convertFromPdfService.pdfToImages(inputFile, fmt, dpi, outputFile);
                    break;
                case "docx":
                    outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".docx";
                    outputFile = storageService.getRootLocation().resolve(outputName).toFile();
                    convertFromPdfService.pdfToDocx(inputFile, outputFile);
                    break;
                case "txt":
                    outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".txt";
                    outputFile = storageService.getRootLocation().resolve(outputName).toFile();
                    convertFromPdfService.pdfToText(inputFile, outputFile);
                    break;
                case "md":
                case "markdown":
                    outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".md";
                    outputFile = storageService.getRootLocation().resolve(outputName).toFile();
                    convertFromPdfService.pdfToMarkdown(inputFile, outputFile);
                    break;
                case "html":
                    outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".html";
                    outputFile = storageService.getRootLocation().resolve(outputName).toFile();
                    convertFromPdfService.pdfToHtml(inputFile, outputFile);
                    break;
                case "csv":
                    outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".csv";
                    outputFile = storageService.getRootLocation().resolve(outputName).toFile();
                    convertFromPdfService.extractTablesToCsv(inputFile, outputFile);
                    break;
                case "xlsx":
                    outputName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".xlsx";
                    outputFile = storageService.getRootLocation().resolve(outputName).toFile();
                    convertFromPdfService.extractTablesToXlsx(inputFile, outputFile);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported conversion target from PDF: " + targetFormat);
            }

            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Conversion from PDF failed", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/image")
    public ResponseEntity<ConversionJob> convertImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetFormat", required = false) String targetFormat,
            @RequestParam(value = "width", defaultValue = "0") int width,
            @RequestParam(value = "height", defaultValue = "0") int height,
            @RequestParam(value = "keepAspectRatio", defaultValue = "true") boolean keepAspectRatio,
            @RequestParam(value = "rotateDegrees", defaultValue = "0") int rotateDegrees,
            @RequestParam(value = "flipH", defaultValue = "false") boolean flipH,
            @RequestParam(value = "flipV", defaultValue = "false") boolean flipV,
            @RequestParam(value = "cropX", defaultValue = "-1") int cropX,
            @RequestParam(value = "cropY", defaultValue = "-1") int cropY,
            @RequestParam(value = "cropW", defaultValue = "0") int cropW,
            @RequestParam(value = "cropH", defaultValue = "0") int cropH,
            @RequestParam(value = "quality", defaultValue = "0") float quality
    ) {
        ConversionJob job = storageService.createJob("Image Processing", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File currentFile = storageService.load(stored).toFile();

            String baseName = getBaseName(file.getOriginalFilename());
            String ext = (targetFormat != null && !targetFormat.isEmpty()) ? targetFormat.toLowerCase() : getExtension(file.getOriginalFilename());

            File workingOut = storageService.getRootLocation().resolve("proc_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext).toFile();

            // 1. Crop if specified
            if (cropX >= 0 && cropY >= 0 && cropW > 0 && cropH > 0) {
                imageConversionService.cropImage(currentFile, cropX, cropY, cropW, cropH, workingOut);
                currentFile = workingOut;
                workingOut = storageService.getRootLocation().resolve("proc_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext).toFile();
            }

            // 2. Rotate / Flip if specified
            if (rotateDegrees != 0 || flipH || flipV) {
                imageConversionService.rotateAndFlip(currentFile, rotateDegrees, flipH, flipV, workingOut);
                currentFile = workingOut;
                workingOut = storageService.getRootLocation().resolve("proc_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext).toFile();
            }

            // 3. Resize if specified
            if (width > 0 || height > 0) {
                imageConversionService.resizeImage(currentFile, width, height, keepAspectRatio, workingOut);
                currentFile = workingOut;
                workingOut = storageService.getRootLocation().resolve("proc_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext).toFile();
            }

            // 4. Compress / Change Quality if specified
            if (quality > 0 && quality < 1.0f) {
                imageConversionService.compressImage(currentFile, quality, workingOut);
                currentFile = workingOut;
                workingOut = storageService.getRootLocation().resolve("proc_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext).toFile();
            }

            // 5. Format conversion if requested and not yet converted
            if (targetFormat != null && !targetFormat.isEmpty()) {
                imageConversionService.convertFormat(currentFile, targetFormat, "#ffffff", workingOut);
                currentFile = workingOut;
            }

            String finalOutputName = baseName + "_converted." + ext;
            File finalFile = storageService.getRootLocation().resolve(finalOutputName).toFile();
            Files.copy(currentFile.toPath(), finalFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            storageService.completeJob(job.getId(), finalOutputName, finalFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Image processing error", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/document")
    public ResponseEntity<ConversionJob> convertDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat
    ) {
        ConversionJob job = storageService.createJob("Document Conversion", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String baseName = getBaseName(file.getOriginalFilename());
            String srcExt = getExtension(file.getOriginalFilename()).toLowerCase();
            String tgt = targetFormat.toLowerCase();

            String outputName = baseName + "_converted." + tgt;
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            if (srcExt.equals("docx") && tgt.equals("txt")) {
                documentConversionService.docxToTxt(inputFile, outputFile);
            } else if (srcExt.equals("docx") && tgt.equals("html")) {
                documentConversionService.docxToHtml(inputFile, outputFile);
            } else if (srcExt.equals("txt") && tgt.equals("docx")) {
                documentConversionService.txtToDocx(inputFile, outputFile);
            } else if ((srcExt.equals("md") || srcExt.equals("markdown")) && tgt.equals("html")) {
                documentConversionService.markdownToHtml(inputFile, outputFile);
            } else if (srcExt.equals("html") && (tgt.equals("md") || tgt.equals("markdown"))) {
                documentConversionService.htmlToMarkdown(inputFile, outputFile);
            } else {
                throw new IllegalArgumentException("Unsupported document conversion from " + srcExt + " to " + tgt);
            }

            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Document conversion failed", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    private String getBaseName(String filename) {
        if (filename == null) return "file";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }
}
