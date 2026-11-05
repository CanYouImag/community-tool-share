package com.linzu.common.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linzu.user.entity.User;
import com.linzu.user.entity.UserRealName;
import com.linzu.user.mapper.UserMapper;
import com.linzu.user.mapper.UserRealNameMapper;
import com.linzu.wallet.entity.Wallet;
import com.linzu.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final UserRealNameMapper userRealNameMapper;
    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.username:Starling}")
    private String username;

    @Value("${app.demo.password:123456}")
    private String password;

    @Value("${app.demo.nickname:Starling}")
    private String nickname;

    @Override
    @Transactional
    public void run(String... args) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
        if (exists != null && exists > 0) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPhone("13800000000");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setBio("把闲置交给需要它的邻居。");
        user.setRole("CERTIFIED");
        user.setStatus("ACTIVE");
        user.setCreditScore(100);
        userMapper.insert(user);

        UserRealName realName = new UserRealName();
        realName.setUserId(user.getId());
        realName.setRealName("演示用户");
        realName.setIdCardNo("220101199001011234");
        realName.setStatus("VERIFIED");
        userRealNameMapper.insert(realName);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(new BigDecimal("500.00"));
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        wallet.setVersion(0);
        walletMapper.insert(wallet);

        log.info("已创建演示账号：{} / {}（钱包余额 500.00）", username, password);
    }
}
