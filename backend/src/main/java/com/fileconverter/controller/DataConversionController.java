package com.fileconverter.controller;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.service.data.DataConversionService;
import com.fileconverter.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(originPatterns = "*")
public class DataConversionController {

    private static final Logger log = LoggerFactory.getLogger(DataConversionController.class);

    private final DataConversionService dataConversionService;
    private final StorageService storageService;

    @Autowired
    public DataConversionController(DataConversionService dataConversionService, StorageService storageService) {
        this.dataConversionService = dataConversionService;
        this.storageService = storageService;
    }

    @PostMapping("/convert")
    public ResponseEntity<ConversionJob> convertData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat
    ) {
        ConversionJob job = storageService.createJob("Data Conversion", file.getOriginalFilename());
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();

            String name = file.getOriginalFilename().toLowerCase();
            String tgt = targetFormat.toLowerCase();

            String outputName = "data_" + UUID.randomUUID().toString().substring(0, 8) + "." + tgt;
            File outputFile = storageService.getRootLocation().resolve(outputName).toFile();

            if (name.endsWith(".csv") && tgt.equals("xlsx")) {
                dataConversionService.csvToXlsx(inputFile, outputFile);
            } else if ((name.endsWith(".xlsx") || name.endsWith(".xls")) && tgt.equals("csv")) {
                dataConversionService.xlsxToCsv(inputFile, 0, outputFile);
            } else if (name.endsWith(".csv") && tgt.equals("json")) {
                dataConversionService.csvToJson(inputFile, outputFile);
            } else if (name.endsWith(".json") && tgt.equals("csv")) {
                dataConversionService.jsonToCsv(inputFile, outputFile);
            } else if (name.endsWith(".xml") && tgt.equals("json")) {
                String xml = Files.readString(inputFile.toPath());
                String json = dataConversionService.xmlToJson(xml);
                Files.writeString(outputFile.toPath(), json);
            } else if (name.endsWith(".json") && tgt.equals("xml")) {
                String json = Files.readString(inputFile.toPath());
                String xml = dataConversionService.jsonToXml(json);
                Files.writeString(outputFile.toPath(), xml);
            } else {
                throw new IllegalArgumentException("Unsupported data conversion from " + name + " to " + tgt);
            }

            storageService.completeJob(job.getId(), outputName, outputFile.length());
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            log.error("Data conversion error", e);
            storageService.failJob(job.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(job);
        }
    }

    @PostMapping("/format/json")
    public ResponseEntity<String> formatJson(@RequestBody String rawJson) {
        try {
            return ResponseEntity.ok(dataConversionService.formatJson(rawJson));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid JSON syntax: " + e.getMessage());
        }
    }

    @PostMapping("/format/xml")
    public ResponseEntity<String> formatXml(@RequestBody String rawXml) {
        try {
            return ResponseEntity.ok(dataConversionService.formatXml(rawXml));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid XML syntax: " + e.getMessage());
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewSpreadsheet(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "rows", defaultValue = "25") int rows
    ) {
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();
            return ResponseEntity.ok(dataConversionService.previewSpreadsheet(inputFile, rows));
        } catch (Exception e) {
            log.error("Preview error", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
