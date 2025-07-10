package com.translation.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.services.TranslationService;
import java.io.File;

public class TranslationStep extends PipelineStepBase {
    private final TranslationService translationService;

    public TranslationStep(int order, TranslationService translationService) {
        super(order, "Translation");
        this.translationService = translationService;
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Processing translation from " + getInputDirectory() + " to " + getOutputDirectory());
        
        // Find .txt files in the input directory
        File[] txtFiles = getInputDirectory().listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        
        if (txtFiles == null || txtFiles.length == 0) {
            throw new Exception("No .txt files found in input directory: " + getInputDirectory().getAbsolutePath());
        }
        
        for (File txtFile : txtFiles) {
            String baseName = txtFile.getName().replaceAll("\\.[^.]*$", "");
            File outputFile = new File(getOutputDirectory(), baseName + ".txt");
            
            logger.info("Translating: " + txtFile.getName() + " -> " + outputFile.getName());
            translationService.translate(txtFile.getAbsolutePath(), outputFile.getAbsolutePath());
        }
    }

    public TranslationService getTranslationService() {
        return translationService;
    }
}