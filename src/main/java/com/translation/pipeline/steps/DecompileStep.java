package com.translation.pipeline.steps;

import com.translation.pipeline.PipelineStepBase;
import com.translation.util.IpeWrapper;

import java.io.File;

public class DecompileStep extends PipelineStepBase {
    private final IpeWrapper ipeWrapper;
    private final File inputDirectory;
    private final File outputDirectory;

    public DecompileStep(int order, File inputDirectory, File outputDirectory) {
        super(order, "IPE Decompilation");
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.ipeWrapper = new IpeWrapper();
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Decompiling IPE files from: " + inputDirectory.getAbsolutePath());
        
        if (!inputDirectory.exists() || !inputDirectory.isDirectory()) {
            throw new Exception("Input directory does not exist or is not a directory: " + inputDirectory.getAbsolutePath());
        }
        
        // Ensure output directory exists
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new Exception("Failed to create output directory: " + outputDirectory.getAbsolutePath());
            }
        }
        
        // Find all IPE files in input directory
        File[] ipeFiles = inputDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".ipe"));
        
        if (ipeFiles == null || ipeFiles.length == 0) {
            throw new Exception("No IPE files found in input directory: " + inputDirectory.getAbsolutePath());
        }
        
        logger.info("Found " + ipeFiles.length + " IPE files to decompile");
        
        // Process each IPE file
        for (File ipeFile : ipeFiles) {
            String baseName = ipeFile.getName().replaceAll("\\.ipe$", "");
            File outputFile = new File(outputDirectory, baseName + ".xml");
            
            logger.info("Decompiling: " + ipeFile.getName() + " -> " + outputFile.getName());
            ipeWrapper.decompile(ipeFile, outputFile);
        }
        
        logger.info("Successfully decompiled " + ipeFiles.length + " files to: " + outputDirectory.getAbsolutePath());
    }
}
