package com.linzu;

import com.linzu.common.security.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan({"com.linzu.user.mapper", "com.linzu.wallet.mapper"})
@EnableConfigurationProperties(JwtProperties.class)
public class LinzuApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinzuApplication.class, args);
    }
}
