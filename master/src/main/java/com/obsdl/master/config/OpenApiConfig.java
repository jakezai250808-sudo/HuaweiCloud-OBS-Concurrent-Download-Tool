package com.obsdl.master.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI masterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OBSDL Master API")
                        .version("v1")
                        .description("Master service for coordinating OBS concurrent downloads"));
    }
}
