package com.jb.cloudstorage.cloud_storage.util;

public final class FileUtils {
    private FileUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
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
}
