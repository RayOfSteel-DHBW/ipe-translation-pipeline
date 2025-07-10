package com.translation.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {
    @JsonProperty("ipe_dir")
    private String ipeDir;
    
    @JsonProperty("ipe_extract")
    private String ipeExtract = "ipeextract.exe";
    
    @JsonProperty("ipe2ipe")
    private String ipe2ipe = "ipe2ipe.exe";
    
    @JsonProperty("pipeline")
    private PipelineConfig pipeline = new PipelineConfig();
    
    @JsonProperty("translation")
    private TranslationConfig translation = new TranslationConfig();
    
    @JsonProperty("download")
    private DownloadConfig download = new DownloadConfig();
    
    public String getIpeDir() {
        return ipeDir;
    }
    
    public void setIpeDir(String ipeDir) {
        this.ipeDir = ipeDir;
    }
    
    public String getIpeExtract() {
        return ipeExtract;
    }
    
    public void setIpeExtract(String ipeExtract) {
        this.ipeExtract = ipeExtract;
    }
    
    public String getIpe2ipe() {
        return ipe2ipe;
    }
    
    public void setIpe2ipe(String ipe2ipe) {
        this.ipe2ipe = ipe2ipe;
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
    
    // Helper methods to get full executable paths
    public String getIpeExtractPath() {
        if (ipeDir == null || ipeDir.isEmpty()) {
            return ipeExtract;
        }
        return ipeDir + java.io.File.separator + ipeExtract;
    }
    
    public String getIpe2ipePath() {
        if (ipeDir == null || ipeDir.isEmpty()) {
            return ipe2ipe;
        }
        return ipeDir + java.io.File.separator + ipe2ipe;
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
