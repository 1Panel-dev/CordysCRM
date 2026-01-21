package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "business_title")
public class BusinessTitle extends BaseModel {

    @Schema(description = "公司名称")
    private String name;

    @Schema(description = "来源类型")
    private String type;

    @Schema(description = "纳税人识别号")
    private String identificationNumber;

    @Schema(description = "开户银行")
    private String openingBank;

    @Schema(description = "银行账户")
    private String bankAccount;

    @Schema(description = "注册地址")
    private String registrationAddress;

    @Schema(description = "注册电话")
    private String phoneNumber;

    @Schema(description = "注册资本")
    private String registeredCapital;

    @Schema(description = "公司规模")
    private String companySize;

    @Schema(description = "工商注册账号")
    private String registrationNumber;

    @Schema(description = "审核状态")
    private String approvalStatus;

    @Schema(description = "不通过原因")
    private String unapprovedReason;

    @Schema(description = "组织id")
    private String organizationId;
}
