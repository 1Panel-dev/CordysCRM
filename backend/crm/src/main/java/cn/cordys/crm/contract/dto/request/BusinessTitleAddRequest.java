package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BusinessTitleAddRequest {

    @Size(max = 255)
    @Schema(description = "公司名称")
    private String businessName;

    @Size(max = 255)
    @Schema(description = "纳税人识别号")
    private String identificationNumber;

    @Size(max = 255)
    @Schema(description = "开户银行")
    private String openingBank;

    @Size(max = 50)
    @Schema(description = "银行账号")
    private String bankAccount;

    @Size(max = 255)
    @Schema(description = "注册地址")
    private String registrationAddress;

    @Size(max = 50)
    @Schema(description = "注册电话")
    private String phoneNumber;

    @Schema(description = "注册资本")
    private BigDecimal registeredCapital;

    @Size(max = 50)
    @Schema(description = "客户规模")
    private String customerSize;

    @Size(max = 50)
    @Schema(description = "工商注册号")
    private String registrationNumber;

    @Size(max = 50)
    @Schema(description = "来源类型(自定义(CUSTOM)/三方(THIRD_PARTY))")
    private String type;

}
