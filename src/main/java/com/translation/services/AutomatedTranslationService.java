package com.translation.services;

import com.google.gson.Gson;
import com.translation.Constants;
import okhttp3.*;
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
    private static final int REQUEST_DELAY_MS = 10000;
    
    private final String apiKey;
    private final String targetLanguage;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public AutomatedTranslationService() throws IOException {
        this.apiKey = Files.readString(Paths.get("C:\\Dev\\Repos\\Remotes\\JavaProject\\api.key")).trim();
        this.targetLanguage = Constants.TARGET_LANGUAGE;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    @Override
    public void translate(String inputFilePath, String outputFilePath) throws Exception {
        logger.info("Starting automated translation from " + inputFilePath + " to " + outputFilePath);
        
        List<String> lines = readLinesFromFile(inputFilePath);
        List<String> translatedLines = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                translatedLines.add(line);
                continue;
            }
            
            String translatedLine = translateWithDeepL(line.trim());
            translatedLines.add(translatedLine);
            
            // Add delay between requests to avoid rate limiting
            if (i < lines.size() - 1) { // Don't delay after the last request
                logger.info("Waiting " + REQUEST_DELAY_MS + "ms before next request to avoid rate limiting");
                try {
                    Thread.sleep(REQUEST_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Translation interrupted", e);
                }
            }
        }
        
        writeLinesToFile(translatedLines, outputFilePath);
        logger.info("Translation completed successfully");
    }
    
    private String translateWithDeepL(String text) throws Exception {
        DeepLRequest request = new DeepLRequest(new String[]{text}, targetLanguage);
        String requestBody = gson.toJson(request);
        
        RequestBody body = RequestBody.create(
            requestBody, 
            MediaType.get("application/json")
        );
        
        Request httpRequest = new Request.Builder()
            .url("https://api-free.deepl.com/v2/translate")
            .header("Authorization", "DeepL-Auth-Key " + apiKey)
            .post(body)
            .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                logger.severe("DeepL API request failed with status: " + response.code());
                logger.severe("Response body: " + responseBody);
                logger.severe("Request body: " + requestBody);
                logger.severe("API key length: " + apiKey.length());
                throw new Exception("DeepL API request failed with status: " + response.code() + " - " + responseBody);
            }
            
            String responseBody = response.body() != null ? response.body().string() : "";
            return parseTranslationResponse(responseBody);
        }
    }
    
    private String parseTranslationResponse(String jsonResponse) {
        // DeepL returns: {"translations":[{"detected_source_language":"EN","text":"Hallo Welt!"}]}
        int translationsStart = jsonResponse.indexOf("\"translations\":[{");
        if (translationsStart == -1) {
            throw new RuntimeException("Invalid DeepL response format");
        }
        
        int textStart = jsonResponse.indexOf("\"text\":\"", translationsStart) + 8;
        int textEnd = jsonResponse.indexOf("\"", textStart);
        
        if (textStart == 7 || textEnd == -1) { // 7 because indexOf returned -1 + 8
            throw new RuntimeException("Could not parse translation from DeepL response");
        }
        
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
    
    private static class DeepLRequest {
        private String[] text;
        private String target_lang;
        
        public DeepLRequest(String[] text, String target_lang) {
            this.text = text;
            this.target_lang = target_lang;
        }
    }
}