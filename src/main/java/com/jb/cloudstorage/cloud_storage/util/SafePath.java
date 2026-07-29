package com.jb.cloudstorage.cloud_storage.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafePathValidator.class)
public @interface SafePath {
    String message() default "Invalid path";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
