package cn.cordys.crm.system.mapper;


import cn.cordys.crm.system.domain.License;
import org.apache.ibatis.annotations.Param;

public interface ExtLicenseMapper {

    License get();

    License selectLicenseCode(@Param("code") String code);

}
