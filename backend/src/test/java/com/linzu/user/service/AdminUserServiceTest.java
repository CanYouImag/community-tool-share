package com.linzu.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.common.security.IdCardCipher;
import com.linzu.user.dto.AdminRealNameResponse;
import com.linzu.user.dto.AdminUserResponse;
import com.linzu.user.entity.User;
import com.linzu.user.entity.UserRealName;
import com.linzu.user.mapper.UserMapper;
import com.linzu.user.mapper.UserRealNameMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final String ID_CARD_PLAIN = "220101199001011232";
    private static final String ID_CARD_CIPHER = "enc:v1:test";

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRealNameMapper userRealNameMapper;

    @Mock
    private IdCardCipher idCardCipher;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void listUsersReturnsMaskedRealNameInfo() {
        User user = user(7L, "alice", "REGISTERED", "ACTIVE");
        UserRealName realName = realName(9L, 7L, "PENDING");
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user));
        page.setTotal(1);

        when(userMapper.selectPage(any(), any())).thenReturn(page);
        when(userRealNameMapper.selectList(any())).thenReturn(List.of(realName));
        when(idCardCipher.decrypt(ID_CARD_CIPHER)).thenReturn(ID_CARD_PLAIN);

        PageResult<AdminUserResponse> result = adminUserService.listUsers(
            "alice",
            "registered",
            "active",
            1,
            10
        );

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        AdminUserResponse record = result.getRecords().get(0);
        assertEquals("alice", record.username());
        assertEquals("PENDING", record.realNameStatus());
        assertEquals("220101********1232", record.idCardMasked());
    }

    @Test
    void listRealNamesJoinsUserInfoAndMasksIdCard() {
        User user = user(7L, "alice", "REGISTERED", "ACTIVE");
        UserRealName realName = realName(9L, 7L, "PENDING");
        Page<UserRealName> page = new Page<>(1, 10);
        page.setRecords(List.of(realName));
        page.setTotal(1);

        when(userRealNameMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user));
        when(idCardCipher.decrypt(ID_CARD_CIPHER)).thenReturn(ID_CARD_PLAIN);

        PageResult<AdminRealNameResponse> result = adminUserService.listRealNames(
            "pending",
            null,
            1,
            10
        );

        assertEquals(1, result.getTotal());
        AdminRealNameResponse record = result.getRecords().get(0);
        assertEquals("alice", record.username());
        assertEquals("张三", record.realName());
        assertEquals("PENDING", record.status());
        assertEquals("220101********1232", record.idCardMasked());
    }

    @Test
    void banRejectsSelf() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> adminUserService.banUser(7L, 7L)
        );

        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void banRejectsPlatformAdmin() {
        User admin = user(8L, "admin", "PLATFORM_ADMIN", "ACTIVE");
        when(userMapper.selectById(8L)).thenReturn(admin);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> adminUserService.banUser(1L, 8L)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void banSetsUserStatusToBanned() {
        User user = user(8L, "bob", "REGISTERED", "ACTIVE");
        when(userMapper.selectById(8L)).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        when(userRealNameMapper.selectOne(any())).thenReturn(null);

        AdminUserResponse result = adminUserService.banUser(1L, 8L);

        assertEquals("BANNED", result.status());
        verify(userMapper).updateById(user);
    }

    @Test
    void unbanSetsUserStatusToActive() {
        User user = user(8L, "bob", "REGISTERED", "BANNED");
        when(userMapper.selectById(8L)).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        when(userRealNameMapper.selectOne(any())).thenReturn(null);

        AdminUserResponse result = adminUserService.unbanUser(8L);

        assertEquals("ACTIVE", result.status());
        verify(userMapper).updateById(user);
    }

    @Test
    void approvePendingRealNameUpgradesUserRole() {
        User user = user(7L, "alice", "REGISTERED", "ACTIVE");
        UserRealName realName = realName(9L, 7L, "PENDING");
        when(userRealNameMapper.selectById(9L)).thenReturn(realName);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userRealNameMapper.updateById(realName)).thenReturn(1);
        when(userMapper.updateById(user)).thenReturn(1);
        when(idCardCipher.decrypt(ID_CARD_CIPHER)).thenReturn(ID_CARD_PLAIN);

        AdminRealNameResponse result = adminUserService.approveRealName(9L);

        assertEquals("VERIFIED", result.status());
        assertNull(result.rejectReason());
        assertEquals("CERTIFIED", user.getRole());
        verify(userRealNameMapper).updateById(realName);
        verify(userMapper).updateById(user);
    }

    @Test
    void approveKeepsAdminRole() {
        User user = user(7L, "community-admin", "COMMUNITY_ADMIN", "ACTIVE");
        UserRealName realName = realName(9L, 7L, "PENDING");
        when(userRealNameMapper.selectById(9L)).thenReturn(realName);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userRealNameMapper.updateById(realName)).thenReturn(1);
        when(idCardCipher.decrypt(ID_CARD_CIPHER)).thenReturn(ID_CARD_PLAIN);

        AdminRealNameResponse result = adminUserService.approveRealName(9L);

        assertEquals("VERIFIED", result.status());
        assertEquals("COMMUNITY_ADMIN", user.getRole());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void rejectPendingRealNameStoresReason() {
        User user = user(7L, "alice", "REGISTERED", "ACTIVE");
        UserRealName realName = realName(9L, 7L, "PENDING");
        when(userRealNameMapper.selectById(9L)).thenReturn(realName);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userRealNameMapper.updateById(realName)).thenReturn(1);
        when(idCardCipher.decrypt(ID_CARD_CIPHER)).thenReturn(ID_CARD_PLAIN);

        AdminRealNameResponse result = adminUserService.rejectRealName(
            9L,
            "  照片不清晰  "
        );

        assertEquals("REJECTED", result.status());
        assertEquals("照片不清晰", result.rejectReason());
        verify(userRealNameMapper).updateById(realName);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void rejectRequiresReason() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> adminUserService.rejectRealName(9L, "   ")
        );

        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
        verify(userRealNameMapper, never()).updateById(any(UserRealName.class));
    }

    @Test
    void approveRejectsApplicationNotPending() {
        UserRealName realName = realName(9L, 7L, "VERIFIED");
        when(userRealNameMapper.selectById(9L)).thenReturn(realName);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> adminUserService.approveRealName(9L)
        );

        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
        verify(userRealNameMapper, never()).updateById(any(UserRealName.class));
    }

    @Test
    void approveRejectsMissingApplication() {
        when(userRealNameMapper.selectById(9L)).thenReturn(null);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> adminUserService.approveRealName(9L)
        );

        assertEquals(ErrorCode.REAL_NAME_NOT_FOUND, exception.getErrorCode());
    }

    private User user(Long id, String username, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPhone("1380000000" + id);
        user.setNickname(username);
        user.setRole(role);
        user.setStatus(status);
        user.setCreditScore(100);
        user.setCreatedAt(LocalDateTime.of(2026, 11, 13, 10, 0));
        return user;
    }

    private UserRealName realName(Long id, Long userId, String status) {
        UserRealName realName = new UserRealName();
        realName.setId(id);
        realName.setUserId(userId);
        realName.setRealName("张三");
        realName.setIdCardNo(ID_CARD_CIPHER);
        realName.setStatus(status);
        realName.setCreatedAt(LocalDateTime.of(2026, 11, 13, 10, 0));
        realName.setUpdatedAt(LocalDateTime.of(2026, 11, 13, 10, 0));
        return realName;
    }
}
