package io.cordys.crm.customer.domain;

import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 客户自定义属性
 * 
 * @author jianxing
 * @date 2025-02-10 18:12:46
 */
@Data
@Table(name = "customer_field")
public class CustomerField {

	@Schema(description = "客户id")
	private String customerId;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "自定义属性id")
	private String fieldId;

	@Schema(description = "自定义属性值")
	private String fieldValue;
}
