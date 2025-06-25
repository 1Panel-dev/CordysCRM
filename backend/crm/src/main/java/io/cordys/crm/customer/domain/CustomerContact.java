package io.cordys.crm.customer.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 客户联系人
 * 
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Data
@Table(name = "customer_contact")
public class CustomerContact extends BaseModel {

	@Schema(description = "客户id")
	private String customerId;

	@Schema(description = "责任人")
	private String owner;

	@Schema(description = "联系人姓名")
	private String name;

	@Schema(description = "联系人电话")
	private String phone;

	@Schema(description = "是否停用")
	private Boolean enable;

	@Schema(description = "停用原因")
	private String disableReason;

	@Schema(description = "组织id")
	private String organizationId;
}
