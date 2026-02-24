package com.obsdl.master.config;

import com.obsdl.master.service.ObsRealDataProvider;
import com.obsdl.master.service.PlaceholderObsRealDataProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObsBrowserConfig {

    @Bean
    @ConditionalOnProperty(prefix = "obs.browser", name = "mode", havingValue = "real")
    @ConditionalOnMissingBean(ObsRealDataProvider.class)
    public ObsRealDataProvider placeholderObsRealDataProvider() {
        return new PlaceholderObsRealDataProvider();
    }
}
