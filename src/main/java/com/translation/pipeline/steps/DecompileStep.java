package com.translation.pipeline.steps;

import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.util.IpeWrapper;

import java.io.File;

public class DecompileStep extends PipelineStepBase {
    private static final int STEP_ORDER = 1;
    private final IpeWrapper ipeWrapper;
    private final Configuration configuration;

    // file-handling
    private static final String INPUT_EXT = ".pdf";
    private static final String OUTPUT_EXT = ".xml";

    @Inject
    public DecompileStep(IpeWrapper ipeWrapper, Configuration configuration) {
        super("decompile");
        this.ipeWrapper = ipeWrapper;
        this.configuration = configuration;
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

        File pdfFile = new File(getInputDirectory(), fileName + INPUT_EXT);
        if (!pdfFile.exists()) {
            throw new Exception("PDF file not found: " + pdfFile.getAbsolutePath());
        }

        File outputFile = new File(getOutputDirectory(), fileName + OUTPUT_EXT);
        logger.info("Extracting IPE XML from: " + pdfFile.getName() + " -> " + outputFile.getName());

        if (!ipeWrapper.extractXml(pdfFile, outputFile)) {
            logger.warning("Failed to extract IPE XML from: " + pdfFile.getName() + " (likely not an IPE file)");
            return false;
        }

        logger.info("Successfully extracted IPE XML to: " + outputFile.getAbsolutePath());
        return true;
    }
}