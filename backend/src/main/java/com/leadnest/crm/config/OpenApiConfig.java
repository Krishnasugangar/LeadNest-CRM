package com.leadnest.crm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI leadNestOpenApi() {
        return new OpenAPI().info(new Info()
                .title("LeadNest CRM API")
                .version("1.0.0")
                .description("Session-authenticated sales CRM. Sign in with POST /api/auth/login, then send the CSRF cookie value in the X-XSRF-TOKEN header."));
    }
}
