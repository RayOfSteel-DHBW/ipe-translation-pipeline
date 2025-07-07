package com.translation.pipeline.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.util.IpeWrapper;

import java.io.File;

public class DecompileStep extends PipelineStepBase {
    private final IpeWrapper ipeWrapper;
    private final File inputFile;
    private final File outputFile;

    public DecompileStep(int order, File inputFile, File outputFile) {
        super(order, "IPE Decompilation");
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.ipeWrapper = new IpeWrapper();
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Decompiling IPE file: " + inputFile.getAbsolutePath());
        
        if (!inputFile.exists()) {
            throw new Exception("Input file does not exist: " + inputFile.getAbsolutePath());
        }
        
        ipeWrapper.decompile(inputFile, outputFile);
        logger.info("Successfully decompiled to: " + outputFile.getAbsolutePath());
    }
}
