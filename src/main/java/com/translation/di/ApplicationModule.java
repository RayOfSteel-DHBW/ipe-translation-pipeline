package com.translation.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.translation.config.Configuration;
import com.translation.pipeline.Pipeline;
import com.translation.pipeline.steps.*;
import com.translation.services.AutomatedTranslationService;
import com.translation.services.DownloadService;
import com.translation.services.NullTranslationService;
import com.translation.services.TranslationService;
import com.translation.util.IpeWrapper;

public class ApplicationModule extends AbstractModule {
    private final String[] args;
    
    public ApplicationModule(String[] args) {
        this.args = args;
    }
    
    @Override
    protected void configure() {
        bind(DownloadService.class);
        
        // Auto-register all pipeline steps
        Multibinder<PipelineStepBase> stepBinder = Multibinder.newSetBinder(binder(), PipelineStepBase.class);
        stepBinder.addBinding().to(DecompileStep.class);
        stepBinder.addBinding().to(TextExtractionStep.class);
        stepBinder.addBinding().to(TranslationStep.class);
        stepBinder.addBinding().to(TextRestorationStep.class);
        stepBinder.addBinding().to(CompileStep.class);
        
        // Pipeline will be auto-created with all steps
        bind(Pipeline.class);
    }
    
    @Provides
    @Singleton
    public TranslationService provideTranslationService(Configuration configuration) {
        // Check for --no-translate or -nt flag
        for (String arg : args) {
            if ("--no-translate".equals(arg) || "-nt".equals(arg)) {
                return new NullTranslationService();
            }
        }
        
        // Default to automated translation
        try {
            return new AutomatedTranslationService();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TranslationService", e);
        }
    }
    
    @Provides
    @Singleton
    public IpeWrapper provideIpeWrapper(Configuration configuration) {
        return new IpeWrapper(configuration);
    }

}
