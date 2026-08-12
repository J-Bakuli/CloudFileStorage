package com.jb.cloudstorage.cloud_storage.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceNameValidator {
    private final String FORBIDDEN_SPECIAL_CHARS = ":*?\"<>|";

    public boolean isSafeName(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        if (containsForbiddenChars(s)) {
            return false;
        }
        return !(s.contains("..")
                || s.contains("\\")
                || s.contains("/"));
    }

    public boolean isSafeUploadFileName(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        if (s.contains("\\")
                || s.contains("..")
                || s.contains("//")
                || s.startsWith("/")
                || s.endsWith("/")) {
            return false;
        }
        String[] segments = s.split("/");
        for (String segment : segments) {
            if (!isSafeName(segment)) {
                return false;
            }
        }
        return true;
    }

    public boolean containsForbiddenChars(String input) {
        for (int i = 0; i < FORBIDDEN_SPECIAL_CHARS.length(); i++) {
            if (input.indexOf(FORBIDDEN_SPECIAL_CHARS.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
