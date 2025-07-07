package com.translation.translator;

public interface TranslationService {
    void translate(String inputFilePath, String outputFilePath) throws Exception;
}