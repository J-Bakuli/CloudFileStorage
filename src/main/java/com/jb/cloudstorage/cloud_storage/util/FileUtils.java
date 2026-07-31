package com.jb.cloudstorage.cloud_storage.util;

import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileUtils {
    private static final String FORBIDDEN_SPECIAL_CHARS = ":*?\"<>|";

    public record PathParts(String parentPath, String name, ResourceType type) {
    }

    public static String normalizeParentPath(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            return "";
        }
        return folderPath.endsWith("/") ? folderPath : folderPath + "/";
    }

    public static String joinPath(String folderPath, String fileName) {
        if (folderPath == null || folderPath.isBlank()) {
            return fileName;
        }
        return normalizeParentPath(folderPath) + fileName;
    }

    public static PathParts splitPath(String fullPath) {
        ResourceType resourceType = getResourceType(fullPath);
        String normalizedPath = removeTrailingSlash(fullPath);
        String resourceName = extractResourceName(normalizedPath);
        String parentPath = extractParentPath(normalizedPath);
        return new PathParts(parentPath, resourceName, resourceType);
    }

    public static ResourceType getResourceType(String fullPath) {
        return fullPath.endsWith("/") ? ResourceType.DIRECTORY : ResourceType.FILE;
    }

    public static boolean isSafeName(String s) {
        if (s == null) {
            return false;
        }
        if (s.isBlank()) {
            return false;
        }
        if (checkIfContainsForbiddenChars(s)) {
            return false;
        }
        return !(s.contains("..")
                || s.contains("\\")
                || s.contains("/"));
    }

    public static boolean checkIfContainsForbiddenChars(String input) {
        for (int i = 0; i < FORBIDDEN_SPECIAL_CHARS.length(); i++) {
            if (input.indexOf(FORBIDDEN_SPECIAL_CHARS.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static String getRelativePath(Long userId, String objectName) {
        return objectName.substring(userRootPrefix(userId).length());
    }

    public String userRootPrefix(Long userId) {
        return "user-" + userId + "-files/";
    }

    private String removeTrailingSlash(String fullPath) {
        return fullPath.endsWith("/") ? fullPath.substring(0, fullPath.length() - 1) : fullPath;
    }

    private String extractParentPath(String path) {
        int lastSeparatorIndex = path.lastIndexOf('/');
        return lastSeparatorIndex == -1 ? "" : path.substring(0, lastSeparatorIndex + 1);
    }

    private String extractResourceName(String path) {
        int lastSeparatorIndex = path.lastIndexOf('/');
        return lastSeparatorIndex == -1 ? path : path.substring(lastSeparatorIndex + 1);
    }
}
