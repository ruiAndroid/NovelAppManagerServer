package com.fun.novel.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueAppIdValidator.class)
@Documented
public @interface UniqueAppId {
    String message() default "应用ID已存在";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
} 