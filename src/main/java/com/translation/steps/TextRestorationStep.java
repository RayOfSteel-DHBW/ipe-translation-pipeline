package com.translation.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.util.XmlProcessor;
import com.translation.util.FileManager;

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
        
        // Read the structure XML with placeholders
        String structureContent = FileManager.readFile(structureFilePath);
        logger.info("Loaded structure XML from: " + structureFilePath);
        
        // First, we need to recreate the placeholder mapping from the structure
        // This is a simplified approach - in a real implementation, we'd save/load the mapping
        xmlProcessor.writeStructureWithPlaceholders(structureContent, structureFilePath + ".temp");
        
        // Now restore the translated text
        xmlProcessor.restoreTranslatedText(translatedTextFilePath);
        logger.info("Restored translated text from: " + translatedTextFilePath);
        
        // Write the final restored XML
        xmlProcessor.writeXmlStructure(outputFilePath, null);
        logger.info("Restored XML written to: " + outputFilePath);
        
        // Apply LaTeX language switching optimizations
        applyLatexLanguageSwitching();
    }
    
    private void applyLatexLanguageSwitching() throws Exception {
        logger.info("Applying LaTeX language switching optimizations");
        
        String content = FileManager.readFile(outputFilePath);
        
        // Replace German LaTeX switches with English
        content = content.replace("\\germantrue", "\\germanfalse");
        
        // Add English language setting at the beginning if not present
        if (!content.contains("\\germanfalse") && content.contains("\\document")) {
            content = content.replace("\\begin{document}", "\\germanfalse\n\\begin{document}");
        }
        
        FileManager.writeFile(outputFilePath, content);
        logger.info("LaTeX language switching completed");
    }
}
