package com.translation.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.util.XmlProcessor;

import java.util.List;

public class TextExtractionStep extends PipelineStepBase {
    private final XmlProcessor xmlProcessor;
    private final String xmlFilePath;
    private final String structureOutputPath;
    private final String textOutputPath;

    public TextExtractionStep(int order, String xmlFilePath, String structureOutputPath, String textOutputPath) {
        super(order, "Text Extraction");
        this.xmlFilePath = xmlFilePath;
        this.structureOutputPath = structureOutputPath;
        this.textOutputPath = textOutputPath;
        this.xmlProcessor = new XmlProcessor();
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Extracting text from XML file: " + xmlFilePath);
        
        String xmlContent = com.translation.util.FileManager.readFile(xmlFilePath);
        
        List<String> extractedTexts = xmlProcessor.extractText(xmlContent);
        logger.info("Extracted " + extractedTexts.size() + " text elements");
        
        xmlProcessor.writeStructureWithPlaceholders(xmlContent, structureOutputPath);
        logger.info("Structure with placeholders written to: " + structureOutputPath);
        
        String textContent = String.join("\n", extractedTexts);
        com.translation.util.FileManager.writeFile(textOutputPath, textContent);
        logger.info("Text content written to: " + textOutputPath);
    }
}
