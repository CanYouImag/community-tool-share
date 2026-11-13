package com.linzu.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.linzu.common.security.IdCardCipher;
import com.linzu.user.dto.UserProfileResponse;
import com.linzu.user.entity.User;
import com.linzu.user.entity.UserRealName;
import com.linzu.user.mapper.UserMapper;
import com.linzu.user.mapper.UserRealNameMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRealNameMapper userRealNameMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IdCardCipher idCardCipher;

    @InjectMocks
    private UserService userService;

    @Test
    void profileReturnsRejectReasonAndMaskedIdCard() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setRole("REGISTERED");
        user.setStatus("ACTIVE");
        user.setCreditScore(100);

        UserRealName realName = new UserRealName();
        realName.setUserId(7L);
        realName.setRealName("张三");
        realName.setIdCardNo("enc:v1:test");
        realName.setStatus("REJECTED");
        realName.setRejectReason("照片不清晰");

        when(userMapper.selectById(7L)).thenReturn(user);
        when(userRealNameMapper.selectOne(any())).thenReturn(realName);
        when(idCardCipher.decrypt("enc:v1:test")).thenReturn("220101199001011232");

        UserProfileResponse response = userService.getProfile(7L);

        assertEquals("REJECTED", response.realNameStatus());
        assertEquals("照片不清晰", response.realNameRejectReason());
        assertEquals("220101********1232", response.idCardMasked());
    }
}
