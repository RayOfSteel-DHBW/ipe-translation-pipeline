package com.translation;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.translation.di.ApplicationModule;
import com.translation.pipeline.Pipeline;
import com.translation.pipeline.PipelineStepBase;
import com.translation.pipeline.steps.DecompileStep;
import com.translation.services.DownloadService;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Bootstrapper {
    private static final Logger logger = Logger.getLogger(Bootstrapper.class.getName());

    private Injector injector;
    private DownloadService downloadService;

    public void run(String[] args) {
        logger.info("Bootstrapper starting IPE Translation Pipeline");
        
        try {
            setupDependencyInjection();
            createDirectories();
            downloadIpeFiles();
            
            Pipeline pipeline = createPipeline();
            pipeline.execute();
            
            logger.info("Pipeline completed successfully!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Bootstrapper execution failed", e);
            System.exit(1);
        }
    }
    
    private void setupDependencyInjection() {
        logger.info("Setting up dependency injection...");
        
        this.injector = Guice.createInjector(new ApplicationModule());
        this.downloadService = injector.getInstance(DownloadService.class);
        
        logger.info("Dependency injection setup complete");
    }
    
    private void createDirectories() {
        logger.info("Creating working directories...");
        
        String[] directories = {
            Constants.INPUT_DIR, Constants.WORKING_DIR, Constants.OUTPUT_DIR,
            Constants.STEP_01_INPUT, Constants.STEP_01_OUTPUT
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
    
    private void downloadIpeFiles() throws Exception {
        logger.info("Step 0: Downloading IPE files...");
        
        downloadService.downloadToDirectory(Constants.STEP_01_INPUT);
        
        logger.info("Download completed. Files saved to: " + Constants.STEP_01_INPUT);
    }
    
    private Pipeline createPipeline() {
        logger.info("Creating pipeline with hardcoded step paths...");
        
        ArrayList<PipelineStepBase> steps = new ArrayList<>();
        
        DecompileStep decompileStep = new DecompileStep(
            1, 
            new File(Constants.STEP_01_INPUT), 
            new File(Constants.STEP_01_OUTPUT)
        );
        steps.add(decompileStep);
        
        logger.info("Pipeline created with " + steps.size() + " steps");
        return new Pipeline(steps);
    }
}
