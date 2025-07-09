package com.translation.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.io.IOException;

public class ConfigurationModule extends AbstractModule {
    
    @Provides
    @Singleton
    public Configuration provideConfiguration(ConfigurationLoader loader) {
        try {
            return loader.loadConfiguration();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
}
