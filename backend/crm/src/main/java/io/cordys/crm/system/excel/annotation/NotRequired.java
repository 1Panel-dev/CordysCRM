package io.cordys.crm.system.excel.annotation;

import java.lang.annotation.*;

/**
 * @author wx
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface NotRequired {
}
