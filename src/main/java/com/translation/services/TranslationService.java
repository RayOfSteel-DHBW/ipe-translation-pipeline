package com.translation.services;

public interface TranslationService {
    boolean translate(String inputFilePath, String outputFilePath) throws Exception;
}