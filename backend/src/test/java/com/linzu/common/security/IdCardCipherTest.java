package com.linzu.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdCardCipherTest {

    private final IdCardCipher cipher = new IdCardCipher(
        "unit-test-id-card-key-at-least-32-characters"
    );

    @Test
    void encryptsAndDecrypts() {
        String encrypted = cipher.encrypt("11010519491231002X");

        assertTrue(cipher.isEncrypted(encrypted));
        assertFalse(encrypted.contains("110105"));
        assertEquals("11010519491231002X", cipher.decrypt(encrypted));
    }

    @Test
    void usesRandomIv() {
        String first = cipher.encrypt("11010519491231002X");
        String second = cipher.encrypt("11010519491231002X");

        assertNotEquals(first, second);
    }

    @Test
    void keepsLegacyPlainTextReadable() {
        assertEquals("11010519491231002X", cipher.decrypt("11010519491231002X"));
    }

    @Test
    void rejectsDataEncryptedWithAnotherKey() {
        IdCardCipher other = new IdCardCipher("another-unit-test-key-32-characters!!");
        String encrypted = other.encrypt("11010519491231002X");

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(encrypted));
    }
}
