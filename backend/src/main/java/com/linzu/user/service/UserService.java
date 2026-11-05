package com.linzu.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.user.dto.ChangePasswordRequest;
import com.linzu.user.dto.RealNameRequest;
import com.linzu.user.dto.UpdateProfileRequest;
import com.linzu.user.dto.UserProfileResponse;
import com.linzu.user.entity.User;
import com.linzu.user.entity.UserRealName;
import com.linzu.user.mapper.UserMapper;
import com.linzu.user.mapper.UserRealNameMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String UNVERIFIED = "UNVERIFIED";
    private static final String PENDING = "PENDING";
    private static final String VERIFIED = "VERIFIED";
    private static final String REJECTED = "REJECTED";
    private static final String ACTIVE = "ACTIVE";
    private static final String BANNED = "BANNED";

    private final UserMapper userMapper;
    private final UserRealNameMapper userRealNameMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.auto-verify-real-name:true}")
    private boolean autoVerifyRealName;

    public User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = requireUser(userId);
        UserRealName realName = findRealName(userId);
        return toProfile(user, realName);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        if (request.nickname() != null && StringUtils.hasText(request.nickname())) {
            user.setNickname(request.nickname().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().trim());
        }
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        return getProfile(userId);
    }

    @Transactional
    public UserProfileResponse updateAvatar(Long userId, String avatarUrl) {
        User user = requireUser(userId);
        user.setAvatarUrl(avatarUrl.trim());
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        return getProfile(userId);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
    }

    @Transactional
    public UserProfileResponse submitRealName(Long userId, RealNameRequest request) {
        User user = requireUser(userId);
        UserRealName realName = findRealName(userId);
        if (realName != null && (VERIFIED.equals(realName.getStatus()) || PENDING.equals(realName.getStatus()))) {
            throw new BusinessException(ErrorCode.REAL_NAME_EXISTS);
        }

        String idCardNo = request.idCardNo().trim().toUpperCase();
        String status = autoVerifyRealName ? VERIFIED : PENDING;
        if (realName == null) {
            realName = new UserRealName();
            realName.setUserId(userId);
            realName.setRealName(request.realName().trim());
            realName.setIdCardNo(idCardNo);
            realName.setStatus(status);
            realName.setRejectReason(null);
            userRealNameMapper.insert(realName);
        } else {
            realName.setRealName(request.realName().trim());
            realName.setIdCardNo(idCardNo);
            realName.setStatus(status);
            realName.setRejectReason(null);
            userRealNameMapper.updateById(realName);
        }

        if (VERIFIED.equals(status) && !isAdmin(user.getRole())) {
            user.setRole("CERTIFIED");
            userMapper.updateById(user);
        }
        return getProfile(userId);
    }

    public boolean isBanned(User user) {
        return BANNED.equals(user.getStatus());
    }

    public boolean isActive(User user) {
        return ACTIVE.equals(user.getStatus());
    }

    private UserRealName findRealName(Long userId) {
        return userRealNameMapper.selectOne(
                Wrappers.<UserRealName>lambdaQuery().eq(UserRealName::getUserId, userId)
        );
    }

    private UserProfileResponse toProfile(User user, UserRealName realName) {
        String realNameStatus = realName == null ? UNVERIFIED : realName.getStatus();
        String realNameValue = realName == null ? null : realName.getRealName();
        String idCardMasked = realName == null ? null : maskIdCard(realName.getIdCardNo());
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getRole(),
                user.getStatus(),
                user.getCreditScore(),
                realNameStatus,
                realNameValue,
                idCardMasked,
                user.getCreatedAt()
        );
    }

    private String maskIdCard(String idCardNo) {
        if (!StringUtils.hasText(idCardNo) || idCardNo.length() < 10) {
            return idCardNo;
        }
        return idCardNo.substring(0, 6) + "********" + idCardNo.substring(idCardNo.length() - 4);
    }

    private boolean isAdmin(String role) {
        return "COMMUNITY_ADMIN".equals(role) || "PLATFORM_ADMIN".equals(role);
    }
}
