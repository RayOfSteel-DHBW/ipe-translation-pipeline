package com.translation.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {
    @JsonProperty("ipe_dir")
    private String ipeDir;
    
    @JsonProperty("ipe_executable")
    private String ipeExecutable = "ipeextract";
    
    @JsonProperty("pipeline")
    private PipelineConfig pipeline = new PipelineConfig();
    
    @JsonProperty("translation")
    private TranslationConfig translation = new TranslationConfig();
    
    @JsonProperty("download")
    private DownloadConfig download = new DownloadConfig();
    
    // Getters and setters
    public String getIpeDir() {
        return ipeDir;
    }
    
    public void setIpeDir(String ipeDir) {
        this.ipeDir = ipeDir;
    }
    
    public String getIpeExecutable() {
        return ipeExecutable;
    }
    
    public void setIpeExecutable(String ipeExecutable) {
        this.ipeExecutable = ipeExecutable;
    }
    
    public PipelineConfig getPipeline() {
        return pipeline;
    }
    
    public void setPipeline(PipelineConfig pipeline) {
        this.pipeline = pipeline;
    }
    
    public TranslationConfig getTranslation() {
        return translation;
    }
    
    public void setTranslation(TranslationConfig translation) {
        this.translation = translation;
    }
    
    public DownloadConfig getDownload() {
        return download;
    }
    
    public void setDownload(DownloadConfig download) {
        this.download = download;
    }
    
    // Helper method to get full IPE executable path
    public String getIpeExecutablePath() {
        if (ipeDir == null || ipeDir.isEmpty()) {
            return ipeExecutable;
        }
        return ipeDir + java.io.File.separator + ipeExecutable;
    }
    
    public static class PipelineConfig {
        @JsonProperty("working_directory")
        private String workingDirectory = ".work";
        
        @JsonProperty("clean_on_start")
        private boolean cleanOnStart = false;
        
        public String getWorkingDirectory() {
            return workingDirectory;
        }
        
        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }
        
        public boolean isCleanOnStart() {
            return cleanOnStart;
        }
        
        public void setCleanOnStart(boolean cleanOnStart) {
            this.cleanOnStart = cleanOnStart;
        }
    }
    
    public static class TranslationConfig {
        @JsonProperty("source_language")
        private String sourceLanguage = "en";
        
        @JsonProperty("target_language")
        private String targetLanguage = "de";
        
        @JsonProperty("service")
        private String service = "automated";
        
        public String getSourceLanguage() {
            return sourceLanguage;
        }
        
        public void setSourceLanguage(String sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
        }
        
        public String getTargetLanguage() {
            return targetLanguage;
        }
        
        public void setTargetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
        }
        
        public String getService() {
            return service;
        }
        
        public void setService(String service) {
            this.service = service;
        }
    }
    
    public static class DownloadConfig {
        @JsonProperty("base_url")
        private String baseUrl;
        
        @JsonProperty("timeout_seconds")
        private int timeoutSeconds = 30;
        
        @JsonProperty("max_retries")
        private int maxRetries = 3;
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }
        
        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }
}
