package com.translation.util;

import org.w3c.dom.Document;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlProcessor {

    private Document originalXml;
    private Map<String, String> placeholderMap = new HashMap<>();
    private String structureXml;

    public XmlProcessor(Document originalXml) {
        this.originalXml = originalXml;
    }

    public XmlProcessor() {
    }

    public List<String> extractText(String xmlContent) throws Exception {
        List<String> extractedTexts = new ArrayList<>();
        
        // Simple regex-based text extraction - looks for text content in XML
        Pattern textPattern = Pattern.compile(">([^<>]+)<", Pattern.MULTILINE);
        Matcher matcher = textPattern.matcher(xmlContent);
        
        while (matcher.find()) {
            String text = matcher.group(1).trim();
            if (isTranslatableText(text)) {
                extractedTexts.add(text);
            }
        }
        
        return extractedTexts;
    }

    public void writeStructureWithPlaceholders(String xmlContent, String outputPath) throws Exception {
        int placeholderCounter = 1;
        
        // Extract text and replace with placeholders
        Pattern textPattern = Pattern.compile(">([^<>]+)<", Pattern.MULTILINE);
        Matcher matcher = textPattern.matcher(xmlContent);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String text = matcher.group(1).trim();
            if (isTranslatableText(text)) {
                String placeholder = "PLACEHOLDER_" + placeholderCounter;
                placeholderMap.put(placeholder, text);
                matcher.appendReplacement(sb, ">" + placeholder + "<");
                placeholderCounter++;
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        
        this.structureXml = sb.toString();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(this.structureXml);
        } catch (IOException e) {
            throw new Exception("Error writing structure XML to file: " + outputPath, e);
        }
    }

    public void restoreTranslatedText(String translatedTextFilePath) throws Exception {
        List<String> translatedLines = Files.readAllLines(Paths.get(translatedTextFilePath));
        
        String restoredXml = this.structureXml;
        int lineIndex = 0;
        
        // Replace placeholders with translated text
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            String placeholder = entry.getKey();
            String translatedText = lineIndex < translatedLines.size() ? 
                translatedLines.get(lineIndex).trim() : entry.getValue();
            
            if (!translatedText.isEmpty()) {
                restoredXml = restoredXml.replace(placeholder, translatedText);
            } else {
                restoredXml = restoredXml.replace(placeholder, entry.getValue());
            }
            lineIndex++;
        }
        
        this.structureXml = restoredXml;
    }

    private boolean isTranslatableText(String text) {
        if (text == null || text.trim().isEmpty() || text.length() < 2) {
            return false;
        }
        
        // Skip XML attributes and tags
        if (text.contains("=") || text.startsWith("<") || text.endsWith(">")) {
            return false;
        }
        
        // Skip pure numbers and coordinates
        if (text.matches("^[\\d\\s\\.\\-,]+$")) {
            return false;
        }
        
        // Skip single characters unless they're meaningful
        if (text.length() == 1 && !text.matches("[a-zA-ZäöüÄÖÜß]")) {
            return false;
        }
        
        // Must contain some letters
        if (!text.matches(".*[a-zA-ZäöüÄÖÜßàáâãçèéêëìíîïñòóôõùúûüýÿ].*")) {
            return false;
        }
        
        return true;
    }

    public Document getReconstructedXml() {
        return originalXml;
    }

    public void setOriginalXml(Document originalXml) {
        this.originalXml = originalXml;
    }

    public void writeXmlStructure(String filePath, String xmlContent) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(xmlContent != null ? xmlContent : this.structureXml);
        } catch (IOException e) {
            throw new Exception("Error writing XML structure to file: " + filePath, e);
        }
    }

    public String getStructureXml() {
        return structureXml;
    }

    public Map<String, String> getPlaceholderMap() {
        return new HashMap<>(placeholderMap);
    }
}
