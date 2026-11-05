package com.linzu.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI linzuOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("邻租 API")
                .version("v1")
                .description("社区闲置工具共享与租赁平台后端接口"));
    }
}
