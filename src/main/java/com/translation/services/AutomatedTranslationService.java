package com.translation.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AutomatedTranslationService implements TranslationService {
    private static final Logger logger = Logger.getLogger(AutomatedTranslationService.class.getName());
    
    private final String apiKey;
    private final String targetLanguage;

    public AutomatedTranslationService(String apiKey) throws IOException {
        this.apiKey = Files.readString(Paths.get("C:\\Dev\\Repos\\Remotes\\JavaProject\\api.key")).trim();
        this.targetLanguage = "EN";
    }

    @Override
    public void translate(String inputFilePath, String outputFilePath) throws Exception {
        logger.info("Starting automated translation from " + inputFilePath + " to " + outputFilePath);
        
        List<String> lines = readLinesFromFile(inputFilePath);
        List<String> translatedLines = new ArrayList<>();
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                translatedLines.add(line);
                continue;
            }
            
            String translatedLine = translateWithDeepL(line.trim());
            translatedLines.add(translatedLine);
        }
        
        writeLinesToFile(translatedLines, outputFilePath);
        logger.info("Translation completed successfully");
    }
    
    private String translateWithDeepL(String text) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        String requestBody = String.format(
            "{\"text\": [\"%s\"], \"target_lang\": \"%s\"}",
            text.replace("\"", "\\\""),
            targetLanguage
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.deepl.com/v2/translate"))
            .header("Content-Type", "application/json")
            .header("Authorization", "DeepL-Auth-Key " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new Exception("DeepL API request failed with status: " + response.statusCode());
        }
        
        return parseTranslationResponse(response.body());
    }
    
    private String parseTranslationResponse(String jsonResponse) {
        int textStart = jsonResponse.indexOf("\"text\":\"") + 8;
        int textEnd = jsonResponse.indexOf("\"", textStart);
        return jsonResponse.substring(textStart, textEnd);
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