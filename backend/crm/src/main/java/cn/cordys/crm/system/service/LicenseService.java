package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.LicenseStatus;
import cn.cordys.crm.system.domain.License;
import cn.cordys.crm.system.dto.LicenseDTO;
import cn.cordys.crm.system.dto.log.LicenseLogDTO;
import cn.cordys.crm.system.mapper.ExtLicenseMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class LicenseService {

  @Resource private BaseMapper<License> licenseBaseMapper;

  @Resource private ExtLicenseMapper extLicenseMapper;

  @Cacheable(value = "license_cache", key = "'CORDYS-LICENSE'", unless = "#result == null")
  public LicenseDTO validate() {
    var code = Optional.ofNullable(extLicenseMapper.get()).map(License::getLicenseCode).orElse("");
    return validate(code);
  }

  @OperationLog(module = LogModule.SYSTEM, type = LogType.ADD)
  @CachePut(value = "license_cache", key = "'CORDYS-LICENSE'", unless = "#result == null")
  public LicenseDTO add(String code, String userId) {
    LicenseDTO licenseDTO = validate(code);

    if (licenseDTO == null || StringUtils.isBlank(licenseDTO.getProduct())) {
      throw new GenericException(Translator.get("license_valid_license_error"));
    }
    License license = extLicenseMapper.get();
    if (license == null) {
      license = new License();
      license.setId(IDGenerator.nextStr());
      license.setLicenseCode(code);
      license.setCreateTime(System.currentTimeMillis());
      license.setUpdateTime(System.currentTimeMillis());
      license.setCreateUser(userId);
      license.setUpdateUser(userId);
      licenseBaseMapper.insert(license);
      LicenseLogDTO licenseLogDTO = new LicenseLogDTO();
      BeanUtils.copyBean(licenseLogDTO, license);
      OperationLogContext.setContext(
          LogContextInfo.builder()
              .originalValue(null)
              .resourceId(license.getId())
              .resourceName("license")
              .modifiedValue(licenseLogDTO)
              .build());
    } else {
      LicenseLogDTO oldLogDTO = new LicenseLogDTO();
      BeanUtils.copyBean(oldLogDTO, license);
      license.setLicenseCode(code);
      license.setUpdateTime(System.currentTimeMillis());
      license.setUpdateUser(userId);
      licenseBaseMapper.update(license);
      LicenseLogDTO updateDTO = new LicenseLogDTO();
      BeanUtils.copyBean(updateDTO, license);
      // 添加日志上下文
      OperationLogContext.setContext(
          LogContextInfo.builder()
              .originalValue(oldLogDTO)
              .resourceId(license.getId())
              .resourceName("license")
              .modifiedValue(updateDTO)
              .build());
    }
    return licenseDTO;
  }

  public LicenseDTO validate(String code) {
    boolean isValid = CommonBeanFactory.packageExists();
    LicenseDTO licenseDTO = new LicenseDTO();
    if (isValid && StringUtils.isNotBlank(code)) {
      Object license =
          CommonBeanFactory.invoke(
              "extLicenseService",
              clazz -> {
                try {
                  return clazz.getMethod("command", String.class);
                } catch (NoSuchMethodException e) {
                  return null;
                }
              },
              code);
      if (license != null) {
        return (LicenseDTO) license;
      }
    }

    if (isValid) {
      licenseDTO.setStatus(LicenseStatus.INVALID.getName());
    }

    return licenseDTO;
  }
}
