package com.linzu.common.security;

import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Set<String> WHITE_LIST = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/captcha",
        "/api/v1/health"
    );

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || WHITE_LIST.contains(path) || isPublicBrowse(request, path)) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            AuthContext.set(new AuthUser(jwtUtil.getUserId(token), jwtUtil.getRole(token)));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 社区浏览（列表 / 主页 / 公告）对游客开放（UC-B1）。
     * 仅放行只读 GET；成员管理、加入申请审核等写操作仍走登录与权限拦截。
     */
    private boolean isPublicBrowse(HttpServletRequest request, String path) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        for (String base : new String[]{"/api/communities", "/api/v1/communities"}) {
            if (path.equals(base)) {
                return true;
            }
            if (path.startsWith(base + "/home/")) {
                return true;
            }
            if (path.matches(base + "/[^/]+/announcements")) {
                return true;
            }
        }
        // 物品广场、分类和物品详情允许游客只读浏览（D017）。
        for (String base : new String[]{"/api/items", "/api/v1/items"}) {
            if (path.equals(base) || path.equals(base + "/categories")) {
                return true;
            }
            if (path.matches(base + "/\\d+")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        AuthContext.clear();
    }
}
