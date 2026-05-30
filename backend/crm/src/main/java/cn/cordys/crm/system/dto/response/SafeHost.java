package cn.cordys.crm.system.dto.response;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SafeHostValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeHost {
    String message() default "不允许使用内网或保留地址";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}