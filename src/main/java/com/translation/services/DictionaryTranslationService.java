package com.translation.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryTranslationService implements TranslationService {
    private static final Logger logger = Logger.getLogger(DictionaryTranslationService.class.getName());
    private static final Pattern ID_PATTERN = Pattern.compile("@\\((\\d+)\\):(.*)");
    
    private final Gson gson;

    public DictionaryTranslationService() {
        this.gson = new Gson();
    }

    @Override
    public boolean translate(String inputFilePath, String outputFilePath) throws Exception {
        logger.fine("Starting dictionary translation from " + inputFilePath + " to " + outputFilePath);
        
        // inputFilePath is the text file to be translated
        // We need to find the corresponding JSON file in manual-work directory
        String jsonFilePath = getCorrespondingJsonFile(inputFilePath);
        
        // Load translation entries from the manual-work JSON file
        Map<String, TranslationEntry> jsonEntries = loadJsonEntries(jsonFilePath);
        
        // Load German text entries from the input file
        Map<String, String> textEntries = loadTextEntries(inputFilePath);

        // If any JSON entry is missing English translation, abort translation
        for (Map.Entry<String, TranslationEntry> entry : jsonEntries.entrySet()) {
            TranslationEntry translationEntry = entry.getValue();
            if (translationEntry.english == null || translationEntry.english.isEmpty()) {
                logger.warning("Empty or missing English translation for ID " + entry.getKey());
                return false;
            }
        }
        
        // Create translations by replacing German text with English from JSON
        Map<String, String> translatedEntries = new HashMap<>();
        
        for (Map.Entry<String, String> textEntry : textEntries.entrySet()) {
            String id = textEntry.getKey();
            String germanText = textEntry.getValue();
            
            // Look for matching JSON entry with English translation
            TranslationEntry jsonEntry = jsonEntries.get(id);
            
            if (jsonEntry != null && jsonEntry.english != null) {
                // Use the English translation from the JSON file
                translatedEntries.put(id, jsonEntry.english);
                logger.fine("Translated ID " + id + ": " + germanText + " -> " + jsonEntry.english);
            } else {
                // Keep German text as fallback
                translatedEntries.put(id, germanText);
                logger.warning("No English translation found for ID " + id + ", keeping German: " + germanText);
            }
        }
        
        // Write the translated text file
        writeTranslatedEntries(translatedEntries, outputFilePath);
        logger.fine("Translation completed successfully. Processed " + jsonEntries.size() + " entries.");
        return true;
    }
    
    private Map<String, TranslationEntry> loadJsonEntries(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        Type type = new TypeToken<Map<String, TranslationEntry>>(){}.getType();
        return gson.fromJson(content, type);
    }
    
    private String getCorrespondingJsonFile(String textFilePath) {
        Path textPath = Paths.get(textFilePath);
        String fileName = textPath.getFileName().toString();
        String baseName = fileName.replace(".txt", ".json");
        return Paths.get("manual-work", baseName).toString();
    }
    
    private Map<String, String> loadTextEntries(String textFilePath) throws IOException {
        Map<String, String> entries = new HashMap<>();
        
        if (!Files.exists(Paths.get(textFilePath))) {
            logger.warning("Text file not found: " + textFilePath);
            return entries;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(textFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                Matcher matcher = ID_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String id = matcher.group(1);
                    String content = matcher.group(2);
                    entries.put(id, content);
                } else {
                    logger.warning("Line doesn't match expected format: " + line);
                }
            }
        }
        
        logger.fine("Loaded " + entries.size() + " entries from " + textFilePath);
        return entries;
    }
    
    private void writeTranslatedEntries(Map<String, String> entries, String outputFilePath) throws IOException {
        try (var writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                writer.write("@(" + entry.getKey() + "):" + entry.getValue());
                writer.newLine();
            }
        }
    }
    
    // Inner class to represent translation entries
    private static class TranslationEntry {
        public String english;
    }
}
