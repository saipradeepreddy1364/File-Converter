package com.fileconverter.controller;

import com.fileconverter.service.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(originPatterns = "*")
public class HealthController {

    private final StorageService storageService;
    private final Instant startTime = Instant.now();

    @Autowired
    public HealthController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("serviceName", "FileConverter Enterprise Platform");
        status.put("version", "1.0.0-PROD");
        status.put("startedAt", startTime.toString());
        status.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);

        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory();
        long totalMem = runtime.totalMemory();
        long freeMem = runtime.freeMemory();
        long usedMem = totalMem - freeMem;

        Map<String, Object> mem = new LinkedHashMap<>();
        mem.put("usedMB", usedMem / (1024 * 1024));
        mem.put("totalMB", totalMem / (1024 * 1024));
        mem.put("maxMB", maxMem / (1024 * 1024));
        status.put("jvmMemory", mem);

        File storageRoot = storageService.getRootLocation().toFile();
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("tempDirLocation", storageRoot.getAbsolutePath());
        storage.put("usableSpaceMB", storageRoot.getUsableSpace() / (1024 * 1024));
        storage.put("activeTempFiles", storageRoot.list() != null ? storageRoot.list().length : 0);
        status.put("storage", storage);

        status.put("ocrEngine", "Online & Ready (In-House Multi-Font Prototype Centroids Loaded)");
        return ResponseEntity.ok(status);
    }
}
