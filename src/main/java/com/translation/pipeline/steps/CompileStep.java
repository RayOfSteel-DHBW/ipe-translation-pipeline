package com.translation.pipeline.steps;

import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.util.IpeWrapper;
import java.io.File;

public class CompileStep extends PipelineStepBase {
    private final IpeWrapper ipeWrapper;

    // file-handling
    private static final String INPUT_EXT = ".xml";
    private static final String OUTPUT_EXT = ".pdf";

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
    protected void performAction(String fileName) throws Exception {
        if (fileName != null && !fileName.trim().isEmpty()) {
            // single-file compilation
            File xmlFile = new File(getInputDirectory(), fileName + INPUT_EXT);
            if (!xmlFile.exists()) {
                throw new Exception("XML file not found: " + xmlFile.getAbsolutePath());
            }

            File outputFile = new File(getOutputDirectory(), fileName + OUTPUT_EXT);
            logger.info("Compiling: " + xmlFile.getName() + " -> " + outputFile.getName());

            ipeWrapper.compile(xmlFile.getAbsolutePath(), outputFile.getAbsolutePath());
            logger.info("Successfully compiled: " + outputFile.getAbsolutePath());
        } else {
            // multi-file compilation
            File[] xmlFiles = getInputDirectory().listFiles((dir, name) -> name.toLowerCase().endsWith(INPUT_EXT));
            if (xmlFiles == null || xmlFiles.length == 0) {
                throw new Exception("No XML files found in input directory");
            }

            int successCount = 0;
            for (File xmlFile : xmlFiles) {
                String baseName = xmlFile.getName().replaceAll("\\.[^.]*$", "");
                File outputFile = new File(getOutputDirectory(), baseName + OUTPUT_EXT);

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
}
