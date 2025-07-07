package com.translation.util;

import java.io.File;

public class IpeWrapper {

    public void decompile(File ipeFile, File outputXmlFile) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("ipe2ipe", "-xml", ipeFile.getAbsolutePath(), outputXmlFile.getAbsolutePath());
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Decompilation failed with exit code: " + exitCode);
        }
    }

    public void compile(String xmlFilePath, String outputFilePath) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("ipe2ipe", "-pdf", xmlFilePath, outputFilePath);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Compilation failed with exit code: " + exitCode);
        }
    }
}
