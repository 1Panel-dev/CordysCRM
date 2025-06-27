package io.cordys.crm.system.service;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.system.dto.LicenseDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class LicenseService {

    @Cacheable(value = "license_cache", key = "'CORDYS-LICENSE'", unless = "#result == null")
    public LicenseDTO validate() {
        // TODO 从数据库查询 code
        String code = "xx"; // 这里需要替换为实际的许可证代码获取逻辑
        return validate(code);
    }

    @CachePut(value = "license_cache", key = "'CORDYS-LICENSE'", unless = "#result == null")
    public LicenseDTO add(String code) {
        LicenseDTO licenseDTO = validate(code);

        // TODO: 实现 License 添加逻辑

        return licenseDTO;
    }

    public LicenseDTO validate(String code) {
        boolean isValid = CommonBeanFactory.packageExists("io.cordys.xpack");
        if (isValid && StringUtils.isNotBlank(code)) {
            Object license = CommonBeanFactory.invoke("extLicenseService",
                    clazz -> {
                        try {
                            return clazz.getMethod("command", String.class);
                        } catch (NoSuchMethodException e) {
                            return null;
                        }
                    }, code);
            if (license != null) {
                return (LicenseDTO) license;
            }
        }

        return new LicenseDTO();
    }

}
