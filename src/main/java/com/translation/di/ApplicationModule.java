package com.translation.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.translation.config.Configuration;
import com.translation.services.AutomatedTranslationService;
import com.translation.services.DownloadService;
import com.translation.services.ManualTranslationService;
import com.translation.services.TranslationService;
import com.translation.util.IpeWrapper;

public class ApplicationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DownloadService.class);
    }
    
    @Provides
    @Singleton
    public TranslationService provideTranslationService(Configuration configuration) {
        Configuration.TranslationConfig translationConfig = configuration.getTranslation();
        
        if ("manual".equalsIgnoreCase(translationConfig.getService())) {
            return new ManualTranslationService();
        } else {
            try {
                return new AutomatedTranslationService(
                    translationConfig.getSourceLanguage(),
                    translationConfig.getTargetLanguage()
                );
            } catch (Exception e) {
                return new ManualTranslationService();
            }
        }
    }
    
    @Provides
    @Singleton
    public IpeWrapper provideIpeWrapper(Configuration configuration) {
        return new IpeWrapper(configuration);
    }
    
    @Provides
    @Singleton
    public ManualTranslationService provideManualTranslationService() {
        return new ManualTranslationService();
    }
}
