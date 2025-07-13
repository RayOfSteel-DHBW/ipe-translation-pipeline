package com.translation;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.translation.config.Configuration;
import com.translation.config.ConfigurationModule;
import com.translation.di.ApplicationModule;
import com.translation.pipeline.Pipeline;
import com.translation.services.DownloadService;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Bootstrapper {
    private static final Logger logger = Logger.getLogger(Bootstrapper.class.getName());

    private Injector injector;
    private DownloadService downloadService;
    private Configuration configuration;
    private Pipeline pipeline;

    public void run(String[] args) {
        logger.info("Bootstrapper starting IPE Translation Pipeline");
        
        try {
            setupDependencyInjection(args);
            createDirectories();
            String[] fileNames = downloadIpeFiles();
            
            // Pipeline is now auto-created by DI with all steps in correct order!
            pipeline.execute(fileNames);
            
            logger.info("Pipeline completed successfully!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Bootstrapper execution failed", e);
            System.exit(1);
        }
    }
    
    private void setupDependencyInjection(String[] args) {
        logger.info("Setting up dependency injection...");
        
        this.injector = Guice.createInjector(new ApplicationModule(args), new ConfigurationModule());
        this.downloadService = injector.getInstance(DownloadService.class);
        this.configuration = injector.getInstance(Configuration.class);
        this.pipeline = injector.getInstance(Pipeline.class);
        
        logger.info("Dependency injection setup complete");
        logger.info("IPE extract path: " + configuration.getIpeExtractPath());
    }
    
    private void createDirectories() {
        logger.info("Creating working directory...");
        
        File directory = new File(Constants.WORK_DIR);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.info("Created directory: " + Constants.WORK_DIR);
            } else {
                logger.warning("Failed to create directory: " + Constants.WORK_DIR);
            }
        }
    }
    
    private String[] downloadIpeFiles() throws Exception {
        logger.info("Step 0: Downloading IPE files...");

        File step0Dir = new File(Constants.WORK_DIR + "/step-0");
        if (!step0Dir.exists()) {
            step0Dir.mkdirs();
        }

        List<String> names = downloadService.downloadToDirectory(step0Dir.getAbsolutePath());
        logger.info("Download completed. Files saved to: " + step0Dir.getAbsolutePath());
        return names.toArray(new String[0]);
    }
}
