package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseModel;
import jakarta.persistence.Table;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Table(name = "business_title")
public class BusinessTitle extends BaseModel {

	@Schema(description = "公司名称")
	private String businessName;

	@Schema(description = "来源类型")
	private String type;

	@Schema(description = "纳税人识别号")
	private String identificationNumber;

	@Schema(description = "开户银行")
	private String openingBank;

	@Schema(description = "银行账号")
	private String bankAccount;

	@Schema(description = "注册地址")
	private String registrationAddress;

	@Schema(description = "注册电话")
	private String phoneNumber;

	@Schema(description = "注册资本")
	private BigDecimal registeredCapital;

	@Schema(description = "客户规模")
	private String customerSize;

	@Schema(description = "工商注册号")
	private String registrationNumber;

	@Schema(description = "审核状态")
	private String approvalStatus;

	@Schema(description = "不通过原因")
	private String unapprovedReason;

	@Schema(description = "组织id")
	private String organizationId;
}
