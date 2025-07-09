package com.translation;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.translation.config.Configuration;
import com.translation.config.ConfigurationModule;
import com.translation.di.ApplicationModule;
import com.translation.pipeline.Pipeline;
import com.translation.pipeline.PipelineStepBase;
import com.translation.steps.DecompileStep;
import com.translation.services.DownloadService;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Bootstrapper {
    private static final Logger logger = Logger.getLogger(Bootstrapper.class.getName());

    private Injector injector;
    private DownloadService downloadService;
    private Configuration configuration;

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
        
        this.injector = Guice.createInjector(new ApplicationModule(), new ConfigurationModule());
        this.downloadService = injector.getInstance(DownloadService.class);
        this.configuration = injector.getInstance(Configuration.class);
        
        logger.info("Dependency injection setup complete");
        logger.info("IPE executable path: " + configuration.getIpeExecutablePath());
    }
    
    private void createDirectories() {
        logger.info("Creating working directories...");
        
        String[] directories = {
            Constants.INPUT_DIR, Constants.WORK_DIR, Constants.OUTPUT_DIR
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
        
        File step0Dir = new File(Constants.WORK_DIR + "/step-0");
        if (!step0Dir.exists()) {
            step0Dir.mkdirs();
        }
        
        downloadService.downloadToDirectory(step0Dir.getAbsolutePath());
        
        logger.info("Download completed. Files saved to: " + step0Dir.getAbsolutePath());
    }
    
    private Pipeline createPipeline() {
        logger.info("Creating pipeline with hardcoded step paths...");
        
        ArrayList<PipelineStepBase> steps = new ArrayList<>();
        
        DecompileStep decompileStep = injector.getInstance(DecompileStep.class);
        steps.add(decompileStep);
        
        logger.info("Pipeline created with " + steps.size() + " steps");
        return new Pipeline(steps);
    }
}
