package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BusinessTitleAddRequest {

    @NotBlank(message = "{business_title.business_name.required}")
    @Size(max = 255)
    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessName;

    @NotBlank(message = "{business_title.identification_number.required}")
    @Size(max = 255)
    @Schema(description = "纳税人识别号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identificationNumber;

    @NotBlank(message = "{business_title.opening_bank.required}")
    @Size(max = 255)
    @Schema(description = "开户银行", requiredMode = Schema.RequiredMode.REQUIRED)
    private String openingBank;

    @NotBlank(message = "{business_title.bank_account.required}")
    @Size(max = 50)
    @Schema(description = "银行账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankAccount;

    @NotBlank(message = "{business_title.registration_address.required}")
    @Size(max = 255)
    @Schema(description = "注册地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationAddress;

    @NotBlank(message = "{business_title.phone_number.required}")
    @Size(max = 50)
    @Schema(description = "注册电话", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @Schema(description = "注册资本", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal registeredCapital;

    @NotBlank(message = "{business_title.customer_size.required}")
    @Size(max = 50)
    @Schema(description = "客户规模", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerSize;

    @NotBlank(message = "{business_title.registration_number.required}")
    @Size(max = 50)
    @Schema(description = "工商注册号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationNumber;

    @NotBlank(message = "{business_title.type.required}")
    @Size(max = 50)
    @Schema(description = "来源类型(自定义(CUSTOM)/三方(THIRD_PARTY))", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

}
