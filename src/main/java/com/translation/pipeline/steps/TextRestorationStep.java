package com.translation.pipeline.steps;

import com.google.inject.Inject;
import com.translation.util.FileManager;
import java.io.File;

public class TextRestorationStep extends PipelineStepBase {
    private static final int STEP_ORDER = 4;

    private static final String STRUCTURE_EXT = ".xml";
    private static final String TRANSLATED_TEXT_EXT = ".txt";
    private static final String OUTPUT_EXT = ".xml";

    @Inject
    public TextRestorationStep() {
        super("Text Restoration");
    }

    @Override
    protected int getStepOrder() {
        return STEP_ORDER;
    }

    @Override
    protected boolean performAction(String fileName) throws Exception {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new Exception("Single-file mode: filename (without extension) must be provided");
        }

        File structureFile = new File("c:\\Dev\\Repos\\Remotes\\JavaProject\\ipe-translation-pipeline\\.work\\step-2", fileName + STRUCTURE_EXT);
        if (!structureFile.exists()) {
            logger.warning("Structure file not found: " + structureFile.getAbsolutePath() + " (likely previous step failed)");
            return false;
        }

        File translatedFile = new File("c:\\Dev\\Repos\\Remotes\\JavaProject\\ipe-translation-pipeline\\.work\\step-3", fileName + TRANSLATED_TEXT_EXT);
        if (!translatedFile.exists()) {
            logger.warning("Translated text file not found: " + translatedFile.getAbsolutePath() + " (likely previous step failed)");
            return false;
        }

        File outputFile = new File(getOutputDirectory(), fileName + OUTPUT_EXT);
        logger.info("Restoring: " + structureFile.getName() + " + " + translatedFile.getName() + " -> " + outputFile.getName());

        String structureContent = FileManager.readFile(structureFile.getAbsolutePath());
        String translatedContent = FileManager.readFile(translatedFile.getAbsolutePath());
        
        String restoredContent = restoreTranslatedText(structureContent, translatedContent);
        
        FileManager.writeFile(outputFile.getAbsolutePath(), restoredContent);

        changeDocumentLanguage(outputFile.getAbsolutePath());
        return true;
    }
    
    private String restoreTranslatedText(String structureContent, String translatedContent) {
        String[] translatedLines = translatedContent.split("\n");
        String result = structureContent;
        
        logger.info("Starting replacement with " + translatedLines.length + " translated lines");
        
        for (String line : translatedLines) {
            line = line.trim();
            if (line.startsWith("@(") && line.contains("):")) {
                int closeParenIndex = line.indexOf("):");
                String numberPart = line.substring(2, closeParenIndex);
                String translatedText = line.substring(closeParenIndex + 2);
                
                String placeholder = "@PLACEHOLDER(" + numberPart + ")@";
                logger.info("Replacing placeholder: " + placeholder + " with: " + translatedText.substring(0, Math.min(50, translatedText.length())));
                result = result.replace(placeholder, translatedText);
            }
        }
        
        boolean stillContainsPlaceholders = result.contains("@PLACEHOLDER(");
        logger.info("After replacement, still contains placeholders: " + stillContainsPlaceholders);
        
        return result;
    }

    private void changeDocumentLanguage(String filePath) throws Exception {
        logger.info("Applying LaTeX language switching optimizations");

        String content = FileManager.readFile(filePath);

        content = content.replace("\\germantrue", "\\germanfalse");

        FileManager.writeFile(filePath, content);
        logger.info("LaTeX language switching completed");
    }
}
