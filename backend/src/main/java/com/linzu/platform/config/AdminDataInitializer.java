package com.linzu.platform.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预置系统管理员账号（需求：系统管理员由数据库预置）。
 * 社区管理员在运行时通过 UC-G2（任命社区管理员）产生。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
public class AdminDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.admin-username:admin}")
    private String adminUsername;

    @Value("${app.demo.admin-password:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, adminUsername)
        );
        if (exists != null && exists > 0) {
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPhone("13900000000");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setNickname("系统管理员");
        admin.setBio("平台运营方");
        admin.setRole("PLATFORM_ADMIN");
        admin.setStatus("ACTIVE");
        admin.setCreditScore(100);
        userMapper.insert(admin);

        log.info("已创建系统管理员账号：{} / {}", adminUsername, adminPassword);
    }
}
