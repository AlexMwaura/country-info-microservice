package com.ncba.countryinfo.util;

/**
 * Utility methods for string normalization used across the service.
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * Converts a country name to sentence case (first letter uppercase, rest lowercase).
     * Handles multi-word country names by capitalizing each word.
     * Examples: "kenya" -> "Kenya", "united states" -> "United States"
     */
    public static String toSentenceCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String trimmed = input.trim().toLowerCase();
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : trimmed.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
