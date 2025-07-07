package com.translation.di;

import com.google.inject.AbstractModule;
import com.translation.services.DownloadService;

public class ApplicationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DownloadService.class);
    }
}
