package com.translation.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.translation.config.Configuration;
import com.translation.extraction.ExtractionResult;
import com.translation.extraction.SmartTextExtractor;
import com.translation.pipeline.PipelineStepBase;
import com.translation.util.FileManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextExtractionStep extends PipelineStepBase {
    private final SmartTextExtractor textExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Inject
    public TextExtractionStep(SmartTextExtractor textExtractor, Configuration configuration) {
        super(2, "Text Extraction");
        this.textExtractor = textExtractor;
    }

    public TextExtractionStep(int order, String stepName, SmartTextExtractor textExtractor, Configuration configuration) {
        super(order, stepName);
        this.textExtractor = textExtractor;
    }

    @Override
    protected void performAction() throws Exception {
        logger.info("Extracting text from XML files in: " + getInputDirectory().getAbsolutePath());
        File[] xmlFiles = getInputDirectory().listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));

        if (xmlFiles == null || xmlFiles.length == 0) {
            throw new Exception("No XML files found in input directory: " + getInputDirectory().getAbsolutePath());
        }

        File jsonDir = new File(getOutputDirectory(), "JSON");
        File simpleTextDir = new File(getOutputDirectory(), "SimpleText");
        File testVersionDir = new File(getOutputDirectory(), "TestVersions");
        File batchDir = new File(getOutputDirectory(), "Batches");
        jsonDir.mkdirs();
        simpleTextDir.mkdirs();
        testVersionDir.mkdirs();
        batchDir.mkdirs();

        List<Map<String, Object>> batchFiles = new ArrayList<>();
        List<Map<String, Object>> batchTexts = new ArrayList<>();
        int globalTextId = 0;
        int successCount = 0;

        for (File xmlFile : xmlFiles) {
            logger.info("Processing XML file: " + xmlFile.getName());
            try {
                String xmlContent = FileManager.readFile(xmlFile.getAbsolutePath());
                ExtractionResult result = textExtractor.extractText(xmlContent);
                String baseName = xmlFile.getName().replaceAll("\\.[^.]*$", "");
                File structureFile = new File(getOutputDirectory(), baseName + "_structure.xml");
                FileManager.writeFile(structureFile.getAbsolutePath(), result.getProcessedXml());
                File jsonFile = new File(jsonDir, baseName + ".json");
                String extractionJson = objectMapper.writeValueAsString(result);
                FileManager.writeFile(jsonFile.getAbsolutePath(), extractionJson);
                File simpleTextFile = new File(simpleTextDir, baseName + ".txt");
                StringBuilder simpleTextBuilder = new StringBuilder();
                result.getTextElements().forEach(element -> {
                    simpleTextBuilder.append(element.getId())
                            .append("\n")
                            .append(element.getOriginalText())
                            .append("\n");
                });
                FileManager.writeFile(simpleTextFile.getAbsolutePath(), simpleTextBuilder.toString());
                File testVersionFile = new File(testVersionDir, baseName + "_test_version.json");
                result.getTextElements().forEach(element -> {
                    String txt = element.getOriginalText().trim();
                    if (txt.startsWith("\\")) {
                        element.setOriginalText("\\textbf{" + element.getId() + "}");
                    } else {
                        element.setOriginalText(String.valueOf(element.getId()));
                    }
                });
                String testJson = objectMapper.writeValueAsString(result);
                FileManager.writeFile(testVersionFile.getAbsolutePath(), testJson);
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("source_file", xmlFile.getAbsolutePath());
                fileInfo.put("filename", xmlFile.getName());
                fileInfo.put("text_count", result.getTextElements().size());
                batchFiles.add(fileInfo);
                for (var element : result.getTextElements()) {
                    Map<String, Object> textItem = new HashMap<>();
                    textItem.put("global_id", globalTextId++);
                    textItem.put("source_file", xmlFile.getAbsolutePath());
                    textItem.put("local_id", element.getId());
                    textItem.put("original_text", element.getOriginalText());
                    textItem.put("xpath", element.getXpath());
                    textItem.put("text_type", element.getTextType());
                    textItem.put("context", element.getContext());
                    batchTexts.add(textItem);
                }
                logger.info("Processed file: " + xmlFile.getName());
                successCount++;
            } catch (Exception e) {
                logger.warning("Failed to process XML file " + xmlFile.getName() + ": " + e.getMessage());
            }
        }

        if (successCount == 0) {
            throw new Exception("No XML files could be processed for text extraction");
        }

        Map<String, Object> batchData = new HashMap<>();
        batchData.put("batch_id", "smart_ipe_translation_batch");
        batchData.put("source_language", "de");
        batchData.put("target_language", "en");
        batchData.put("files", batchFiles);
        batchData.put("texts_for_translation", batchTexts);
        File batchFile = new File(batchDir, "smart_translation_batch.json");
        String batchJson = objectMapper.writeValueAsString(batchData);
        FileManager.writeFile(batchFile.getAbsolutePath(), batchJson);
        logger.info("Successfully processed " + successCount + " of " + xmlFiles.length + " XML files");
        logger.info("Batch file created: " + batchFile.getAbsolutePath());
    }
}
