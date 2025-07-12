package com.translation.pipeline.steps;

import com.translation.config.Configuration;
import com.translation.config.ConfigurationLoader;
import com.translation.util.ProcessExecutor;
import java.io.File;

public class IpeCompilationStep extends PipelineStepBase {

    private static final String INPUT_EXT = ".xml";
    private static final String OUTPUT_EXT = ".pdf";
    private final Configuration configuration;

    public IpeCompilationStep(int order) throws Exception {
        super(order, "IPE Compilation");
        this.configuration = ConfigurationLoader.loadConfiguration();
    }

    @Override
    protected boolean performAction(String fileName) throws Exception {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new Exception("Single-file mode: filename (without extension) must be provided");
        }

        File inputFile = new File(getInputDirectory(), fileName + INPUT_EXT);
        if (!inputFile.exists()) {
            logger.warning("Input XML file not found: " + inputFile.getAbsolutePath() + " (likely previous step failed)");
            return false;
        }

        File outputFile = new File(getOutputDirectory(), fileName + OUTPUT_EXT);
        logger.info("Compiling: " + inputFile.getName() + " -> " + outputFile.getName());

        String ipe2ipePath = configuration.getIpe2ipePath();
        String command = "\"" + ipe2ipePath + "\" -pdf \"" + inputFile.getAbsolutePath() + "\" \"" + outputFile.getAbsolutePath() + "\"";

        ProcessExecutor processExecutor = new ProcessExecutor();
        try {
            String output = processExecutor.executeCommand(command);
            logger.info("IPE compilation output: " + output);
        } catch (Exception e) {
            logger.severe("IPE compilation failed: " + e.getMessage());
            throw new Exception("IPE compilation failed: " + e.getMessage(), e);
        }

        if (!outputFile.exists()) {
            throw new Exception("IPE compilation completed but output file was not created: " + outputFile.getAbsolutePath());
        }

        logger.info("IPE compilation completed successfully");
        return true;
    }
}
