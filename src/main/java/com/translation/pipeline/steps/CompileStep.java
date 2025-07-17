package com.translation.pipeline.steps;

import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.ipe.CompilerException;
import com.translation.util.IpeWrapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class CompileStep extends PipelineStepBase {
    private static final int STEP_ORDER = 5;
    private final IpeWrapper ipeWrapper;

    // file-handling
    private static final String INPUT_EXT = ".xml";
    private static final String OUTPUT_EXT = ".pdf";
    
    // IPE log file handling
    private static final String IPE_LOG_PATH = "C:\\Users\\raine\\AppData\\Local\\ipe\\ipetemp.log";
    private static final String IPE_TEX_PATH = "C:\\Users\\raine\\AppData\\Local\\ipe\\ipetemp.tex";
    private static final String IPE_LOGS_DIR = "ipelogs";

    @Inject
    public CompileStep(IpeWrapper ipeWrapper, Configuration configuration) {
        super("IPE Compilation");
        this.ipeWrapper = ipeWrapper;
    }

    @Override
    protected int getStepOrder() {
        return STEP_ORDER;
    }

    @Override
    protected boolean performAction(String fileName) throws Exception {
        if (fileName != null && !fileName.trim().isEmpty()) {
            // single-file compilation
            File xmlFile = new File(getInputDirectory(), fileName + INPUT_EXT);
            if (!xmlFile.exists()) {
                logger.warning("XML file not found: " + xmlFile.getAbsolutePath() + " (likely previous step failed)");
                return false;
            }

            File outputFile = new File(getOutputDirectory(), fileName + OUTPUT_EXT);
            logger.info("Compiling: " + xmlFile.getName() + " -> " + outputFile.getName());

            try {
                ipeWrapper.compile(xmlFile.getAbsolutePath(), outputFile.getAbsolutePath());
                logger.fine("Successfully compiled: " + outputFile.getAbsolutePath());
                return true;
            } catch (Exception e) {
                logger.warning("Failed to compile: " + xmlFile.getName() + " - " + e.getMessage());
                saveIpeFiles(fileName);
                // Wrap in CompilerException for more specific error handling
                throw new CompilerException("IPE compilation failed for " + xmlFile.getName(), e);
            }
        } else {
            throw new Exception("No filename provided for compilation");
        }
    }

    /**
     * Copies the IPE log and tex files to our ipelogs directory when compilation fails
     */
    private void saveIpeFiles(String xmlFileName) {
        try {
            // Create ipelogs directory if it doesn't exist
            Path ipeLogsDir = Paths.get(IPE_LOGS_DIR);
            if (!Files.exists(ipeLogsDir)) {
                Files.createDirectories(ipeLogsDir);
            }

            // Save log file
            Path ipeLogPath = Paths.get(IPE_LOG_PATH);
            if (Files.exists(ipeLogPath)) {
                String logFileName = xmlFileName + ".log";
                Path destinationLogPath = ipeLogsDir.resolve(logFileName);
                Files.copy(ipeLogPath, destinationLogPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Saved IPE compilation log to: " + destinationLogPath.toAbsolutePath());
            } else {
                logger.warning("IPE log file not found at: " + IPE_LOG_PATH);
            }

            // Save tex file
            Path ipeTexPath = Paths.get(IPE_TEX_PATH);
            if (Files.exists(ipeTexPath)) {
                String texFileName = xmlFileName + ".tex";
                Path destinationTexPath = ipeLogsDir.resolve(texFileName);
                Files.copy(ipeTexPath, destinationTexPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Saved IPE compilation tex file to: " + destinationTexPath.toAbsolutePath());
            } else {
                logger.warning("IPE tex file not found at: " + IPE_TEX_PATH);
            }

        } catch (IOException e) {
            logger.warning("Failed to save IPE files: " + e.getMessage());
        }
    }
}
