package com.translation.services;

import com.deepl.api.*;
import com.translation.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AutomatedTranslationService implements TranslationService {
    private static final Logger logger = Logger.getLogger(AutomatedTranslationService.class.getName());
    private static final int INITIAL_RETRY_DELAY_MS = 5000; 
    private static final int RETRY_DELAY_INCREMENT_MS = 5000;
    private static final int MAX_BATCH_SIZE = 50;
    private static final int MAX_BATCH_CHARS = 90000;

    private final String apiKey;
    private final String targetLanguage;
    private final Translator translator;   // ← new

    public AutomatedTranslationService() throws IOException {
        this.apiKey = Files.readString(Paths.get("C:\\Dev\\Repos\\Remotes\\JavaProject\\api.key")).trim();
        this.targetLanguage = Constants.TARGET_LANGUAGE;
        this.translator = new Translator(apiKey);   // ← new
    }

    @Override
    public void translate(String inputFilePath, String outputFilePath) throws Exception {
        logger.info("Starting automated translation from " + inputFilePath + " to " + outputFilePath);
        
        List<String> lines = readLinesFromFile(inputFilePath);
        List<String> translatedLines = new ArrayList<>();
        
        List<String> batch = new ArrayList<>();
        int batchChars = 0;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                translatedLines.add(line);
                continue;
            }
            
            if (batchChars + line.length() > MAX_BATCH_CHARS || batch.size() >= MAX_BATCH_SIZE) {
                List<TextResult> results = translateBatchWithDeepL(batch);
                for (TextResult result : results) {
                    translatedLines.add(result.getText());
                }
                batch.clear();
                batchChars = 0;
                Thread.sleep(1000);
            }
            
            batch.add(line.trim());
            batchChars += line.length();
        }
        
        if (!batch.isEmpty()) {
            List<TextResult> results = translateBatchWithDeepL(batch);
            for (TextResult result : results) {
                translatedLines.add(result.getText());
            }
        }
        
        writeLinesToFile(translatedLines, outputFilePath);
        logger.info("Translation completed successfully");
    }
    
    private List<TextResult> translateBatchWithDeepL(List<String> texts) throws Exception {
        int retryCount = 0;
        while (true) {
            try {
                return translator.translateText(texts, null, targetLanguage);
            } catch (TooManyRequestsException e) {
                retryCount++;
                int delayMs = INITIAL_RETRY_DELAY_MS + (retryCount - 1) * RETRY_DELAY_INCREMENT_MS;
                logger.warning("DeepL rate limit hit - retry " + retryCount + " after " + delayMs + "ms");
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Translation interrupted", ie);
                }
            } catch (DeepLException e) {
                throw e;
            }
        }
    }
    
    private List<String> readLinesFromFile(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
    
    private void writeLinesToFile(List<String> lines, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}