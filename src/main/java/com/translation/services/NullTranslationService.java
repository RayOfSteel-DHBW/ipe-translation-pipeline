package com.translation.services;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class NullTranslationService implements TranslationService {
    
    @Override
    public boolean translate(String inputFilePath, String outputFilePath) throws Exception {
        // No-op translation - just copy the input file to output file as-is
        Files.copy(Paths.get(inputFilePath), Paths.get(outputFilePath), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
