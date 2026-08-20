package com.jb.cloudstorage.cloud_storage.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafePathValidator implements ConstraintValidator<SafePath, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return ResourceNameValidator.isSafePath(value);
    }
}