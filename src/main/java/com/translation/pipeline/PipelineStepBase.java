package com.translation.pipeline;

import com.translation.Constants;
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

    public final void execute() throws Exception {
        logger.info("Starting pipeline step: " + stepName);
        
        try {
            performAction();
            logger.info("Successfully completed pipeline step: " + stepName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to execute pipeline step: " + stepName, e);
            throw new PipelineStepException(stepName, order, e.getMessage(), e);
        }
    }

    protected abstract void performAction() throws Exception;
    
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
