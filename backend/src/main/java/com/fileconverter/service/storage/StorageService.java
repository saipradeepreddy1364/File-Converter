package com.fileconverter.service.storage;

import com.fileconverter.model.ConversionJob;
import com.fileconverter.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Value("${fileconverter.storage.temp-dir:./temp-storage}")
    private String tempDirPath;

    @Value("${fileconverter.storage.cleanup-minutes:60}")
    private long cleanupMinutes;

    private Path rootLocation;
    private final Map<String, ConversionJob> jobRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(tempDirPath).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            log.info("Initialized temporary storage directory at: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    public Path getRootLocation() {
        return rootLocation;
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty or null file");
        }
        String originalFilename = file.getOriginalFilename();
        return store(file.getInputStream(), originalFilename);
    }

    public String store(InputStream inputStream, String originalFilename) throws IOException {
        String cleanName = sanitizeFilename(originalFilename);
        String storedFileName = UUID.randomUUID().toString() + "_" + cleanName;
        Path targetPath = rootLocation.resolve(storedFileName).normalize();

        // Path traversal defense
        if (!targetPath.startsWith(rootLocation)) {
            throw new SecurityException("Cannot store file outside current storage directory");
        }

        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return storedFileName;
    }

    public String store(byte[] content, String originalFilename) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            return store(bais, originalFilename);
        }
    }

    public Path load(String filename) {
        Path filePath = rootLocation.resolve(filename).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new SecurityException("Access to path outside storage directory is forbidden");
        }
        return filePath;
    }

    public Resource loadAsResource(String filename) throws FileNotFoundException {
        Path file = load(filename);
        if (Files.exists(file) && Files.isReadable(file)) {
            return new FileSystemResource(file.toFile());
        } else {
            throw new FileNotFoundException("Requested file was not found or is unreadable: " + filename);
        }
    }

    public void delete(String filename) {
        try {
            Path file = load(filename);
            Files.deleteIfExists(file);
        } catch (Exception e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }

    public ConversionJob createJob(String toolName, String originalFileName) {
        String id = UUID.randomUUID().toString();
        ConversionJob job = new ConversionJob(id, toolName, originalFileName);
        jobRegistry.put(id, job);
        return job;
    }

    public ConversionJob getJob(String jobId) {
        return jobRegistry.get(jobId);
    }

    public void updateJob(ConversionJob job) {
        jobRegistry.put(job.getId(), job);
    }

    public void completeJob(String jobId, String outputFileName, long outputSizeBytes) {
        ConversionJob job = jobRegistry.get(jobId);
        if (job != null) {
            job.setStatus(JobStatus.COMPLETED);
            job.setProgress(100);
            job.setMessage("Completed successfully");
            job.setOutputFileName(outputFileName);
            job.setOutputSizeBytes(outputSizeBytes);
            job.setDownloadUrl("/api/download/" + outputFileName);
            job.setCompletedAt(Instant.now());
            if (job.getOriginalSizeBytes() > 0 && outputSizeBytes > 0) {
                double ratio = 100.0 * (1.0 - ((double) outputSizeBytes / job.getOriginalSizeBytes()));
                job.setCompressionRatio(Math.round(ratio * 100.0) / 100.0);
            }
        }
    }

    public void failJob(String jobId, String errorMessage) {
        ConversionJob job = jobRegistry.get(jobId);
        if (job != null) {
            job.setStatus(JobStatus.FAILED);
            job.setMessage("Failed: " + errorMessage);
            job.setError(errorMessage);
            job.setCompletedAt(Instant.now());
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file.bin";
        String name = Paths.get(filename).getFileName().toString();
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Scheduled(fixedRate = 1800000) // run every 30 minutes
    public void cleanupOldFiles() {
        log.info("Starting automated storage cleanup for files older than {} minutes", cleanupMinutes);
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(cleanupMinutes));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootLocation)) {
            for (Path entry : stream) {
                try {
                    Instant lastModified = Files.getLastModifiedTime(entry).toInstant();
                    if (lastModified.isBefore(cutoff)) {
                        Files.deleteIfExists(entry);
                        log.debug("Cleaned up expired temporary file: {}", entry.getFileName());
                    }
                } catch (Exception ex) {
                    log.warn("Error cleaning file: {}", entry, ex);
                }
            }
        } catch (IOException e) {
            log.error("Error during scheduled storage cleanup", e);
        }
    }
}
