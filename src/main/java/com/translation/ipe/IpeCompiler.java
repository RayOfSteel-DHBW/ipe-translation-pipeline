package com.translation.ipe;

public class IpeCompiler {
    public void compile(String xmlFilePath, String outputFilePath) throws CompilerException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ipe2ipe", "-pdf", xmlFilePath, outputFilePath);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new CompilerException("Compilation failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            throw new CompilerException("An error occurred during compilation: " + e.getMessage(), e);
        }
    }
}