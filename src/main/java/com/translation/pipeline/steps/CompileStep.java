package com.translation.pipeline.steps;

import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.pipeline.PipelineStepBase;
import com.translation.util.IpeWrapper;

public class CompileStep extends PipelineStepBase {
    private final IpeWrapper ipeWrapper;
    private final String xmlFilePath;
    private final String outputFilePath;

    @Inject
    public CompileStep(IpeWrapper ipeWrapper, Configuration configuration) {
        super(4, "IPE Compilation"); // Default order and name
        this.ipeWrapper = ipeWrapper;
        this.xmlFilePath = null; // Will be set later
        this.outputFilePath = null; // Will be set later
    }

    public CompileStep(int order, String xmlFilePath, String outputFilePath, IpeWrapper ipeWrapper) {
        super(order, "IPE Compilation");
        this.xmlFilePath = xmlFilePath;
        this.outputFilePath = outputFilePath;
        this.ipeWrapper = ipeWrapper;
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Compiling XML to IPE: " + xmlFilePath);
        
        ipeWrapper.compile(xmlFilePath, outputFilePath);
        logger.info("Successfully compiled to: " + outputFilePath);
    }
}
