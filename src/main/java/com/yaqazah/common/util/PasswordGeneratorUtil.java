package com.yaqazah.common.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PasswordGeneratorUtil {

    // Private constructor to prevent anyone from instantiating this utility class
    private PasswordGeneratorUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String generateCompliantPassword() {
        // Guarantee at least 2 of each required type
        String uppers = RandomStringUtils.secure().nextAlphabetic(2).toUpperCase();
        String lowers = RandomStringUtils.secure().nextAlphabetic(2).toLowerCase();
        String numbers = RandomStringUtils.secure().nextNumeric(2);

        // Add 2 more random characters to reach 8 total
        String remaining = RandomStringUtils.secure().nextAlphanumeric(2);

        // Combine them all
        String combined = uppers + lowers + numbers + remaining;

        // Shuffle the characters so the pattern isn't predictable
        List<Character> chars = combined.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(chars);

        return chars.stream().map(String::valueOf).collect(Collectors.joining());
    }
}