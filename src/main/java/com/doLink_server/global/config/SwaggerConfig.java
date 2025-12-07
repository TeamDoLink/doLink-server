package com.doLink_server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // url: http://localhost:8080/swagger-ui/index.html
    @Bean
    public OpenAPI getOpenApi() {
        Server server = new Server().url("/");
        return new OpenAPI()
                .info(getSwaggerInfo())
                .components(authSetting())
                .addServersItem(server)
                .addSecurityItem(new SecurityRequirement().addList("access-token"));
    }

    private Info getSwaggerInfo() {
        License license = new License();
        license.name("Copyright © 2025-12th-Dolink");

        return new Info()
                .title("Dolink API Documentation")
                .description("This is DoLink! 저장하고, 묶고, 바로 꺼내는 링크 관리")
                .version("v1.0.0")
                .license(license);
    }

    private Components authSetting() {
        return new Components()
                .addSecuritySchemes(
                        "access-token",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
    }
}
