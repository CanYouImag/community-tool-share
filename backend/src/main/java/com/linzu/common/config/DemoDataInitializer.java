package com.linzu.common.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linzu.common.security.IdCardCipher;
import com.linzu.user.entity.User;
import com.linzu.user.entity.UserRealName;
import com.linzu.user.mapper.UserMapper;
import com.linzu.user.mapper.UserRealNameMapper;
import com.linzu.wallet.entity.Wallet;
import com.linzu.wallet.mapper.WalletMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final UserRealNameMapper userRealNameMapper;
    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdCardCipher idCardCipher;

    @Value("${app.demo.username:Starling}")
    private String username;

    @Value("${app.demo.password:123456}")
    private String password;

    @Value("${app.demo.nickname:Starling}")
    private String nickname;

    @Value("${app.demo.admin-username:admin}")
    private String adminUsername;

    @Value("${app.demo.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.demo.admin-nickname:平台管理员}")
    private String adminNickname;

    @Override
    @Transactional
    public void run(String... args) {
        seedUser(
            username,
            password,
            nickname,
            "CERTIFIED",
            "13800000000",
            "演示用户",
            "220101199001011232",
            new BigDecimal("500.00"),
            "VERIFIED"
        );
        seedUser(
            adminUsername,
            adminPassword,
            adminNickname,
            "PLATFORM_ADMIN",
            "13900000000",
            "平台管理员",
            "220101199001011240",
            BigDecimal.ZERO,
            "VERIFIED"
        );
        // 给平台管理端留一条待审核实名认证，方便答辩演示审核通过/驳回。
        seedUser(
            "pending",
            "123456",
            "待审核用户",
            "REGISTERED",
            "13700000000",
            "待审核用户",
            "11010519491231002X",
            new BigDecimal("100.00"),
            "PENDING"
        );
    }

    private void seedUser(
        String loginName,
        String rawPassword,
        String displayName,
        String role,
        String phone,
        String realName,
        String idCardNo,
        BigDecimal balance,
        String realNameStatus
    ) {
        Long exists = userMapper.selectCount(
            Wrappers
                .<User>lambdaQuery()
                .eq(User::getUsername, loginName)
                .or()
                .eq(User::getPhone, phone)
        );
        if (exists != null && exists > 0) {
            return;
        }

        User user = new User();
        user.setUsername(loginName);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setNickname(displayName);
        user.setBio("把闲置交给需要它的邻居。");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreditScore(100);
        userMapper.insert(user);

        UserRealName userRealName = new UserRealName();
        userRealName.setUserId(user.getId());
        userRealName.setRealName(realName);
        userRealName.setIdCardNo(idCardCipher.encrypt(idCardNo));
        userRealName.setStatus(realNameStatus);
        userRealNameMapper.insert(userRealName);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(balance);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        wallet.setVersion(0);
        walletMapper.insert(wallet);

        log.info(
            "已创建演示账号：{} / {}（角色 {}，实名状态 {}，钱包余额 {}）",
            loginName,
            rawPassword,
            role,
            realNameStatus,
            balance
        );
    }
}
