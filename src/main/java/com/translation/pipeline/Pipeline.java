package com.translation.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Pipeline {
    private ArrayList<PipelineStepBase> _steps;
    private static final Logger logger = Logger.getLogger(Pipeline.class.getName());

    public Pipeline(ArrayList<PipelineStepBase> steps) 
    {
        _steps = steps;
    }

    public void execute() throws PipelineStepException {
        logger.info("Starting pipeline execution with " + _steps.size() + " steps");
        
        for (int i = 0; i < _steps.size(); i++) {
            PipelineStepBase step = _steps.get(i);
            logger.info("Executing step " + (i + 1) + "/" + _steps.size() + ": " + step.getStepName());
            
            try {
                step.execute();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Pipeline execution failed at step " + (i + 1) + ": " + step.getStepName(), e);
                throw new PipelineStepException(step.getStepName(), step.getOrder(), "Pipeline execution failed", e);
            }
        }
        
        logger.info("Pipeline execution completed successfully");
    }

    public List<PipelineStepBase> getSteps() {
        return new ArrayList<>(_steps);
    }
}