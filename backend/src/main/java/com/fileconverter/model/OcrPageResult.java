package com.fileconverter.model;

import java.util.ArrayList;
import java.util.List;

public class OcrPageResult {
    private int pageNumber;
    private int width;
    private int height;
    private String text;
    private double confidence;
    private List<OcrLineResult> lines = new ArrayList<>();

    public OcrPageResult() {}

    public OcrPageResult(int pageNumber, int width, int height, String text, double confidence, List<OcrLineResult> lines) {
        this.pageNumber = pageNumber;
        this.width = width;
        this.height = height;
        this.text = text;
        this.confidence = confidence;
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public List<OcrLineResult> getLines() { return lines; }
    public void setLines(List<OcrLineResult> lines) { this.lines = lines; }
}
