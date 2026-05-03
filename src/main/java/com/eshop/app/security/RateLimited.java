package com.eshop.app.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    int requests() default 10;
    int period() default 60; // seconds
    String key() default "";
    String message() default "Too many requests. Please try again later.";
}
