package com.fileconverter.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ConversionJob {
    private String id;
    private String toolName;
    private JobStatus status = JobStatus.PENDING;
    private String message = "Initializing";
    private int progress = 0;
    private String originalFileName;
    private String outputFileName;
    private String downloadUrl;
    private long outputSizeBytes;
    private long originalSizeBytes;
    private double compressionRatio;
    private String error;
    private Instant createdAt = Instant.now();
    private Instant completedAt;
    private Map<String, Object> metadata = new HashMap<>();

    public ConversionJob() {}

    public ConversionJob(String id, String toolName, String originalFileName) {
        this.id = id;
        this.toolName = toolName;
        this.originalFileName = originalFileName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public long getOutputSizeBytes() { return outputSizeBytes; }
    public void setOutputSizeBytes(long outputSizeBytes) { this.outputSizeBytes = outputSizeBytes; }

    public long getOriginalSizeBytes() { return originalSizeBytes; }
    public void setOriginalSizeBytes(long originalSizeBytes) { this.originalSizeBytes = originalSizeBytes; }

    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double compressionRatio) { this.compressionRatio = compressionRatio; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
