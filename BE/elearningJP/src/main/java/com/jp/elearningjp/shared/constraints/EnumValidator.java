package com.jp.elearningjp.shared.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValidatorImpl.class)
@Documented
public @interface EnumValidator {

    Class<? extends Enum<?>> enumClass();

    String message() default "INVALID_ENUM_VALUE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
