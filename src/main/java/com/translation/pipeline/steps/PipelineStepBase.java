package com.translation.pipeline.steps;

import com.translation.Constants;
import com.translation.pipeline.PipelineStepException;

import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class PipelineStepBase {
    protected final Logger logger;
    private final String stepName;
    private final int order;
    private final File inputDirectory;
    private final File outputDirectory;

    public PipelineStepBase(int order, String stepName) {
        this.order = order;
        this.stepName = stepName;
        this.logger = Logger.getLogger(this.getClass().getName());
        
        this.inputDirectory = new File(Constants.WORK_DIR + "/step-" + (order - 1));
        this.outputDirectory = new File(Constants.WORK_DIR + "/step-" + order);
        
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    public final boolean execute(String fileName) throws Exception {
        logger.info("Starting pipeline step: " + stepName);
        
        try {
            boolean success = performAction(fileName);
            if (success) {
                logger.info("Successfully completed pipeline step: " + stepName);
            } else {
                logger.info("Pipeline step skipped: " + stepName);
            }
            return success;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to execute pipeline step: " + stepName, e);
            throw new PipelineStepException(stepName, order, e.getMessage(), e);
        }
    }

    protected abstract boolean performAction(String fileName) throws Exception;
    
    protected File getInputDirectory() {
        return inputDirectory;
    }
    
    protected File getOutputDirectory() {
        return outputDirectory;
    }
    
    public int getOrder() {
        return order;
    }

    public String getStepName() {
        return stepName;
    }
}
