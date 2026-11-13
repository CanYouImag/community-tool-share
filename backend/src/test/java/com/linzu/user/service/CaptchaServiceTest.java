package com.linzu.user.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.linzu.user.dto.CaptchaResponse;
import org.junit.jupiter.api.Test;

class CaptchaServiceTest {

    @Test
    void createsImageAndRejectsWrongCode() {
        CaptchaService service = new CaptchaService(true, 120);

        CaptchaResponse captcha = service.create();

        assertNotNull(captcha.captchaId());
        assertTrue(captcha.image().startsWith("data:image/svg+xml;base64,"));
        assertFalse(service.validate(captcha.captchaId(), "0000"));
    }

    @Test
    void challengeIsSingleUse() {
        CaptchaService service = new CaptchaService(true, 120);
        CaptchaResponse captcha = service.create();

        assertFalse(service.validate(captcha.captchaId(), "0000"));
        assertFalse(service.validate(captcha.captchaId(), "0000"));
    }

    @Test
    void disabledCaptchaSkipsValidation() {
        CaptchaService service = new CaptchaService(false, 120);

        assertTrue(service.validate(null, null));
    }
}
