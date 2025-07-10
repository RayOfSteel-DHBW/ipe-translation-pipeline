package com.translation.extraction;

import java.util.List;
import java.util.ArrayList;

public class ExtractionResult {
    private String originalXml;
    private String processedXml;
    private List<TextElement> textElements;
    private ExtractionStats stats;
    
    public ExtractionResult() {
        this.textElements = new ArrayList<>();
        this.stats = new ExtractionStats();
    }
    
    public String getOriginalXml() {
        return originalXml;
    }
    
    public void setOriginalXml(String originalXml) {
        this.originalXml = originalXml;
    }
    
    public String getProcessedXml() {
        return processedXml;
    }
    
    public void setProcessedXml(String processedXml) {
        this.processedXml = processedXml;
    }
    
    public List<TextElement> getTextElements() {
        return textElements;
    }
    
    public void setTextElements(List<TextElement> textElements) {
        this.textElements = textElements;
    }
    
    public void addTextElement(TextElement element) {
        this.textElements.add(element);
    }
    
    public ExtractionStats getStats() {
        return stats;
    }
    
    public void setStats(ExtractionStats stats) {
        this.stats = stats;
    }
    
    public static class ExtractionStats {
        private int totalElementsScanned;
        private int elementsSkipped;
        private int translatableFound;
        private double noiseReductionPercent;
        
        public int getTotalElementsScanned() {
            return totalElementsScanned;
        }
        
        public void setTotalElementsScanned(int totalElementsScanned) {
            this.totalElementsScanned = totalElementsScanned;
        }
        
        public int getElementsSkipped() {
            return elementsSkipped;
        }
        
        public void setElementsSkipped(int elementsSkipped) {
            this.elementsSkipped = elementsSkipped;
        }
        
        public int getTranslatableFound() {
            return translatableFound;
        }
        
        public void setTranslatableFound(int translatableFound) {
            this.translatableFound = translatableFound;
        }
        
        public double getNoiseReductionPercent() {
            return noiseReductionPercent;
        }
        
        public void setNoiseReductionPercent(double noiseReductionPercent) {
            this.noiseReductionPercent = noiseReductionPercent;
        }
        
        public void calculateNoiseReduction() {
            if (totalElementsScanned > 0) {
                this.noiseReductionPercent = ((double) elementsSkipped / totalElementsScanned) * 100.0;
            }
        }
    }
}
