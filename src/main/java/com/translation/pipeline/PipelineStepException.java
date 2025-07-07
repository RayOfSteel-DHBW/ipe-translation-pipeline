package com.translation.pipeline;

public class PipelineStepException extends Exception {
    
    public PipelineStepException(String stepName, int stepOrder, String message) {
        super("[Step " + stepOrder + " - " + stepName + "] " + message);
    }

    public PipelineStepException(String stepName, int stepOrder, String message, Throwable cause) {
        super("[Step " + stepOrder + " - " + stepName + "] " + message, cause);
    }
}
