package com.translation.steps;

import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.pipeline.PipelineStepBase;
import com.translation.util.IpeWrapper;
import java.io.File;

public class CompileStep extends PipelineStepBase {
    private final IpeWrapper ipeWrapper;

    @Inject
    public CompileStep(IpeWrapper ipeWrapper, Configuration configuration) {
        super(5, "IPE Compilation");
        this.ipeWrapper = ipeWrapper;
    }

    public CompileStep(int order, IpeWrapper ipeWrapper) {
        super(order, "IPE Compilation");
        this.ipeWrapper = ipeWrapper;
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Compiling XML files from: " + getInputDirectory().getAbsolutePath());
        
        // Find XML files in the input directory
        File[] xmlFiles = getInputDirectory().listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        
        if (xmlFiles == null || xmlFiles.length == 0) {
            throw new Exception("No XML files found in input directory: " + getInputDirectory().getAbsolutePath());
        }
        
        int successCount = 0;
        for (File xmlFile : xmlFiles) {
            String baseName = xmlFile.getName().replaceAll("\\.[^.]*$", "");
            File outputFile = new File(getOutputDirectory(), baseName + ".pdf");
            
            logger.info("Compiling: " + xmlFile.getName() + " -> " + outputFile.getName());
            
            try {
                ipeWrapper.compile(xmlFile.getAbsolutePath(), outputFile.getAbsolutePath());
                successCount++;
            } catch (Exception e) {
                logger.warning("Failed to compile: " + xmlFile.getName() + " - " + e.getMessage());
            }
        }
        
        logger.info("Successfully compiled " + successCount + " out of " + xmlFiles.length + " XML files");
        
        if (successCount == 0) {
            throw new Exception("Failed to compile any XML files");
        }
    }
}
