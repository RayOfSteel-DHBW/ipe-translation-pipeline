package com.translation.pipeline;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.translation.pipeline.steps.PipelineStepBase;

import java.util.logging.Level;
import java.util.Arrays;

public class Pipeline {
    private ArrayList<PipelineStepBase> _steps;
    private static final Logger logger = Logger.getLogger(Pipeline.class.getName());

    public Pipeline(ArrayList<PipelineStepBase> steps) 
    {
        _steps = steps;
    }

    public void execute(String[] fileNames) throws PipelineStepException {
        logger.info("Starting pipeline execution with " + _steps.size() + " steps");

        if (fileNames == null || fileNames.length == 0) {
            logger.warning("No file names supplied – nothing to execute");
            return;
        }

        logger.info("Files to process: " + Arrays.toString(fileNames));

        for (String fileName : fileNames) {
            logger.info("Processing file: " + fileName);
            for (int i = 0; i < _steps.size(); i++) {
                PipelineStepBase step = _steps.get(i);
                logger.info("Executing step " + (i + 1) + "/" + _steps.size() + ": " + step.getStepName());

                try {
                    step.execute(fileName); // pass current file to step
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                               "Pipeline execution failed at step " + (i + 1) + ": " + step.getStepName(),
                               e);
                    throw new PipelineStepException(step.getStepName(), step.getOrder(),
                                                    "Pipeline execution failed", e);
                }
            }
        }

        logger.info("Pipeline execution completed successfully");
    }
}