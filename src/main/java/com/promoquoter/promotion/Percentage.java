package com.promoquoter.promotion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A discount percentage: greater than 0, at most 100, with at most two decimal places.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@DecimalMin(value = "0", inclusive = false)
@DecimalMax("100")
@Digits(integer = 3, fraction = 2)
@ReportAsSingleViolation
public @interface Percentage {

    String message() default "must be greater than 0 and at most 100, with at most 2 decimal places";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
