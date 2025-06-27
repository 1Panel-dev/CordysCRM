package io.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LicenseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 客户名称
     */
    @Schema(description = "客户名称")
    private String corporation = "CORDYS";
    /**
     * 授权截止时间
     */
    @Schema(description = "授权截止时间")
    private String expired;
    /**
     * 产品名称
     */
    @Schema(description = "产品名称")
    private String product = "CRM";
    /**
     * 产品版本
     */
    @Schema(description = "产品版本")
    private String edition;
    /**
     * License版本
     */
    @Schema(description = "license版本")
    private String licenseVersion;
    /**
     * 授权数量
     */
    @Schema(description = "授权数量")
    private int count;

    /**
     * 状态
     */
    private String status;

    public LicenseDTO() {
        this.status = "invalid";
    }
}
