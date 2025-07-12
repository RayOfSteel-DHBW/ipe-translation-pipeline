package com.translation.pipeline.steps;

import com.translation.util.XmlProcessor;
import com.translation.util.FileManager;
import java.io.File;

public class TextRestorationStep extends PipelineStepBase {
    private final XmlProcessor xmlProcessor;

    // file-handling
    private static final String STRUCTURE_EXT = "_structure.xml";
    private static final String TRANSLATED_TEXT_EXT = ".txt";
    private static final String OUTPUT_EXT = ".xml";

    public TextRestorationStep(int order) {
        super(order, "Text Restoration");
        this.xmlProcessor = new XmlProcessor();
    }

    @Override
    protected void performAction(String fileName) throws Exception {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new Exception("Single-file mode: filename (without extension) must be provided");
        }

        File structureFile = new File(getInputDirectory(), fileName + STRUCTURE_EXT);
        if (!structureFile.exists()) {
            throw new Exception("Structure file not found: " + structureFile.getAbsolutePath());
        }

        File translatedFile = new File(getInputDirectory(), fileName + TRANSLATED_TEXT_EXT);
        if (!translatedFile.exists()) {
            throw new Exception("Translated text file not found: " + translatedFile.getAbsolutePath());
        }

        File outputFile = new File(getOutputDirectory(), fileName + OUTPUT_EXT);
        logger.info("Restoring: " + structureFile.getName() + " + " + translatedFile.getName() + " -> " + outputFile.getName());

        // Read structure content
        String structureContent = FileManager.readFile(structureFile.getAbsolutePath());

        // Create temp file for processing
        File tempFile = new File(getOutputDirectory(), fileName + "_temp.xml");
        xmlProcessor.writeStructureWithPlaceholders(structureContent, tempFile.getAbsolutePath());

        // Restore translated text
        xmlProcessor.restoreTranslatedText(translatedFile.getAbsolutePath());

        // Write final XML
        xmlProcessor.writeXmlStructure(outputFile.getAbsolutePath(), null);

        // Clean up temp file
        if (tempFile.exists()) {
            tempFile.delete();
        }

        // Apply language changes
        changeDocumentLanguage(outputFile.getAbsolutePath());
    }

    private void changeDocumentLanguage(String filePath) throws Exception {
        logger.info("Applying LaTeX language switching optimizations");

        String content = FileManager.readFile(filePath);

        content = content.replace("\\germantrue", "\\germanfalse");

        FileManager.writeFile(filePath, content);
        logger.info("LaTeX language switching completed");
    }
}
