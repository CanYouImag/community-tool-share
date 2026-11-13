package com.linzu;

import com.linzu.common.security.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({
    "com.linzu.user.mapper",
    "com.linzu.wallet.mapper",
    "com.linzu.item.mapper",
    "com.linzu.order.mapper",
    "com.linzu.community.mapper",
    "com.linzu.dispute.mapper",
    "com.linzu.platform.mapper"
})
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class LinzuApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinzuApplication.class, args);
    }
}
