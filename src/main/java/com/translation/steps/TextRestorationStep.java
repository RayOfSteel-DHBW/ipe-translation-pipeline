package com.translation.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.util.XmlProcessor;
import com.translation.util.FileManager;
import java.io.File;

public class TextRestorationStep extends PipelineStepBase {
    private final XmlProcessor xmlProcessor;

    public TextRestorationStep(int order) {
        super(order, "Text Restoration");
        this.xmlProcessor = new XmlProcessor();
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Restoring translated text into XML structure");
        
        // Find the structure files (.xml) in the input directory
        File[] structureFiles = getInputDirectory().listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        
        if (structureFiles == null || structureFiles.length == 0) {
            throw new Exception("No XML structure files found in input directory: " + getInputDirectory().getAbsolutePath());
        }
        
        // Find the translated text files (.txt) in the input directory
        File[] translatedFiles = getInputDirectory().listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        
        if (translatedFiles == null || translatedFiles.length == 0) {
            throw new Exception("No translated text files found in input directory: " + getInputDirectory().getAbsolutePath());
        }
        
        // Process each structure file
        for (File structureFile : structureFiles) {
            String baseName = structureFile.getName().replaceAll("\\.[^.]*$", "");
            
            // Find corresponding translated text file
            File translatedFile = null;
            for (File txtFile : translatedFiles) {
                String txtBaseName = txtFile.getName().replaceAll("\\.[^.]*$", "");
                if (txtBaseName.equals(baseName)) {
                    translatedFile = txtFile;
                    break;
                }
            }
            
            if (translatedFile == null) {
                logger.warning("No translated text file found for: " + structureFile.getName());
                continue;
            }
            
            File outputFile = new File(getOutputDirectory(), baseName + ".xml");
            
            logger.info("Restoring: " + structureFile.getName() + " + " + translatedFile.getName() + " -> " + outputFile.getName());
            
            // Read structure content
            String structureContent = FileManager.readFile(structureFile.getAbsolutePath());
            
            // Create temp file for processing
            File tempFile = new File(getOutputDirectory(), baseName + "_temp.xml");
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
    }
    
    private void changeDocumentLanguage(String filePath) throws Exception {
        logger.info("Applying LaTeX language switching optimizations");
        
        String content = FileManager.readFile(filePath);
        
        content = content.replace("\\germantrue", "\\germanfalse");
        
        FileManager.writeFile(filePath, content);
        logger.info("LaTeX language switching completed");
    }
}
