package com.fileconverter.controller;

import com.fileconverter.service.intelligence.DocumentIntelligenceService;
import com.fileconverter.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class DocumentIntelligenceController {

    private static final Logger log = LoggerFactory.getLogger(DocumentIntelligenceController.class);

    private final DocumentIntelligenceService intelligenceService;
    private final StorageService storageService;

    @Autowired
    public DocumentIntelligenceController(DocumentIntelligenceService intelligenceService, StorageService storageService) {
        this.intelligenceService = intelligenceService;
        this.storageService = storageService;
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, Object>> summarize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sentences", defaultValue = "5") int sentences
    ) {
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();
            Map<String, Object> summary = intelligenceService.summarizeDocument(inputFile, sentences);
            summary.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("AI Summarization failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/qa")
    public ResponseEntity<Map<String, Object>> answerQuestion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question
    ) {
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();
            Map<String, Object> answer = intelligenceService.answerQuestion(inputFile, question);
            answer.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            log.error("AI Q&A failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/entities")
    public ResponseEntity<Map<String, Object>> extractEntities(@RequestParam("file") MultipartFile file) {
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();
            Map<String, Object> entities = intelligenceService.extractEntities(inputFile);
            entities.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(entities);
        } catch (Exception e) {
            log.error("Entity extraction failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/study-notes")
    public ResponseEntity<Map<String, Object>> generateStudyNotes(@RequestParam("file") MultipartFile file) {
        try {
            String stored = storageService.store(file);
            File inputFile = storageService.load(stored).toFile();
            Map<String, Object> notes = intelligenceService.generateStudyNotes(inputFile);
            return ResponseEntity.ok(notes);
        } catch (Exception e) {
            log.error("Study notes generation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
