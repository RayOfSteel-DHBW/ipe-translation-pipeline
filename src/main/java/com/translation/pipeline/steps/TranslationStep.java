package com.translation.pipeline.steps;

import com.translation.services.TranslationService;
import java.io.File;

public class TranslationStep extends PipelineStepBase {
    private final TranslationService translationService;

    // file-handling
    private static final String INPUT_EXT = ".txt";
    private static final String OUTPUT_EXT = ".txt"; // translated text keeps same ext

    public TranslationStep(int order, TranslationService translationService) {
        super(order, "Translation");
        this.translationService = translationService;
    }

    @Override
    protected void performAction(String fileName) throws Exception {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new Exception("Single-file mode: filename (without extension) must be provided");
        }

        File txtFile = new File(getInputDirectory(), fileName + INPUT_EXT);
        if (!txtFile.exists()) {
            throw new Exception("Text file not found: " + txtFile.getAbsolutePath());
        }

        File outputFile = new File(getOutputDirectory(), fileName + OUTPUT_EXT);
        logger.info("Translating: " + txtFile.getName() + " -> " + outputFile.getName());

        translationService.translate(txtFile.getAbsolutePath(), outputFile.getAbsolutePath());
    }

    public TranslationService getTranslationService() {
        return translationService;
    }
}