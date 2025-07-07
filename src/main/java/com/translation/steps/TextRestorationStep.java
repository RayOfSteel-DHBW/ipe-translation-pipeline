package com.translation.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.util.XmlProcessor;

public class TextRestorationStep extends PipelineStepBase {
    private final XmlProcessor xmlProcessor;
    private final String structureFilePath;
    private final String translatedTextFilePath;
    private final String outputFilePath;

    public TextRestorationStep(int order, String structureFilePath, String translatedTextFilePath, String outputFilePath) {
        super(order, "Text Restoration");
        this.structureFilePath = structureFilePath;
        this.translatedTextFilePath = translatedTextFilePath;
        this.outputFilePath = outputFilePath;
        this.xmlProcessor = new XmlProcessor();
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Restoring translated text into XML structure");
        
        String translatedText = com.translation.util.FileManager.readFile(translatedTextFilePath);
        logger.info("Loaded translated text from: " + translatedTextFilePath);
        
        xmlProcessor.restoreTranslatedText(translatedText);
        logger.info("Text restoration completed");
        
        String structureContent = com.translation.util.FileManager.readFile(structureFilePath);
        xmlProcessor.writeXmlStructure(outputFilePath, structureContent);
        logger.info("Restored XML written to: " + outputFilePath);
    }
}
