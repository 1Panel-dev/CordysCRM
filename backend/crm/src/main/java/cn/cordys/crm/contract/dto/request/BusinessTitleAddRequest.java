package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BusinessTitleAddRequest {

  @Size(max = 255)
  @Schema(description = "公司名称")
  private String name;

  @Size(max = 255)
  @Schema(description = "纳税人识别号")
  private String identificationNumber;

  @Size(max = 255)
  @Schema(description = "开户银行")
  private String openingBank;

  @Size(max = 255)
  @Schema(description = "银行账户")
  private String bankAccount;

  @Size(max = 255)
  @Schema(description = "注册地址")
  private String registrationAddress;

  @Size(max = 255)
  @Schema(description = "注册电话")
  private String phoneNumber;

  @Schema(description = "注册资本")
  private String registeredCapital;

  @Size(max = 255)
  @Schema(description = "公司规模")
  private String companySize;

  @Size(max = 255)
  @Schema(description = "工商注册账号")
  private String registrationNumber;

  @Size(max = 50)
  @Schema(description = "来源类型(自定义(CUSTOM)/三方(THIRD_PARTY))")
  private String type;
}
