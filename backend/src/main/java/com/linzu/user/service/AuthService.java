package com.linzu.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.common.security.JwtUtil;
import com.linzu.user.dto.LoginRequest;
import com.linzu.user.dto.LoginResponse;
import com.linzu.user.dto.RegisterRequest;
import com.linzu.user.dto.UserProfileResponse;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import com.linzu.wallet.entity.Wallet;
import com.linzu.wallet.mapper.WalletMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        String username = request.username().trim();
        Long exists = userMapper.selectCount(
            Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(
            StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username
        );
        user.setRole("REGISTERED");
        user.setStatus("ACTIVE");
        user.setCreditScore(100);
        userMapper.insert(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        wallet.setVersion(0);
        walletMapper.insert(wallet);

        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        if (
            !captchaService.validate(request.captchaId(), request.captchaCode())
        ) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }

        String account = request.account().trim();
        User user = userMapper.selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getPhone, account)
                .last("limit 1")
        );
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }
        if (userService.isBanned(user)) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        return buildLoginResponse(user);
    }

    public UserProfileResponse me(Long userId) {
        return userService.getProfile(userId);
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return new LoginResponse(
            token,
            jwtUtil.getExpireSeconds(),
            userService.getProfile(user.getId())
        );
    }
}
