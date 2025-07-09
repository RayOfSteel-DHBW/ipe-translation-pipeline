package com.translation.util;

import com.google.inject.Inject;
import com.translation.config.Configuration;

import java.io.File;
import java.util.logging.Logger;

public class IpeWrapper {
    private static final Logger logger = Logger.getLogger(IpeWrapper.class.getName());
    private final Configuration configuration;

    @Inject
    public IpeWrapper(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean extractXml(File pdfFile, File outputXmlFile) {
        try {
            String ipeExtractPath = configuration.getIpeExecutablePath();
            ProcessBuilder processBuilder = new ProcessBuilder(ipeExtractPath, pdfFile.getAbsolutePath(), outputXmlFile.getAbsolutePath());
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && outputXmlFile.exists() && outputXmlFile.length() > 0) {
                return true;
            } else {
                if (outputXmlFile.exists()) {
                    outputXmlFile.delete();
                }
                return false;
            }
        } catch (Exception e) {
            logger.warning("Failed to extract XML from " + pdfFile.getName() + ": " + e.getMessage());
            if (outputXmlFile.exists()) {
                outputXmlFile.delete();
            }
            return false;
        }
    }

    public void decompile(File ipeFile, File outputXmlFile) throws Exception {
        String ipe2ipePath = configuration.getIpeDir() + File.separator + "ipe2ipe";
        ProcessBuilder processBuilder = new ProcessBuilder(ipe2ipePath, "-xml", ipeFile.getAbsolutePath(), outputXmlFile.getAbsolutePath());
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Decompilation failed with exit code: " + exitCode);
        }
    }

    public void compile(String xmlFilePath, String outputFilePath) throws Exception {
        String ipe2ipePath = configuration.getIpeDir() + File.separator + "ipe2ipe";
        ProcessBuilder processBuilder = new ProcessBuilder(ipe2ipePath, "-pdf", xmlFilePath, outputFilePath);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Compilation failed with exit code: " + exitCode);
        }
    }
}
