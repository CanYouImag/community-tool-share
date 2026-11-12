package com.linzu.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdCardUtilTest {

    @Test
    void acceptsValidIdCards() {
        assertTrue(IdCardUtil.isValid("11010519491231002X"));
        assertTrue(IdCardUtil.isValid("220101199001011232"));
        assertTrue(IdCardUtil.isValid("11010519491231002x"));
    }

    @Test
    void rejectsWrongChecksum() {
        assertFalse(IdCardUtil.isValid("110105194912310021"));
    }

    @Test
    void rejectsInvalidBirthDate() {
        assertFalse(IdCardUtil.isValid("11010519491331002X"));
    }

    @Test
    void rejectsMalformedValues() {
        assertFalse(IdCardUtil.isValid(null));
        assertFalse(IdCardUtil.isValid(""));
        assertFalse(IdCardUtil.isValid("11010519491231002"));
        assertFalse(IdCardUtil.isValid("11010519491231002A"));
    }
}
