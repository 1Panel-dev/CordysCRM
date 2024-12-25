package io.cordys.aspectj.annotation;

import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface OperationLogs {
    OperationLog[] value();
}

