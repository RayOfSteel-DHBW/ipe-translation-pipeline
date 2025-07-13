package com.translation.pipeline.steps;

import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.extraction.ExtractionResult;
import com.translation.extraction.SmartTextExtractor;
import com.translation.util.FileManager;
import java.io.File;

public class TextExtractionStep extends PipelineStepBase {
    private static final int STEP_ORDER = 2;
    private final SmartTextExtractor textExtractor;

    // file-handling
    private static final String INPUT_EXT = ".xml";          // source XML from decompile
    private static final String OUTPUT_TEXT_EXT = ".txt";    // extracted text
    private static final String OUTPUT_STRUCT_EXT = ".xml";  // processed XML structure

    @Inject
    public TextExtractionStep(SmartTextExtractor textExtractor, Configuration configuration) {
        super("Text Extraction");
        this.textExtractor = textExtractor;
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

        File xmlFile = new File(getInputDirectory(), fileName + INPUT_EXT);
        if (!xmlFile.exists()) {
            logger.warning("XML file not found: " + xmlFile.getAbsolutePath() + " (likely previous step failed)");
            return false;
        }

        logger.info("Processing XML file: " + xmlFile.getName());

        getOutputDirectory().mkdirs();

        String xmlContent = FileManager.readFile(xmlFile.getAbsolutePath());
        ExtractionResult result = textExtractor.extractText(xmlContent);

        String baseName = fileName;

        File structureFile = new File(getOutputDirectory(), baseName + OUTPUT_STRUCT_EXT);
        FileManager.writeFile(structureFile.getAbsolutePath(), result.getProcessedXml());
        File simpleTextFile = new File(getOutputDirectory(), baseName + OUTPUT_TEXT_EXT);
        StringBuilder simpleTextBuilder = new StringBuilder();
        result.getTextElements().forEach(element -> {
            String escaped = element.getOriginalText()
                                   .replace("\r", "\\n")
                                   .replace("\n", "\\n");
            simpleTextBuilder.append("@(")
                             .append(element.getId())
                             .append("):")
                             .append(escaped)
                             .append("\n");
        });
        FileManager.writeFile(simpleTextFile.getAbsolutePath(), simpleTextBuilder.toString());

        logger.info("Successfully processed file: " + xmlFile.getName());
        return true;
    }
}