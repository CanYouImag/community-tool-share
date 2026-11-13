package com.linzu.common.security;

import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;

public final class AuthContext {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static AuthUser require() {
        AuthUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireUserId() {
        return require().id();
    }

    public static Long userIdOrNull() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.id();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
