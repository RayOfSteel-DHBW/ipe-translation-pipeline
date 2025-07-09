package com.translation;

import com.translation.pipeline.Pipeline;
import com.translation.pipeline.PipelineStepBase;
import com.translation.pipeline.steps.DecompileStep;
import com.translation.services.DownloadService;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Application {
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    
    private static final String INPUT_DIR = "input";
    private static final String WORKING_DIR = "working";
    private static final String OUTPUT_DIR = "output";
    
    private static final String STEP_01_INPUT = INPUT_DIR + "/step-01";
    private static final String STEP_01_OUTPUT = WORKING_DIR + "/step-01";

    public static void main(String[] args) {
        logger.info("Starting IPE Translation Pipeline Application");
        
        try {
            createDirectories();
            downloadIpeFiles();
            
            Pipeline pipeline = createPipeline();
            pipeline.execute();
            
            logger.info("Pipeline completed successfully!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Application execution failed", e);
            System.exit(1);
        }
    }
    
    private static void createDirectories() {
        logger.info("Creating working directories...");
        
        String[] directories = {
            INPUT_DIR, WORKING_DIR, OUTPUT_DIR,
            STEP_01_INPUT, STEP_01_OUTPUT
        };
        
        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    logger.info("Created directory: " + dir);
                } else {
                    logger.warning("Failed to create directory: " + dir);
                }
            }
        }
    }
    
    private static void downloadIpeFiles() throws Exception {
        logger.info("Step 0: Downloading IPE files...");
        
        DownloadService downloadService = new DownloadService();
        downloadService.downloadToDirectory(STEP_01_INPUT);
        
        logger.info("Download completed. Files saved to: " + STEP_01_INPUT);
    }
    
    private static Pipeline createPipeline() {
        logger.info("Creating pipeline with hardcoded step paths...");
        
        ArrayList<PipelineStepBase> steps = new ArrayList<>();
        
        DecompileStep decompileStep = new DecompileStep(
            1, 
            new File(STEP_01_INPUT), 
            new File(STEP_01_OUTPUT)
        );
        steps.add(decompileStep);
        
        logger.info("Pipeline created with " + steps.size() + " steps");
        return new Pipeline(steps);
    }
}