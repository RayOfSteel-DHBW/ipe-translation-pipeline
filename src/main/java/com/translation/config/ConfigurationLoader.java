package com.translation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;

@Singleton
public class ConfigurationLoader {
    private static final String CONFIG_FILE = "settings.json";
    private final ObjectMapper objectMapper;
    
    public ConfigurationLoader() {
        this.objectMapper = new ObjectMapper();
    }
    
    public Configuration loadConfiguration() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IOException("Configuration file '" + CONFIG_FILE + "' not found in resources");
            }
            
            Configuration config = objectMapper.readValue(inputStream, Configuration.class);
            
            validateConfiguration(config);
            
            return config;
        }
    }
    
    private void validateConfiguration(Configuration config) throws IOException {
        if (config.getIpeDir() == null || config.getIpeDir().trim().isEmpty()) {
            throw new IOException("Configuration error: 'ipe_dir' is required and cannot be empty");
        }
        
        if (config.getIpeExtract() == null || config.getIpeExtract().trim().isEmpty()) {
            throw new IOException("Configuration error: 'ipe_extract' is required and cannot be empty");
        }
    }
}
