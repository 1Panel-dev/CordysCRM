package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_license")
public class License extends BaseModel {

    @Schema(description = "license_code")
    private String licenseCode;

}
