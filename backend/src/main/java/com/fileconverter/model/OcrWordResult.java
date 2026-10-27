package com.fileconverter.model;

public class OcrWordResult {
    private String text;
    private double confidence;
    private BoundingBox boundingBox;

    public OcrWordResult() {}

    public OcrWordResult(String text, double confidence, BoundingBox boundingBox) {
        this.text = text;
        this.confidence = confidence;
        this.boundingBox = boundingBox;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public BoundingBox getBoundingBox() { return boundingBox; }
    public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
}
