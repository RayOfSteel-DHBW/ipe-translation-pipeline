package com.translation.services;

public interface TranslationService {
    void translate(String inputFilePath, String outputFilePath) throws Exception;
}