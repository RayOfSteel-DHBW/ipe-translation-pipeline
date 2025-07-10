package com.translation.steps;

import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.pipeline.PipelineStepBase;
import com.translation.util.IpeWrapper;

import java.io.File;

public class DecompileStep extends PipelineStepBase {
    private final IpeWrapper ipeWrapper;
    private final Configuration configuration;

    @Inject
    public DecompileStep(IpeWrapper ipeWrapper, Configuration configuration) {
        super(1, "IPE Decompilation"); // Default to order 1, name can be overridden
        this.ipeWrapper = ipeWrapper;
        this.configuration = configuration;
    }

    public DecompileStep(int order, String stepName, IpeWrapper ipeWrapper, Configuration configuration) {
        super(order, stepName);
        this.ipeWrapper = ipeWrapper;
        this.configuration = configuration;
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Decompiling IPE files from: " + getInputDirectory().getAbsolutePath());
        
        File[] pdfFiles = getInputDirectory().listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        
        if (pdfFiles == null || pdfFiles.length == 0) {
            throw new Exception("No PDF files found in input directory: " + getInputDirectory().getAbsolutePath());
        }
        
        int successCount = 0;
        for (File pdfFile : pdfFiles) {
            String baseName = pdfFile.getName().replaceAll("\\.[^.]*$", "");
            File outputFile = new File(getOutputDirectory(), baseName + ".xml");
            
            logger.info("Extracting IPE XML from: " + pdfFile.getName() + " -> " + outputFile.getName());
            
            if (ipeWrapper.extractXml(pdfFile, outputFile)) {
                successCount++;
            } else {
                logger.warning("Failed to extract IPE XML from: " + pdfFile.getName());
            }
        }
        
        if (successCount == 0) {
            throw new Exception("No IPE XML could be extracted from any PDF files");
        }
        
        logger.info("Successfully extracted IPE XML from " + successCount + " of " + pdfFiles.length + " PDF files to: " + getOutputDirectory().getAbsolutePath());
    }
}
