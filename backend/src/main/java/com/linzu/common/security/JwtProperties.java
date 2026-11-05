package com.linzu.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HS256 密钥，至少 32 字节。
     */
    private String secret;

    /**
     * token 有效期，单位小时。
     */
    private int expireHours = 72;
}
