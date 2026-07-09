package cn.cordys.common.security.validator;

import cn.cordys.common.util.CommonBeanFactory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeHostValidator implements ConstraintValidator<SafeHost, String> {

    @Override
    public boolean isValid(String host, ConstraintValidatorContext context) {
        try {
            if (host == null || host.isEmpty()) {
                return true; // 由 @NotNull 控制
            }
            SSRFValidator ssrfValidator = CommonBeanFactory.getBean(SSRFValidator.class);
            assert ssrfValidator != null;
            ssrfValidator.validateHost(host);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}