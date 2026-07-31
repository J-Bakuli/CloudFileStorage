package com.jb.cloudstorage.cloud_storage.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceNameValidator {
    private static final String FORBIDDEN_SPECIAL_CHARS = ":*?\"<>|";

    public static boolean isSafeName(String s) {
        if (s == null) {
            return false;
        }
        if (s.isBlank()) {
            return false;
        }
        if (containsForbiddenChars(s)) {
            return false;
        }
        return !(s.contains("..")
                || s.contains("\\")
                || s.contains("/"));
    }

    public static boolean containsForbiddenChars(String input) {
        for (int i = 0; i < FORBIDDEN_SPECIAL_CHARS.length(); i++) {
            if (input.indexOf(FORBIDDEN_SPECIAL_CHARS.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
