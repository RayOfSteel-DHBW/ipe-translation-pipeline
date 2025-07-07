package com.translation.translator;

import java.util.Scanner;
import java.util.logging.Logger;

public class ManualTranslationService implements TranslationService {
    private static final Logger logger = Logger.getLogger(ManualTranslationService.class.getName());
    private final Scanner scanner;

    public ManualTranslationService() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void translate(String inputFilePath, String outputFilePath) throws Exception {
        logger.info("Manual translation required:");
        logger.info("Input file: " + inputFilePath);
        logger.info("Output file: " + outputFilePath);
        
        System.out.println("\n=== MANUAL TRANSLATION REQUIRED ===");
        System.out.println("Please translate the content from:");
        System.out.println("  Source: " + inputFilePath);
        System.out.println("  Target: " + outputFilePath);
        System.out.println("\nPress ENTER when translation is complete...");
        
        scanner.nextLine();
        
        logger.info("Manual translation completed for: " + outputFilePath);
        System.out.println("Translation step completed!");
    }
}
