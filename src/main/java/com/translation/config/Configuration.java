package com.translation.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {
    @JsonProperty("ipe_dir")
    private String ipeDir;
    
    @JsonProperty("working_directory")
    private String workingDirectory = ".work";
    
    @JsonProperty("clean_on_start")
    private boolean cleanOnStart = false;
    
    public String getIpeDir() {
        return ipeDir;
    }
    
    public void setIpeDir(String ipeDir) {
        this.ipeDir = ipeDir;
    }
    
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
    
    public String getIpeExtractPath() {
        if (ipeDir == null || ipeDir.isEmpty()) {
            return "ipeextract.exe";
        }
        return ipeDir + java.io.File.separator + "ipeextract.exe";
    }
    
    public String getIpe2ipePath() {
        if (ipeDir == null || ipeDir.isEmpty()) {
            return "ipe2ipe.exe";
        }
        return ipeDir + java.io.File.separator + "ipe2ipe.exe";
    }
}
