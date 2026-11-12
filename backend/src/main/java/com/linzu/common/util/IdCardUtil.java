package com.linzu.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * 中国大陆 18 位身份证号校验。
 */
public final class IdCardUtil {

    private static final int[] WEIGHTS = {
        7,
        9,
        10,
        5,
        8,
        4,
        2,
        1,
        6,
        3,
        7,
        9,
        10,
        5,
        8,
        4,
        2,
    };
    private static final char[] CHECK_CODES = "10X98765432".toCharArray();
    private static final DateTimeFormatter BIRTH_FORMAT = DateTimeFormatter
        .ofPattern("uuuuMMdd", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT);

    private IdCardUtil() {}

    public static String normalize(String idCardNo) {
        return idCardNo == null ? null : idCardNo.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String idCardNo) {
        String value = normalize(idCardNo);
        if (value == null || !value.matches("^\\d{17}[\\dX]$")) {
            return false;
        }
        if (!isValidBirthDate(value)) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i += 1) {
            sum += (value.charAt(i) - '0') * WEIGHTS[i];
        }
        return value.charAt(17) == CHECK_CODES[sum % 11];
    }

    private static boolean isValidBirthDate(String value) {
        try {
            LocalDate birthDate = LocalDate.parse(value.substring(6, 14), BIRTH_FORMAT);
            return (
                !birthDate.isAfter(LocalDate.now()) &&
                birthDate.getYear() >= 1900
            );
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
