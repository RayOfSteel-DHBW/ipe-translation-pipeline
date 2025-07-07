package com.translation.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.services.TranslationService;
import java.nio.file.Paths;

public class TranslationStep extends PipelineStepBase {
    private final TranslationService translationService;
    private final String inputFile;

    public TranslationStep(int order, TranslationService translationService, String inputFile) {
        super(order, "Translation");
        this.translationService = translationService;
        this.inputFile = inputFile;
    }

    @Override
    protected void performAction() throws Exception {
        String outputFile = generateOutputPath();
        logger.info("Processing translation from " + inputFile + " to " + outputFile);
        translationService.translate(inputFile, outputFile);
    }

    private String generateOutputPath() {
        String repoRoot = System.getProperty("user.dir");
        String stepPrefix = String.format("%02d", getOrder());
        String stepName = getStepName().toLowerCase().replace(" ", "-");
        return Paths.get(repoRoot, ".work", stepPrefix + "-" + stepName + ".txt").toString();
    }

    public String getInputFile() {
        return inputFile;
    }

    public TranslationService getTranslationService() {
        return translationService;
    }
}