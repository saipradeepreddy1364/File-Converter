package com.fileconverter.model;

import java.util.ArrayList;
import java.util.List;

public class OcrResult {
    private String jobId;
    private String originalFileName;
    private String fullText;
    private double averageConfidence;
    private int pageCount;
    private long processingTimeMs;
    private String language;
    private String engineInfo;
    private boolean lowQualityWarning;
    private List<OcrPageResult> pages = new ArrayList<>();
    private Double characterErrorRate; // CER if evaluated
    private Double wordErrorRate;      // WER if evaluated

    public OcrResult() {}

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public double getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getEngineInfo() { return engineInfo; }
    public void setEngineInfo(String engineInfo) { this.engineInfo = engineInfo; }

    public boolean isLowQualityWarning() { return lowQualityWarning; }
    public void setLowQualityWarning(boolean lowQualityWarning) { this.lowQualityWarning = lowQualityWarning; }

    public List<OcrPageResult> getPages() { return pages; }
    public void setPages(List<OcrPageResult> pages) { this.pages = pages; }

    public Double getCharacterErrorRate() { return characterErrorRate; }
    public void setCharacterErrorRate(Double characterErrorRate) { this.characterErrorRate = characterErrorRate; }

    public Double getWordErrorRate() { return wordErrorRate; }
    public void setWordErrorRate(Double wordErrorRate) { this.wordErrorRate = wordErrorRate; }
}
