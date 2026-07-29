package com.jb.cloudstorage.cloud_storage.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafePathValidator implements ConstraintValidator<SafePath, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        if (value.isBlank()) {
            return true;
        }
        return !(value.contains("..")
                || value.contains("\\")
                || value.contains("//")
                || value.startsWith("/"));
    }
}