package com.translation.pipeline.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.util.IpeWrapper;

public class CompileStep extends PipelineStepBase {
    private final IpeWrapper ipeWrapper;
    private final String xmlFilePath;
    private final String outputFilePath;

    public CompileStep(int order, String xmlFilePath, String outputFilePath) {
        super(order, "IPE Compilation");
        this.xmlFilePath = xmlFilePath;
        this.outputFilePath = outputFilePath;
        this.ipeWrapper = new IpeWrapper();
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Compiling XML to IPE: " + xmlFilePath);
        
        ipeWrapper.compile(xmlFilePath, outputFilePath);
        logger.info("Successfully compiled to: " + outputFilePath);
    }
}
