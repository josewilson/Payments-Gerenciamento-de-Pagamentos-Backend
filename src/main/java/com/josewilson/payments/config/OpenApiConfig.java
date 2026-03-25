package com.josewilson.payments.config;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payments API")
                        .description("API para gerenciamento de pagamentos com auditoria e cotação")
                        .version("1.0.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentação do projeto")
                        .url("https://example.com/docs"));
    }
}
