package com.fileconverter.model;

import java.util.ArrayList;
import java.util.List;

public class OcrLineResult {
    private String text;
    private double confidence;
    private BoundingBox boundingBox;
    private List<OcrWordResult> words = new ArrayList<>();

    public OcrLineResult() {}

    public OcrLineResult(String text, double confidence, BoundingBox boundingBox, List<OcrWordResult> words) {
        this.text = text;
        this.confidence = confidence;
        this.boundingBox = boundingBox;
        this.words = words != null ? words : new ArrayList<>();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public BoundingBox getBoundingBox() { return boundingBox; }
    public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }

    public List<OcrWordResult> getWords() { return words; }
    public void setWords(List<OcrWordResult> words) { this.words = words; }
}
