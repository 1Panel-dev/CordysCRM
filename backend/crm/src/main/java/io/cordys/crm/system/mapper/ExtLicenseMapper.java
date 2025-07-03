package io.cordys.crm.system.mapper;


import io.cordys.crm.system.domain.License;
import org.apache.ibatis.annotations.Param;

public interface ExtLicenseMapper {

    License get();

    License selectLicenseCode(@Param("code") String code);

}
