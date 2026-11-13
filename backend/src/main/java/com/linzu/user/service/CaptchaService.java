package com.linzu.user.service;

import com.linzu.user.dto.CaptchaResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 简单图形验证码：进程内存储、一次性使用、默认 2 分钟过期。
 * 演示环境足够；多实例部署时应替换为 Redis 实现。
 */
@Service
public class CaptchaService {

    private static final char[] CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 4;
    private static final int MAX_ENTRIES = 10000;
    private static final String[] COLORS = {
        "#1f2937",
        "#334155",
        "#7c2d12",
        "#14532d",
        "#4c1d95",
    };

    private final boolean enabled;
    private final long expireSeconds;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public CaptchaService(
        @Value("${app.captcha.enabled:true}") boolean enabled,
        @Value("${app.captcha.expire-seconds:120}") long expireSeconds
    ) {
        this.enabled = enabled;
        this.expireSeconds = Math.max(1, expireSeconds);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CaptchaResponse create() {
        cleanup();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i += 1) {
            code.append(CHARS[random.nextInt(CHARS.length)]);
        }
        long expiresAt = System.currentTimeMillis() + expireSeconds * 1000;
        store.put(captchaId, new Entry(code.toString(), expiresAt));
        String image = toDataUri(renderSvg(code.toString()));
        return new CaptchaResponse(captchaId, image, expireSeconds);
    }

    /**
     * 校验并立即销毁验证码；即使输入错误也需要刷新验证码。
     */
    public boolean validate(String captchaId, String code) {
        if (!enabled) {
            return true;
        }
        if (captchaId == null || captchaId.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        Entry entry = store.remove(captchaId);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            return false;
        }
        return entry.code().equalsIgnoreCase(code.trim());
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        if (store.size() >= MAX_ENTRIES) {
            store.clear();
        }
    }

    private String renderSvg(String code) {
        StringBuilder svg = new StringBuilder(1024);
        svg
            .append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" height=\"40\" ")
            .append("viewBox=\"0 0 120 40\" role=\"img\" aria-label=\"图形验证码\">")
            .append("<rect width=\"120\" height=\"40\" fill=\"#f1f5f9\"/>");

        for (int i = 0; i < 5; i += 1) {
            svg
                .append("<line x1=\"")
                .append(random.nextInt(120))
                .append("\" y1=\"")
                .append(random.nextInt(40))
                .append("\" x2=\"")
                .append(random.nextInt(120))
                .append("\" y2=\"")
                .append(random.nextInt(40))
                .append("\" stroke=\"")
                .append(COLORS[random.nextInt(COLORS.length)])
                .append("\" stroke-width=\"1\" opacity=\"0.35\"/>");
        }

        for (int i = 0; i < code.length(); i += 1) {
            int x = 12 + i * 27 + random.nextInt(5);
            int y = 28 + random.nextInt(5) - 2;
            int rotate = random.nextInt(25) - 12;
            svg
                .append("<text x=\"")
                .append(x)
                .append("\" y=\"")
                .append(y)
                .append("\" transform=\"rotate(")
                .append(rotate)
                .append(' ')
                .append(x)
                .append(' ')
                .append(y)
                .append(")\" font-family=\"Arial, sans-serif\" font-size=\"24\" ")
                .append("font-weight=\"700\" fill=\"")
                .append(COLORS[random.nextInt(COLORS.length)])
                .append("\">")
                .append(code.charAt(i))
                .append("</text>");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private String toDataUri(String svg) {
        String base64 = Base64
            .getEncoder()
            .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    private record Entry(String code, long expiresAt) {}
}
