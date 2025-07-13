package com.translation.pipeline;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.translation.ipe.CompilerException;
import com.translation.pipeline.steps.PipelineStepBase;

import java.util.logging.Level;
import java.util.Arrays;
import java.util.Set;
import java.util.Comparator;

public class Pipeline {
    private ArrayList<PipelineStepBase> _steps;
    private static final Logger logger = Logger.getLogger(Pipeline.class.getName());

    @Inject
    public Pipeline(Set<PipelineStepBase> steps) {
        // Sort steps by their order automatically
        _steps = new ArrayList<>(steps);
        _steps.sort(Comparator.comparingInt(PipelineStepBase::getOrder));
        
        logger.info("Pipeline created with " + _steps.size() + " steps, sorted by order");
    }

    public void execute(String[] fileNames) throws PipelineStepException {
        logger.info("Starting pipeline execution with " + _steps.size() + " steps");

        if (fileNames == null || fileNames.length == 0) {
            logger.warning("No file names supplied – nothing to execute");
            return;
        }

        logger.info("Files to process: " + Arrays.toString(fileNames));

        for (String fileName : fileNames) {
            logger.fine("Processing file: " + fileName);
            boolean fileProcessingFailed = false;
            
            for (int i = 0; i < _steps.size(); i++) {
                if (fileProcessingFailed) {
                    logger.fine("Skipping remaining steps for " + fileName + " due to previous step failure");
                    break;
                }
                
                PipelineStepBase step = _steps.get(i);
                logger.fine("Executing step " + (i + 1) + "/" + _steps.size() + ": " + step.getStepName());

                try {
                    boolean success = step.execute(fileName); // pass current file to step
                    if (!success) {
                        logger.warning("Step " + step.getStepName() + " failed for file " + fileName + ", skipping remaining steps");
                        fileProcessingFailed = true;
                    }
                } catch (CompilerException e) {
                    // Handle compilation failures gracefully - log and continue with next file
                    logger.warning("Compilation failed for file " + fileName + ": " + e.getMessage());
                    fileProcessingFailed = true;
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                               "Step " + step.getStepName() + " failed for file " + fileName + " with exception: " + e.getMessage(),
                               e);
                    fileProcessingFailed = true; // Mark this file as failed and continue with next file
                }
            }
        }

        logger.info("Pipeline execution completed successfully");
    }
}