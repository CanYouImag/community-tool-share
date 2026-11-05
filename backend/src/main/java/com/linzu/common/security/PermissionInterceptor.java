package com.linzu.common.security;

import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String COMMUNITY_ADMIN = "COMMUNITY_ADMIN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission permission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            permission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (permission == null) {
            return true;
        }

        AuthUser user = AuthContext.require();
        if (PLATFORM_ADMIN.equals(user.role())) {
            return true;
        }

        for (String code : permission.value()) {
            if (code.startsWith("platform:") && !PLATFORM_ADMIN.equals(user.role())) {
                continue;
            }
            if (code.startsWith("community:")
                    && (COMMUNITY_ADMIN.equals(user.role()) || PLATFORM_ADMIN.equals(user.role()))) {
                return true;
            }
            if (!code.contains(":")) {
                return true;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
